# Firmware do Arduino Nano — ControleCarro

## Versão e preservação do original

- Código principal: `sketch_jan20a/sketch_jan20a.ino`.
- **Código antigo preservado integralmente**, incluindo todos os comentários originais: `historico/sketch_jan20a_original.ino.txt`.
- O novo firmware está comentado em português, com explicação de pinos, estado, parser Bluetooth, rampa, inversão e tratamento de falhas.
- Não houve upload para o Arduino nem acionamento dos motores. A compilação sozinha não valida o circuito ou a direção dos motores.

## Configurações iniciais (revisar antes de usar)

| Parâmetro no código | Inicial | Significado |
|---|---:|---|
| `PWM_MAXIMO_TESTE` | 160/255 | Limita a *solicitação* de potência; **não** mede/limita corrente elétrica |
| `PWM_PADRAO` | 120/255 | Valor solicitado ao iniciar |
| `INTERVALO_RAMPA_MS` | 20 ms | Intervalo de ajuste do PWM |
| `PASSO_ACELERACAO` | 2 | Aproximadamente 1,6 s de 0 a 160 |
| `PASSO_DESACELERACAO` | 3 | Redução do PWM interno após soltura; **não** indica tempo de parada física |
| `PASSO_PARADA_PRIORITARIA` | 16 | Redução interna mais rápida; roda livre não freia mecanicamente |
| `NEUTRO_ANTES_REVERSAO_MS` | 2.500 ms | Espera adicional antes de inverter a tração; não comprova parada das rodas |
| `LIMITE_SEM_COMUNICACAO_MS` | 800 ms | Prazo após o último comando válido/heartbeat |

O **tempo de rampa do PWM interno não representa distância ou tempo real de parada**. Com o modo de roda livre habilitado, a ponte é desabilitada ao soltar o acelerador e o carro pode continuar rolando por inércia. A desaceleração deixa de ser controlada eletricamente nessa fase; em declives, o carro pode ganhar velocidade. Sem sensores de velocidade, não é possível verificar se as rodas pararam antes de uma inversão.

### Roda livre da BTS7960 — BLOQUEADA por padrão até conferir os fios

O código tem o sinalizador `BTS_EN_D4_D5_CONFIRMADOS = false`. **Não mude para `true` apenas porque a placa é uma BTS7960.** Antes é necessário conferir:

1. Nano D5 está realmente conectado a `R_EN` da ponte.
2. Nano D4 está realmente conectado a `L_EN` da ponte.
3. Nenhuma das linhas D4/D5 está diretamente ligada ao +5 V ou conectada a relés/desconhecidos; forçar LOW contra +5 V pode causar danos.
4. A placa dispõe de aterramento de referência comum e os terminais `RPWM`/`LPWM` realmente correspondem a D9/D10 do firmware.

**Quando verificado**, mudar somente o sinalizador para `true`, recompilar e gravar no Nano. O código desabilita os dois enables (LOW) após soltura, na reversão e na parada prioritária. Ao voltar a movimentar, configura um dos PWMs e só então habilita os dois enables (HIGH). Com `false`, preserva o estado HIGH original dos enables e, portanto, a ponte pode continuar fazendo frenagem elétrica em PWM zero.

Documentação técnica: [Infineon BTS7960 — datasheet](https://www.infineon.com/assets/row/public/documents/10/57/infineon-bts7960-ds-en.pdf), seção 4.4.1 e tabela 4.4.5. O dispositivo se desabilita com `INH=LOW`.

**Uma placa IBT-2/BTS7960 completa com dois semicondutores forma uma ponte para UM motor independente.** Caso os dois motores de 12 V compartilhem essa mesma placa, é necessário verificar se estão ligados em paralelo e dimensionar a corrente conjunta de partida e de travamento. A indicação comercial de 43 A não assegura que a montagem suportará esse uso.

## Comunicação com o app

Compatibilidade com comandos originais:
- `F` frente, `T` ré, `Y`/`W` soltura da tração, `R` direita, `L` esquerda, `Z`/`X` soltura da direção;
- `NNN;` define o PWM desejado, validado entre 1 e 255 e limitado a 160;
- **Novo:** `H` é um pulso periódico a cada 200 ms enquanto um botão permanece pressionado;
- **Novo:** `!` solicita parada prioritária. O app envia `!YZ`, também compatível com o firmware antigo, que ignora `!`.

Após 800 ms sem mensagens, a solicitação Bluetooth de movimento e direção é anulada. O `H` sozinho não reinicia movimento. A entrada física A0/A1 funciona independentemente do heartbeat, como no sistema original.

## Ligações antigas que **não foram alteradas**

| Nano | Uso no firmware original |
|---|---|
| D2 / D11 | Serial de software para módulo Bluetooth |
| D9 | PWM frente |
| D10 | PWM ré |
| D7 / D8 | Saídas direita e esquerda, ativas em LOW |
| D5 / D4 | Nomes originais `R_EN`/`L_EN`; HIGH até confirmar o chicote; LOW apenas no modo de roda livre confirmado |
| D6 | Pull-up original mantido; **função elétrica incerta** |
| A0 / A1 | Comandos físicos de frente/ré com INPUT_PULLUP |

**Não presumir a topologia:** o comentário original chama D4/D5 de “pinos relés”, enquanto outros comentários citam BTS7960. A identificação da BTS7960 não comprova quais fios vão a `R_EN` e `L_EN`. Por isso a mudança desses pinos permanece bloqueada até confirmação da fiação; sem isso, uma IBT-2 pode frear eletricamente com ambos PWM zero e enables altos.

## Riscos e sequência de validação

1. Antes de energizar, conferir isolação, terminais, polaridade, fixação dos dois motores, lubrificação/folga das engrenagens, rodas, e corrente nominal/estol. Conferir também um fusível correto e **corte físico de potência independente do Nano**.
2. Com **rodas suspensas**, bateria em condição supervisionada e área desocupada, verificar se os comandos e o sentido dos motores continuam corretos. Um relé ou saída invertida pode acelerar de forma inesperada.
3. Após confirmar o cabeamento, verificar os estados ao ligar, ao soltar o botão, ao inverter, ao desligar o Bluetooth, ao bloquear a tela e ao perder energia no Nano. Conferir o nível de D4/D5 e se ocorre roda livre real. Não testar no chão até comprovar a resposta com rodas suspensas.
4. Somente com dados da corrente individual de cada motor, temperatura e comportamento mecânico, ajustar tempos de rampa e limite PWM. Um motor que **ronca sem girar** em PWM baixo pode sobreaquecer; não insistir no comando.
5. Para movimento com ocupantes, é necessário implementar corte de energia por hardware, limites de corrente/temperatura, proteção térmica e validação das frenagens. **Não confiar na interface Bluetooth como freio de emergência.**

Nenhum perfil de desaceleração de software garante que um carro pesado pare dentro de uma distância segura. Não utilizar com criança enquanto estas proteções não forem verificadas.
