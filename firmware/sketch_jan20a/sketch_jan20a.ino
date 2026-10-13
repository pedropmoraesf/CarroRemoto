/*
 * CONTROLE DO CARRO INFANTIL - Arduino Nano
 *
 * Revisao comentada do sketch_jan20a.ino original (versao de outubro/2026).
 * O codigo original, com TODOS os seus comentarios, permanece inalterado em
 * ../historico/sketch_jan20a_original.ino.txt.
 *
 * O carro e pesado; os motores/engrenagens nao foram inspecionados.
 * Esta versao NAO garante seguranca funcional. Teste sem ocupantes e
 * com as rodas suspensas, com interruptor fisico de corte de energia.
 *
 * PROTOCOLO BLUETOOTH SPP (9600 bps), compativel com o aplicativo:
 *   F = frente              T = tras
 *   Y ou W = soltar tracao  (desaceleracao normal)
 *   R = virar direita       L = virar esquerda
 *   Z ou X = soltar direcao
 *   120; = exemplo de PWM desejado
 *   H = pulso de presenca enviado periodicamente pelo aplicativo atualizado
 *   ! = parada prioritaria (mais rapida, pode causar tranco)
 *
 * IMPORTANTE: com alguns circuitos BTS7960, PWM de ambos os lados em ZERO
 * com EN em HIGH equivale a FRENAGEM eletrica, e nao a roda livre.
 * Os pinos EN originais foram mantidos: e indispensavel verificar a fiação
 * ANTES de optar por um modo de roda livre no fim da rampa.
 */

// Biblioteca original para a interface Bluetooth serial por software.
// //#include<ESP8266WiFi.h>  // teste legado de ESP8266; mantido no historico.
#include <SoftwareSerial.h>

// Ligacoes originais do modulo Bluetooth: D2 = RX, D11 = TX.
// //SoftwareSerial mySerial(8,9);  // alternativa antiga no historico.
SoftwareSerial BTSerial(2, 11);

// Ligacoes preservadas sem assumir que "rele" e realmente enable do IBT-2.
// //int pino_R=5;  // anotacao historica preservada na copia original.
// //int pino_L=6;
const uint8_t pino_R_EN = 5;      // fio verde: antes "pinos reles"
const uint8_t pino_bts_vin = 6;   // fio amarelo/azul: funcao eletrica NAO confirmada
const uint8_t pino_L_EN = 4;      // fio azul/marrom
// //int pino_R_EN=12;           // fio roxo/verde: alternativa antiga

// A tracao usa dois canais PWM; nunca ligamos os dois simultaneamente.
// //pinos de transistores?  // duvida original preservada
const uint8_t pino_frente = 9;          // fio roxo/branco: canal PWM frente
const uint8_t pino_tras = 10;           // fio azul/cinza: canal PWM tras
const uint8_t pino_frente_analog = A0;  // chave/pedal local, ativo em LOW
const uint8_t pino_tras_analog = A1;    // chave/pedal local, ativo em LOW

// Saidas antigas de direcao: LOW aciona; HIGH desliga.
// //pinos da bts7960; rel_gnd=33 e rel_vin=43 eram rascunhos antigos.
const uint8_t pino_R = 7;
const uint8_t pino_L = 8;

// Constantes iniciais CONSERVADORAS: revisar apos medir o comportamento real.
// 160/255 ~ 63% de duty-cycle. Isso NAO limita a corrente da ponte H.
// Nao elevar o limite ate inspecionar engrenagens, rodas e temperatura.
const uint8_t PWM_MAXIMO_TESTE = 160;
const uint8_t PWM_PADRAO = 120;

// Rampa executada por millis(), sem delay(10) no loop.
// Passo 2 para acelerar: de 0 a 160 leva aproximadamente 1,6 s.
// Passo 3 para desacelerar: de 160 a 0 leva aproximadamente 1,1 s.
// A parada prioritaria usa um passo maior para reduzir tempo sem controle;
// ela AINDA pode travar roda se a ponte H aplicar freio em PWM = 0.
const unsigned long INTERVALO_RAMPA_MS = 20;
const uint8_t PASSO_ACELERACAO = 2;
const uint8_t PASSO_DESACELERACAO = 3;
const uint8_t PASSO_PARADA_PRIORITARIA = 16;

// Depois de chegar a zero, aguarda antes de inverter o giro da tracao.
// A direcao eletrica tambem recebe um pequeno intervalo entre inversoes.
const unsigned long NEUTRO_ANTES_REVERSAO_MS = 450;
const unsigned long NEUTRO_DIRECAO_MS = 150;

