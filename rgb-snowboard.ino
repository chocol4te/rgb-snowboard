#include <bitswap.h>
#include <chipsets.h>
#include <color.h>
#include <colorpalettes.h>
#include <colorutils.h>
#include <controller.h>
#include <cpp_compat.h>
#include <dmx.h>
#include <fastled_config.h>
#include <fastled_delay.h>
#include <fastled_progmem.h>
#include <fastpin.h>
#include <fastspi.h>
#include <fastspi_bitbang.h>
#include <fastspi_dma.h>
#include <fastspi_nop.h>
#include <fastspi_ref.h>
#include <fastspi_types.h>
#include <hsv2rgb.h>
#include <led_sysdefs.h>
#include <lib8tion.h>
#include <noise.h>
#include <pixelset.h>
#include <pixeltypes.h>
#include <platforms.h>
#include <power_mgt.h>
#include <FastLED.h>
#define NUM_LEDS 240
#define DATA_PIN 3
CRGB leds[NUM_LEDS];

#include<Wire.h>
const int MPU_addr=0x68;  // I2C address of the MPU-6050
#define ARRAY_SIZE 5 // Number of values to store in running average for each sensor
int16_t AvgAccX,AvgAccY,AvgAccZ,Tmp,AvgGyrX,AvgGyrY,AvgGyrZ;
int16_t AccX[ARRAY_SIZE];
int16_t AccY[ARRAY_SIZE];
int16_t AccZ[ARRAY_SIZE];
int16_t GyrX[ARRAY_SIZE];
int16_t GyrY[ARRAY_SIZE];
int16_t GyrZ[ARRAY_SIZE];
uint8_t index = 0; // Index for arrays

#include<SoftwareSerial.h>
SoftwareSerial Bluetooth(10, 11);
uint8_t inByte;
uint8_t type;
uint8_t data;

uint8_t mode;
uint8_t brightness;

void setup(){
  Wire.begin();
  Wire.beginTransmission(MPU_addr);
  Wire.write(0x6B);  // PWR_MGMT_1 register
  Wire.write(0);     // set to zero (wakes up the MPU-6050)
  Wire.endTransmission(true);
  
  Serial.begin(57600);
  Bluetooth.begin(57600);

  FastLED.addLeds<WS2812, DATA_PIN, RGB>(leds, NUM_LEDS);

  // No need for calibration function as accelerometer and gyro will drift over time anyway.
  // Need bluetooth-checking function
  // Interrupt for bluetooth colour control, also write the android app

  /* Controller --> Arduino bluetooth comm in following format.
     8 bits, 
  0 (LSB) -- Data
  1       -- Data
  2       -- Data
  3       -- Data
  4       -- Data
  5       -- Data
  6       -- Type
  7 (MSB) -- Type


  EG 01000010, will be changing the mode (01) to audio reactive (000010 = 2, 3rd mode).
     10101011 will change the brightness (10) to 43 (101011).
  */
}
void loop(){
  checkBluetooth();
  updateValues(); // Takes approx 2700 microseconds
  //printValues();
  // Calculate new postion and velocity
  // Update LEDs to represent this

  switch mode {
    case 0: { // Color Demo
      
    }
    case 1: { // Motion Reactive
      
    }
    case 2: { // Audio Reactive
      
    }
  }
  delay(100);
}

void updateValues() {
  Wire.beginTransmission(MPU_addr);
  Wire.write(0x3B);  // starting with register 0x3B (ACCEL_XOUT_H)
  Wire.endTransmission(false);
  Wire.requestFrom(MPU_addr,14,true);  // request a total of 14 registers
  GyrX[index]=Wire.read()<<8|Wire.read();  // 0x3B (GYRO_XOUT_H) & 0x3C (GYRO_XOUT_L)    
  GyrY[index]=Wire.read()<<8|Wire.read();  // 0x3D (GYRO_YOUT_H) & 0x3E (GYRO_YOUT_L)
  GyrZ[index]=Wire.read()<<8|Wire.read();  // 0x3F (GYRO_ZOUT_H) & 0x40 (GYRO_ZOUT_L)
          Tmp=Wire.read()<<8|Wire.read();  // 0x41 (TEMP_OUT_H) & 0x42 (TEMP_OUT_L)
  AccX[index]=Wire.read()<<8|Wire.read();  // 0x43 (ACCEL_XOUT_H) & 0x44 (ACCEL_XOUT_L)
  AccY[index]=Wire.read()<<8|Wire.read();  // 0x45 (ACCEL_YOUT_H) & 0x46 (ACCEL_YOUT_L)
  AccZ[index]=Wire.read()<<8|Wire.read();  // 0x47 (ACCEL_ZOUT_H) & 0x48 (ACCEL_ZOUT_L)

  if (index = (ARRAY_SIZE - 1)) {
    index = 0;
  }
  else {
    index++;
  }

  for (uint8_t i = 0; i < ARRAY_SIZE; i++) {
    AvgAccX += AccX[i];
    AvgAccY += AccY[i];
    AvgAccZ += AccZ[i];
    AvgGyrX += GyrX[i]; 
    AvgGyrY += GyrY[i];
    AvgGyrZ += GyrZ[i];
  }
  AvgAccX = AvgAccX / ARRAY_SIZE;
  AvgAccY = AvgAccY / ARRAY_SIZE;
  AvgAccZ = AvgAccZ / ARRAY_SIZE;
  AvgGyrX = AvgGyrX / ARRAY_SIZE;
  AvgGyrY = AvgGyrY / ARRAY_SIZE;
  AvgGyrZ = AvgGyrZ / ARRAY_SIZE;
}

void checkBluetooth() {
  while (Bluetooth.available()) { // While instead of if, just in case multiple bytes occured in between checks. Since each byte contains all the data, just use the latest one.
    inByte = Bluetooth.read();
    // Clear unused bits, leaving just type
    Serial.println(inByte, BIN);
    type = inByte;
    bitClear(type, 2);
    bitClear(type, 3);
    bitClear(type, 4);
    bitClear(type, 5);
    bitClear(type, 6);
    bitClear(type, 7);
    // Clear unused bits and shift right
    data = inByte;
    bitClear(data, 0);
    bitClear(data, 1);
    data = data >> 2;
    
    switch (type) {
      case 0: { // Type: power on/off (00)
        if (data == 0) {
          // Low power, turn off unused devices
        } else {
          // Power on. set brightness to data
        }
      }
      break;
      
      case 1: { // Type: mode (01)
        mode = data;
      }
      break;
      
      case 2: { // Type: brightness (10)
        FastLED.setBrightness(data);
      }
      break;
      
      case 3: { // Type: beacon (11)
        Serial.write(type);
        Serial.print(" ");
        Serial.write(data);
        Serial.println();
      }
      break;
    }
    
  }
}

void printValues() {
  Serial.print("AvgAccX = "); Serial.print(AvgAccX);
  Serial.print(" | AvgAccY = "); Serial.print(AvgAccY);
  Serial.print(" | AvgAccZ = "); Serial.print(AvgAccZ);
  Serial.print(" | Tmp = "); Serial.print(Tmp/340.00+36.53);  //equation for temperature in degrees C from datasheet
  Serial.print(" | AvgGyrX = "); Serial.print(AvgGyrX);
  Serial.print(" | AvgGyrY = "); Serial.print(AvgGyrY);
  Serial.print(" | AvgGyrZ = "); Serial.println(AvgGyrZ);
}
