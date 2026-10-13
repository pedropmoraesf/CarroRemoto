//#include<ESP8266WiFi.h>
#include <SoftwareSerial.h>
SoftwareSerial BTSerial(2,11); // RX, TX
//SoftwareSerial mySerial(8,9);
 
 
 

 //int pino_R=5;
 //int pino_L=6;
 char vel_buffer[3];

 int vel=100;


//pinos reles
int pino_R_EN=5; //fio verde
int pino_bts_vin=6;//fio amarelo/azul
int pino_L_EN=4; //fio azul/marrom
//int pino_R_EN=12; //fio roxo/verde

//pinos de transistores?
int pino_frente=9;//fio roxo/branco
int pino_frente_analog=A0;

int pino_tras=10; //fio azul/cinza
int pino_tras_analog=A1;
// int j=255;
//pino voltagem entrada bluetooth
//int pino_blue_vin=2;
//int estado_blue=A2;

//pinos da bts7960
//int rel_gnd=33;
//int rel_vin=43;
int pino_R=7;
int pino_L=8;
int i=0;


char data;

 //int i =0;

//long tempo=0;

void setup(){
 
  
  Serial.begin(9600);
  BTSerial.begin(9600);
  
  delay(100);
  
  
  
  
  pinMode(pino_R, OUTPUT);
  pinMode(pino_L, OUTPUT);
  pinMode(pino_frente, OUTPUT);
  pinMode(pino_tras, OUTPUT);
  //pinMode(rel_gnd, OUTPUT);
  //pinMode(rel_vin, OUTPUT);
  //pinMode(pino_blue_vin, OUTPUT);
   
  // pinMode(pino_bts_vin,OUTPUT);
  pinMode(pino_R_EN, OUTPUT);
  pinMode(pino_L_EN, OUTPUT);
  //pinMode(pino_buzina, OUTPUT);
  
  //pinMode(pino_son, OUTPUT);
   pinMode(pino_frente_analog, INPUT_PULLUP);
   pinMode(pino_tras_analog, INPUT_PULLUP);
   //pinMode(estado_blue,INPUT);
  digitalWrite(pino_R, HIGH);
  digitalWrite(pino_L, HIGH);
  //digitalWrite(pino_farol, LOW);
  //digitalWrite(pino_farol_alto, LOW);
  //digitalWrite(pino_buzina, LOW);
  

  //digitalWrite(pino_bts_vin, HIGH);
  //digitalWrite(rel_vin, HIGH);
  //digitalWrite(rel_gnd, LOW);
  //digitalWrite(pino_blue_vin, HIGH);
  
  digitalWrite(pino_bts_vin, HIGH);
  digitalWrite(pino_R_EN, HIGH);
  digitalWrite(pino_L_EN, HIGH);

  Serial.print("Inicializado...");
//j=255;
  
  //analogWrite(pino_son,170);
}

// void virar(int dire, int vel=255){

// if (dire==1){

//   analogWrite(pino_L, 0);
//   analogWrite(pino_R, vel);
//   //digitalWrite(pino_LUZ_teste, HIGH);
// } 

// else if(dire==0) {

//   analogWrite(pino_R, 0);
//   analogWrite(pino_L, vel);
  
// }
// }

// long extrairNumero(String str) {
//   String apenasDigitos = "";

//   for (byte i = 0; i < str.length(); i++) {
//     char c = str.charAt(i);
    
//     // Se for um dígito, adiciona à nossa nova string
//     if (isdigit(c)) {
//       apenasDigitos += c;
//     }
//   }

//    if (apenasDigitos.length() > 0) {
//     return apenasDigitos.toInt();
//   } else {
//     return -1; // Retorna -1 (ou 0) se não encontrar nenhum número
//   }
// }

void loop(){
  
// if (millis()-tempo>30000){
//   digitalWrite(pino_R, HIGH);
//   digitalWrite(pino_L, HIGH);
//   digitalWrite(pino_frente, HIGH);
//   digitalWrite(pino_tras, HIGH);
//   tempo=millis();
// } Serial.println(BTSerial.available());
 //
 //data='A';
 delay(10);
  
 if (BTSerial.available() ) { // Or use BTSerial.available() for Bluetooth
     //tempo=millis();
    

    data = toupper(BTSerial.read());
    // Serial.println(data);
    
    
    // if(data=='+'||data=='#'){
      
    //    return;//ta querendo disconectar...
    // }

 }
    //i+=1;
    
   // if (data=='|'){
      //vel="";
     // while(vel.length()==0){
        //vel= BTSerial.parseInt();
       // String vel_str = BTSerial.readStringUntil(';');
    
    // if ((data-'0' )>= 0 && (data-'0') <= 9) {
    //   vel = map(data - '0', 0, 9, 0, 255); 
    //   Serial.println("foi"+vel);
    // } 
    
   // String s=Serial.readString(); 

    if (isDigit(data)) {
      
      
      vel_buffer[i]=data;
      i+=1;
      return;
      // Adiciona o caractere à nossa string
    }else if(data==';'||i==3){
      
      i=0;

      if(atoi(vel_buffer)>0){  
        
        
       vel=atoi(vel_buffer);//extrairNumero(vel));
        Serial.println(atoi(vel_buffer));
       }
      vel_buffer[0]='\0';
      vel_buffer[1]='\0';
      vel_buffer[2]='\0';
      i=0;
    }
    
    
       // if (extrairNumero(vel)>-1){


   // 
    //}
    //analogWrite(pino_tras,vel); 
    
    if (data=='R'){

    
    digitalWrite(pino_R, LOW);
    
    }else if (data=='L'){

    digitalWrite(pino_L, LOW);
    

    }else if(data=='Z'||data=='X') {
    
    digitalWrite(pino_L, HIGH);
    digitalWrite(pino_R, HIGH);

    }
    
    if (data=='F'|| digitalRead(pino_frente_analog)==0){
    
    analogWrite(pino_frente,vel);
    analogWrite(pino_tras,0);
    

    // Turn LED ON
    }else if (data=='T'||digitalRead(pino_tras_analog)==0){
    
    analogWrite(pino_tras,vel); 
    analogWrite(pino_frente,0);
    

    }else if (data=='W'|| data=='Y') {
    // int vel_min=10;
    // float des_fator=.25f;
        analogWrite(pino_frente, 0);
        analogWrite(pino_tras, 0);

    }else if (digitalRead(pino_frente_analog) == 1 || digitalRead(pino_tras_analog) == 1) {

        if (data!='R' && data!='L'){
        analogWrite(pino_frente,0);
        analogWrite(pino_tras, 0);
        }
    }
   

    
    

      
}