// Quando o aplicativo deixa de enviar comandos por mais de 800 ms,
// zera o pedido de tracao/direcao do Bluetooth e inicia parada prioritaria.
// O aplicativo 1.1+ envia H a cada 200 ms enquanto ha botao pressionado.
const unsigned long LIMITE_SEM_COMUNICACAO_MS = 800;

// O buffer antigo "char vel_buffer[3]" nao tinha espaco para terminador;
// isso permitia leitura fora dos limites e valores invalidos.
char vel_buffer[4] = {'\0', '\0', '\0', '\0'};
uint8_t digitosRecebidos = 0;
bool numeroInvalido = false;
unsigned long inicioNumeroMs = 0;
const unsigned long TIMEOUT_NUMERO_MS = 250;

// A representacao evita alternar polaridade enquanto ainda ha PWM aplicado:
//  1 = frente/direita; -1 = tras/esquerda; 0 = neutro.
int8_t pedidoBluetoothTracao = 0;
int8_t pedidoBluetoothDirecao = 0;
int8_t tracaoAtiva = 0;
int8_t direcaoAtiva = 0;

uint8_t pwmDesejado = PWM_PADRAO;
uint8_t pwmAplicado = 0;

unsigned long ultimaMensagemMs = 0;
unsigned long ultimoPassoMs = 0;
unsigned long entrouEmNeutroMs = 0;
unsigned long desligouDirecaoMs = 0;
unsigned long instanteParadaMs = 0;

// A trava requer soltar botoes locais antes de limpar uma parada "!".
// Um novo comando F/T explicito pode reiniciar apos rearmar no aplicativo.
bool paradaPrioritaria = false;
bool travaEmergencia = false;
bool iniciouBluetooth = false;
bool entradasLocaisLiberadasNoInicio = false;
bool pedaisEstavamSoltos = false;

// Guarda quanto tempo os dois controles analogicos ficaram soltos.
unsigned long pedaisSoltosDesdeMs = 0;

// Escreve o PWM em apenas UM lado da ponte, com o outro em zero.
// Este codigo respeita os pinos e as polaridades do sketch anterior.
void aplicarTracao(int8_t sentido, uint8_t potencia) {
  if (sentido == 1 && potencia > 0) {
    analogWrite(pino_tras, 0);
    analogWrite(pino_frente, potencia);
  } else if (sentido == -1 && potencia > 0) {
    analogWrite(pino_frente, 0);
    analogWrite(pino_tras, potencia);
  } else {
    analogWrite(pino_frente, 0);
    analogWrite(pino_tras, 0);
  }
}

// Coloca ambas as saidas de direcao em seu estado original de repouso.
void desligarDirecao() {
  digitalWrite(pino_R, HIGH);
  digitalWrite(pino_L, HIGH);
}

// Descarta numeros incompletos e previne que 'F' seja tratado como digito.
void zerarBufferVelocidade() {
  digitosRecebidos = 0;
  numeroInvalido = false;
  vel_buffer[0] = '\0';
}

// A velocidade e aplicada somente apos ';' e e limitada a 160 neste teste.
// Mantem compatibilidade com os comandos ASCII do aplicativo.
void processarDigitoOuTerminador(char recebido) {
  if (recebido >= '0' && recebido <= '9') {
    if (digitosRecebidos == 0) inicioNumeroMs = millis();
    if (digitosRecebidos < 3) {
      vel_buffer[digitosRecebidos++] = recebido;
      vel_buffer[digitosRecebidos] = '\0';
    } else {
      numeroInvalido = true;
    }
    return;
  }

  if (recebido == ';') {
    if (!numeroInvalido && digitosRecebidos > 0) {
      unsigned int valor = (unsigned int)atoi(vel_buffer);
      if (valor >= 1 && valor <= 255) {
        pwmDesejado = (uint8_t)min(valor, (unsigned int)PWM_MAXIMO_TESTE);
      }
    }
    zerarBufferVelocidade();
  }
}

