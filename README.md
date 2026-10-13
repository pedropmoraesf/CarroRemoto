# Controle Carro Remoto

Aplicativo Android para controlar um carro infantil elétrico por Bluetooth clássico (SPP), com firmware para Arduino Nano em `firmware/sketch_jan20a/`.

## Operação

1. Pareie o módulo Bluetooth do carro nas configurações do Android.
2. Abra o aplicativo em modo paisagem. Toque em **MAC** para conferir ou trocar o endereço do módulo. O endereço é salvo no aparelho.
3. Toque em **Conectar** e conceda a permissão Bluetooth quando solicitada.
4. Após a conexão, pressione **Liberar**. A movimentação fica bloqueada por padrão.
5. Toque e segure as setas para dirigir/virar; solte para enviar o comando correspondente de parada. A barra central ajusta a potência solicitada de PWM 120 a 160 (limite inicial conservador).
6. **PARAR** envia comandos para parar tração e direção e bloqueia novamente os controles.
7. Ao sair do aplicativo ou perder o foco, os controles são bloqueados e é tentado o envio da parada.

## Protocolo de comunicação existente

- `F`: frente; `T`: ré; `Y` ou `W`: parar tração.
- `R`: direita; `L`: esquerda; `Z` ou `X`: parar direção.
- `120;` a `160;`: faixa atualmente oferecida no app; o firmware aceita valores de 1 a 255, mas limita a 160 no modo de teste. `H` mantém a comunicação ativa e `!` solicita parada prioritária.

## Atenção à segurança

**A parada pelo aplicativo NÃO substitui um corte físico de energia.**
O firmware atualizado em `firmware/sketch_jan20a/` inclui desaceleração gradual, intervalo antes de inverter o sentido e timeout de 800 ms sem sinal Bluetooth. O aplicativo envia `H` periodicamente ao segurar uma seta e `!YZ` ao solicitar parada prioritária. Mesmo assim, a perda do sinal, uma falha elétrica ou uma ponte H com frenagem ativa podem produzir movimentos ou travamentos inesperados.

Antes de transportar pessoas, instale e teste um **interruptor físico independente do Arduino** para cortar a energia dos motores, confira o cabeamento e meça as correntes e temperaturas em ensaios supervisionados, sem ocupantes e inicialmente com as rodas suspensas. Consulte `firmware/LEIA-ME.md` para as limitações, parâmetros e orientações de validação.

## Desenvolvimento

- Android Studio, Java, Android SDK 33, Gradle 7.5.
- Código do aplicativo: `app/src/main/`.
- Firmware atualizado do Nano: `firmware/sketch_jan20a/sketch_jan20a.ino`; original integral: `firmware/historico/sketch_jan20a_original.ino.txt`.
- O projeto utiliza bibliotecas Android Support antigas; uma migração futura para AndroidX deve ser realizada em uma alteração separada.
