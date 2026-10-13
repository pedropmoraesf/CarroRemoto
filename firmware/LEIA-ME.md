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
| `PASSO_DESACELERACAO` | 3 | Aproximadamente 1,1 s de 160 a 0 |
| `PASSO_PARADA_PRIORITARIA` | 16 | Aproximadamente 0,2 s de 160 a 0 |
| `NEUTRO_ANTES_REVERSAO_MS` | 450 ms | Intervalo adicional em neutro após o PWM chegar a zero |
| `LIMITE_SEM_COMUNICACAO_MS` | 800 ms | Prazo após o último comando válido/heartbeat |

O **tempo de rampa de PWM não representa distância ou tempo real de parada**. Pode haver rolamento, frenagem elétrica, movimento por inércia, travamento ou superaquecimento dependendo da ponte, da bateria e da carga.

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
| D5 / D4 | Habilitações ou relés mantidos em HIGH |
| D6 | Pull-up original mantido; **função elétrica incerta** |
| A0 / A1 | Comandos físicos de frente/ré com INPUT_PULLUP |

**Não presumir a topologia:** o comentário original chama D4/D5 de “pinos relés”, enquanto outros comentários citam BTS7960. Não é seguro alternar D4/D5 para desligar a ponte sem confirmar com multímetro e inspeção do chicote. Uma IBT-2 em configuração habitual pode frear quando os dois PWMs estão em zero com enables altos.

## Riscos e sequência de validação

1. Antes de energizar, conferir isolação, terminais, polaridade, fixação dos dois motores, lubrificação/folga das engrenagens, rodas, e corrente nominal/estol. Conferir também um fusível correto e **corte físico de potência independente do Nano**.
2. Com **rodas suspensas**, bateria em condição supervisionada e área desocupada, verificar se os comandos e o sentido dos motores continuam corretos. Um relé ou saída invertida pode acelerar de forma inesperada.
3. Verificar os estados ao ligar, ao soltar o botão, ao inverter, ao desligar o Bluetooth, ao bloquear a tela e ao perder energia no Nano. Medir o que efetivamente acontece com a ponte H quando o PWM chega a zero.
4. Somente com dados da corrente, temperatura e comportamento mecânico, ajustar tempos de rampa e limite PWM. Um motor que **ronca sem girar** em PWM baixo pode sobreaquecer; não insistir no comando.
5. Para movimento com ocupantes, é necessário implementar corte de energia por hardware, limites de corrente/temperatura, proteção térmica e validação das frenagens. **Não confiar na interface Bluetooth como freio de emergência.**

Nenhum perfil de desaceleração de software garante que um carro pesado pare dentro de uma distância segura. Não utilizar com criança enquanto estas proteções não forem verificadas.