// Trata cada byte recebido. O codigo original reutilizava o ultimo caractere
// se o buffer serial estivesse vazio, podendo manter o motor acionado.
void processarComando(char recebido) {
  if (recebido >= '0' && recebido <= '9') {
    processarDigitoOuTerminador(recebido);
    return;
  }
  if (recebido == ';') {
    processarDigitoOuTerminador(recebido);
    return;
  }

  // Caractere nao numerico interrompe uma velocidade parcial invalida.
  if (digitosRecebidos != 0) zerarBufferVelocidade();
  if (recebido >= 'a' && recebido <= 'z') recebido -= ('a' - 'A');

  // Novo comando de movimentacao renova o tempo de comunicacao.
  // "H" nao pode reativar motores sozinho apos o timeout.
  if (recebido == 'F' || recebido == 'T') {
    ultimaMensagemMs = millis();
    iniciouBluetooth = true;
    travaEmergencia = false;
    paradaPrioritaria = false;
    pedidoBluetoothTracao = (recebido == 'F') ? 1 : -1;
  } else if (recebido == 'Y' || recebido == 'W') {
    ultimaMensagemMs = millis();
    iniciouBluetooth = true;
    pedidoBluetoothTracao = 0;  // Soltar: usa desaceleracao gradual.
  } else if (recebido == 'R' || recebido == 'L') {
    ultimaMensagemMs = millis();
    iniciouBluetooth = true;
    if (!travaEmergencia) {
      pedidoBluetoothDirecao = (recebido == 'R') ? 1 : -1;
    }
  } else if (recebido == 'Z' || recebido == 'X') {
    ultimaMensagemMs = millis();
    iniciouBluetooth = true;
    pedidoBluetoothDirecao = 0;
  } else if (recebido == 'H') {
    ultimaMensagemMs = millis();  // Heartbeat: nao altera sentido nem PWM.
    iniciouBluetooth = true;
  } else if (recebido == '!') {
    // Somente para parada prioritaria; NAO serve como freio mecanico.
    ultimaMensagemMs = millis();
    iniciouBluetooth = true;
    pedidoBluetoothTracao = 0;
    pedidoBluetoothDirecao = 0;
    paradaPrioritaria = true;
    travaEmergencia = true;
    instanteParadaMs = millis();
  }
}

// Retorna o pedido de tracao apos considerar os botoes/pedais ligados ao
// A0/A1. Eles sao entradas fisicas; o timeout Bluetooth nao os substitui.
// Exige ambos soltos no inicio para nao partir com chave presa em LOW.
int8_t obterPedidoTracao() {
  bool frenteLocal = (digitalRead(pino_frente_analog) == LOW);
  bool trasLocal = (digitalRead(pino_tras_analog) == LOW);
  unsigned long agora = millis();

  // Os dois controles simultaneos geram parada prioritaria, nao inversao.
  if (frenteLocal && trasLocal) {
    paradaPrioritaria = true;
    return 0;
  }

  if (!frenteLocal && !trasLocal) {
    if (!pedaisEstavamSoltos) pedaisSoltosDesdeMs = agora;
    pedaisEstavamSoltos = true;
    if (agora - pedaisSoltosDesdeMs >= 200) {
      entradasLocaisLiberadasNoInicio = true;
      // A parada por "!" so e liberada apos soltar os pedais por 200 ms.
      if (travaEmergencia && agora - instanteParadaMs >= 200) {
        travaEmergencia = false;
      }
    }
  } else {
    pedaisEstavamSoltos = false;
  }

  if (travaEmergencia) return 0;
  if (entradasLocaisLiberadasNoInicio) {
    if (frenteLocal) return 1;
    if (trasLocal) return -1;
  }
  return pedidoBluetoothTracao;
}

// A rampa limita variacoes rapidas e so troca frente/tras com PWM zerado.
// O tempo em neutro protege mais as engrenagens que uma inversao imediata.
void atualizarTracao(unsigned long agora) {
  if (agora - ultimoPassoMs < INTERVALO_RAMPA_MS) return;
  ultimoPassoMs = agora;

  int8_t solicitado = obterPedidoTracao();
  uint8_t alvo = (solicitado != 0 && tracaoAtiva == solicitado)
      ? pwmDesejado : 0;
  uint8_t passoParar = paradaPrioritaria
      ? PASSO_PARADA_PRIORITARIA : PASSO_DESACELERACAO;

  if (pwmAplicado > alvo) {
    uint8_t diferenca = pwmAplicado - alvo;
    pwmAplicado -= (diferenca > passoParar ? passoParar : diferenca);
  } else if (pwmAplicado < alvo) {
    uint8_t diferenca = alvo - pwmAplicado;
    pwmAplicado += (diferenca > PASSO_ACELERACAO
        ? PASSO_ACELERACAO : diferenca);
  }

  if (pwmAplicado == 0 && tracaoAtiva != 0) {
    tracaoAtiva = 0;
    entrouEmNeutroMs = agora;
  }

  if (tracaoAtiva == 0 && solicitado != 0
      && agora - entrouEmNeutroMs >= NEUTRO_ANTES_REVERSAO_MS
      && !travaEmergencia) {
    tracaoAtiva = solicitado;
  }

  // Nunca aplica PWM da direcao antiga depois de uma reversao.
  aplicarTracao(tracaoAtiva, pwmAplicado);
}

