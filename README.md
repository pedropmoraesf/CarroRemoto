# Controle Carro Remoto

Aplicativo Android para controlar um carro infantil elétrico por Bluetooth clássico (SPP), com firmware para Arduino Nano em `firmware/sketch_jan20a/`.

## Operação

1. Pareie o módulo Bluetooth do carro nas configurações do Android.
2. Abra o aplicativo em modo paisagem. Toque em **MAC** para conferir ou trocar o endereço do módulo. O endereço é salvo no aparelho.
3. Toque em **Conectar** e conceda a permissão Bluetooth quando solicitada.
4. Após a conexão, pressione **Liberar**. A movimentação fica bloqueada por padrão.
5. Toque e segure as setas para dirigir/virar; solte para enviar o comando correspondente de parada. A barra central ajusta a potência entre os valores PWM 120 e 250.
6. **PARAR** envia comandos para parar tração e direção e bloqueia novamente os controles.
7. Ao sair do aplicativo ou perder o foco, os controles são bloqueados e é tentado o envio da parada.

## Protocolo de comunicação existente

- `F`: frente; `T`: ré; `Y` ou `W`: parar tração.
- `R`: direita; `L`: esquerda; `Z` ou `X`: parar direção.
- `120;` a `250;`: PWM solicitado. O firmware trata esses comandos como caracteres recebidos pelo serial Bluetooth.

## Atenção à segurança

**O comando de parada pelo aplicativo NÃO é um sistema de parada de emergência confiável.**
O firmware atual mantém o último comando de movimento se a conexão Bluetooth cair, pois não possui um temporizador de segurança que desligue a tração na ausência de mensagens. Se o Bluetooth falhar, o botão PARAR também pode deixar de funcionar.

Antes de colocar crianças ou pessoas no carro, é necessário implementar e testar **no Arduino** um timeout de comunicação e um mecanismo físico de corte da alimentação dos motores. Faça os testes de software com as rodas suspensas e sem ocupantes. Nenhuma modificação no firmware foi feita nesta atualização.

## Desenvolvimento

- Android Studio, Java, Android SDK 33, Gradle 7.5.
- Código do aplicativo: `app/src/main/`.
- Firmware original do Nano: `firmware/sketch_jan20a/sketch_jan20a.ino`.
- O projeto utiliza bibliotecas Android Support antigas; uma migração futura para AndroidX deve ser realizada em uma alteração separada.