// A direcao original era acionada por LOW em D7/D8 e desligada por HIGH.
// Antes de inverter, desliga ambos os canais e aguarda 150 ms.
void atualizarDirecao(unsigned long agora) {
  int8_t solicitado = travaEmergencia ? 0 : pedidoBluetoothDirecao;
  if (direcaoAtiva != 0 && direcaoAtiva != solicitado) {
    desligarDirecao();
    direcaoAtiva = 0;
    desligouDirecaoMs = agora;
  }
  if (direcaoAtiva == 0 && solicitado != 0
      && agora - desligouDirecaoMs >= NEUTRO_DIRECAO_MS) {
    desligarDirecao();
    if (solicitado == 1) digitalWrite(pino_R, LOW);
    else digitalWrite(pino_L, LOW);
    direcaoAtiva = solicitado;
  }
}

// Se parar de receber os pulsos, nao permite que a ultima ordem F/T/R/L
// fique ligada indefinidamente como ocorria com o firmware original.
void verificarComunicacao(unsigned long agora) {
  if (iniciouBluetooth
      && agora - ultimaMensagemMs > LIMITE_SEM_COMUNICACAO_MS) {
    // Se um Y/W ja solicitou a rampa normal, nao acelera sua frenagem
    // apenas porque o aplicativo parou de transmitir apos a soltura.
    bool haviaComandoAtivo = (pedidoBluetoothTracao != 0 ||
                             pedidoBluetoothDirecao != 0);
    pedidoBluetoothTracao = 0;
    pedidoBluetoothDirecao = 0;
    if (haviaComandoAtivo) paradaPrioritaria = true;
    iniciouBluetooth = false;  // H isolado jamais reacelera a tracao.
  }
}

// Prepara PWM em zero antes de configurar os enables originais.
void setup() {
  pinMode(pino_frente, OUTPUT);
  pinMode(pino_tras, OUTPUT);
  pinMode(pino_R, OUTPUT);
  pinMode(pino_L, OUTPUT);
  aplicarTracao(0, 0);
  desligarDirecao();

  // //pino voltagem entrada bluetooth; estado_blue=A2
  // //int pino_blue_vin=2;
  // //int estado_blue=A2;
  // Preserva exatamente o comportamento eletrico antigo de D6:
  // digitalWrite HIGH em INPUT ativa pull-up; mudar para OUTPUT sem
  // identificar a fiacao pode causar curto ou alimentar algo indevidamente.
  digitalWrite(pino_bts_vin, HIGH);

  // Estas duas saidas ja eram inicializadas HIGH no firmware anterior.
  // Confirmar no carro se acionam reles ou R_EN/L_EN antes de modificar!
  pinMode(pino_R_EN, OUTPUT);
  pinMode(pino_L_EN, OUTPUT);
  digitalWrite(pino_R_EN, HIGH);
  digitalWrite(pino_L_EN, HIGH);

  pinMode(pino_frente_analog, INPUT_PULLUP);
  pinMode(pino_tras_analog, INPUT_PULLUP);

  Serial.begin(9600);
  BTSerial.begin(9600);
  ultimaMensagemMs = millis();
  ultimoPassoMs = millis();
  entrouEmNeutroMs = millis();
  desligouDirecaoMs = millis();

  Serial.println(F("ControleCarro: seguro ao iniciar; PWM limitado."));
}

// Laco principal: nao usa delay(), para processar solturas e timeout.
// O esboco original e todos os rascunhos comentados estao no historico.
void loop() {
  unsigned long agora = millis();

  // Interpreta TODOS os bytes ja recebidos, em vez de so um a cada loop.
  while (BTSerial.available() > 0) {
    processarComando((char)BTSerial.read());
  }

  // Descarta numero sem ';' e nao acumula bytes de comandos futuros.
  if (digitosRecebidos != 0
      && agora - inicioNumeroMs > TIMEOUT_NUMERO_MS) {
    zerarBufferVelocidade();
  }

  verificarComunicacao(agora);
  atualizarTracao(agora);
  atualizarDirecao(agora);
}
