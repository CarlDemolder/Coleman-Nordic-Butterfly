//Based on twi_sensor example. Mark's code begins at line 179.

/**
 * Copyright (c) 2015 - 2018, Nordic Semiconductor ASA
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form, except as embedded into a Nordic
 *    Semiconductor ASA integrated circuit in a product or a software update for
 *    such product, must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 * 
 * 3. Neither the name of Nordic Semiconductor ASA nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 * 
 * 4. This software, with or without modification, must only be used with a
 *    Nordic Semiconductor ASA integrated circuit.
 * 
 * 5. Any software provided in binary form under this license must not be reverse
 *    engineered, decompiled, modified and/or disassembled.
 * 
 * THIS SOFTWARE IS PROVIDED BY NORDIC SEMICONDUCTOR ASA "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, NONINFRINGEMENT, AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
/** @file
 * @defgroup tw_sensor_example main.c
 * @{
 * @ingroup nrf_twi_example
 * @brief TWI Sensor Example main file.
 *
 * This file contains the source code for a sample application using TWI.
 *
 */

#include <stdio.h>
#include "boards.h"
#include "app_util_platform.h"
#include "app_error.h"
#include "nrf_drv_twi.h"
#include "nrf_delay.h"


#include "nrf_log.h"
#include "nrf_log_ctrl.h"
#include "nrf_log_default_backends.h"

/* TWI instance ID. */
#define TWI_INSTANCE_ID     0

/* Common addresses definition for temperature sensor. */
#define LM75B_ADDR          (0x90U >> 1)

#define LM75B_REG_TEMP      0x00U
#define LM75B_REG_CONF      0x01U
#define LM75B_REG_THYST     0x02U
#define LM75B_REG_TOS       0x03U

/* Mode for LM75B. */
#define NORMAL_MODE 0U

/* Indicates if operation on TWI has ended. */
static volatile bool m_xfer_done = false;

/* TWI instance. */
static const nrf_drv_twi_t m_twi = NRF_DRV_TWI_INSTANCE(TWI_INSTANCE_ID);

/* Buffer for samples read from temperature sensor. */
static uint8_t m_sample[2] = {0,0};

/**
 * @brief Function for setting active mode on MMA7660 accelerometer.
 */
void LM75B_set_mode(void)
{
    ret_code_t err_code;

    /* Writing to LM75B_REG_CONF "0" set temperature sensor in NORMAL mode. */
    uint8_t reg[2] = {LM75B_REG_CONF, NORMAL_MODE};
    err_code = nrf_drv_twi_tx(&m_twi, LM75B_ADDR, reg, sizeof(reg), false);
    APP_ERROR_CHECK(err_code);
    while (m_xfer_done == false);

    /* Writing to pointer byte. */
    reg[0] = LM75B_REG_TEMP;
    m_xfer_done = false;
    err_code = nrf_drv_twi_tx(&m_twi, LM75B_ADDR, reg, 1, false);
    APP_ERROR_CHECK(err_code);
    while (m_xfer_done == false);
}

/**
 * @brief Function for handling data from temperature sensor.
 *
 * @param[in] temp          Temperature in Celsius degrees read from sensor.
 */
__STATIC_INLINE void data_handler(uint8_t temp)
{
    //NRF_LOG_INFO("Temperature: %d Celsius degrees.", temp);
}

/**
 * @brief TWI events handler.
 */
void twi_handler(nrf_drv_twi_evt_t const * p_event, void * p_context)
{
    switch (p_event->type)
    {
        case NRF_DRV_TWI_EVT_DONE:
            if (p_event->xfer_desc.type == NRF_DRV_TWI_XFER_RX)
            {
                data_handler(m_sample);
            }
            m_xfer_done = true;
            break;
        default:
            break;
    }
}

/**
 * @brief UART initialization.
 */
void twi_init (void)
{
    ret_code_t err_code;

    const nrf_drv_twi_config_t twi_TMP116_config = {
       .scl                = 5,
       .sda                = 0,
       .frequency          = NRF_DRV_TWI_FREQ_100K,
       .interrupt_priority = APP_IRQ_PRIORITY_HIGH,
       .clear_bus_init     = false
    };

    err_code = nrf_drv_twi_init(&m_twi, &twi_TMP116_config, twi_handler, NULL);
    APP_ERROR_CHECK(err_code);

    nrf_drv_twi_enable(&m_twi);
}

/**
 * @brief Function for reading data from temperature sensor.
 */
static void read_sensor_data()
{
    m_xfer_done = false;

    /* Read 1 byte from the specified address - skip 3 bits dedicated for fractional part of temperature. */
    ret_code_t err_code = nrf_drv_twi_rx(&m_twi, LM75B_ADDR, &m_sample, sizeof(m_sample));
    APP_ERROR_CHECK(err_code);
}

uint8_t reg_tx[3] = {0,0};
uint8_t reg_rx[2] = {0,0};
uint16_t word;

/**
 * @brief Function for main application entry.
 */


#define p_LED 12
#define p_LDO_EN 8
#define p_INT_TMP 1
#define p_INT_IMU 6

//TMP116 definitions
#define TMP116_ADDR         (0x90U >> 1)
#define TMP116_REG_ID       0x0F
#define TMP116_REG_CONFIG   0x01

//BMI160 definitions
#define BMI160_ADDR         0x69 //address when SDO is floating or HIGH. 0x68 if SD is grounded. 
#define BMI160_REG_CHIP_ID  0x00
#define BMI160_REG_INT_EN_0 0x50
#define BMI160_REG_INT_OUT_CTRL 0x53
#define BMI160_REG_INT_STATUS_0 0x1C


void LED_off()
{nrf_gpio_pin_write(p_LED, 0);}

void LED_on()
{nrf_gpio_pin_write(p_LED, 1);}

void LED_blink(uint16_t on_ms, uint16_t off_ms)
{
  LED_on();
  nrf_delay_ms(on_ms);
  LED_off();
  nrf_delay_ms(off_ms);
}

void config_butterfly_pins()
{
  //set LED pin to output
  nrf_gpio_cfg_output(p_LED);

  //set LDO_EN pin to output
  nrf_gpio_cfg_output(p_LDO_EN);

  //set INT_TMP pin to input
  nrf_gpio_cfg_input(p_INT_TMP, NRF_GPIO_PIN_NOPULL);

  //set INT_IMU pin to input
  nrf_gpio_cfg_input(p_INT_IMU, NRF_GPIO_PIN_NOPULL);

  //set trace pins to output for extra LEDs
  nrf_gpio_cfg_output(14); //TRACEDATA[3]
  nrf_gpio_cfg_output(15); //TRACEDATA[2]
  nrf_gpio_cfg_output(16); //TRACEDATA[1]
  nrf_gpio_cfg_output(18); //TRACEDATA[0]
  nrf_gpio_cfg_output(20); //TRACECLK
}




int main(void)
{
  uint16_t i;
  
  //these 4 lines are for the demo board example. Not used here. 
  //APP_ERROR_CHECK(NRF_LOG_INIT(NULL));
  //NRF_LOG_DEFAULT_BACKENDS_INIT();
  //bsp_board_init(BSP_INIT_LEDS);
  //bsp_board_led_on(3); //turn off LED
  
  //set all inputs/outputs
  config_butterfly_pins();

  //enable the LDO to keep the MCU alive
  nrf_gpio_pin_write(p_LDO_EN,1);

  //flash green LED for 1 second at startup
  LED_blink(1000,1000);

  //initialize TWI (I2C)
  twi_init();
  

  //set TMP116 pointer to ID register
  reg_tx[0] = TMP116_REG_ID;
  nrf_drv_twi_tx(&m_twi, TMP116_ADDR, reg_tx, 1, false);
  nrf_delay_us(300);

  //set BMI160 pointer to CHIP_ID register
  reg_tx[0] = BMI160_REG_CHIP_ID;
  nrf_drv_twi_tx(&m_twi, BMI160_ADDR, reg_tx, 1, false);
  nrf_delay_us(300);


  //loop will read ID registers from TMP116 and BMI160 and flash the green LED for each read if successful. 
  for (i = 0; i < 3; i++)
  {
    //read ID register, 2 bytes. Expect 0xX116. Result must be stitched together into single 16-bit word. 
    nrf_drv_twi_rx(&m_twi, TMP116_ADDR, &reg_rx, 2); 
    nrf_delay_us(500); //delay while I2C transfer happens. Can be replaced with interrupt (see twi_sensor example). 
    word = (uint16_t)(reg_rx[0] & 0x0F) << 8;
    word = word + reg_rx[1];

    //if ID register is 0x0116 flash 500ms on, 500ms off. Do not flash at all if the ID is bad. 
    if (word == 0x0116)
    {
      LED_blink(500,500);
    }
    else
    {
      nrf_delay_ms(1000);
    }
    
    //if ID register is 0xD1, flash 100ms on, 1900ms off. 
    nrf_drv_twi_rx(&m_twi, BMI160_ADDR, &reg_rx, 1); 
    nrf_delay_us(300); //delay while I2C transfer happens. Can be replaced with interrupt (see twi_sensor example). 
    if (reg_rx[0] == 0xD1)
    {
      LED_blink(100,1900);
    }
    else
    {
      nrf_delay_ms(2000);
    }
  }


 

  //flash the LED in shorter and shorter pulses, then shut itself off by disabling the LDO
  for (i = 20; i > 0; i--)
  {
    LED_blink(i*25,i*25);
  }
  nrf_gpio_pin_write(p_LDO_EN,0);
  nrf_delay_ms(1000);


  while(1)
  {}


//Unused code below

/*
  //enable anymotion interrupt
  reg_tx[0] = BMI160_REG_INT_EN_0;
  reg_tx[1] = 0x07;
  nrf_drv_twi_tx(&m_twi, BMI160_ADDR, reg_tx, 2, false);






  while(1)
  {
    //set BMI160 pointer to INT_OUT_CTRL and enable interrupt 1, active low
    reg_tx[0] = BMI160_REG_INT_OUT_CTRL;
    reg_tx[1] = 0x08;
    nrf_drv_twi_tx(&m_twi, BMI160_ADDR, reg_tx, 2, false);
    nrf_gpio_pin_write(p_LED, nrf_gpio_pin_read(p_INT_IMU));
    nrf_delay_ms(500);
    //set BMI160 pointer to INT_OUT_CTRL and enable interrupt 1, active high
    reg_tx[0] = BMI160_REG_INT_OUT_CTRL;
    reg_tx[1] = 0x0A;
    nrf_drv_twi_tx(&m_twi, BMI160_ADDR, reg_tx, 2, false);
    nrf_gpio_pin_write(p_LED, nrf_gpio_pin_read(p_INT_IMU));
    nrf_delay_ms(500);
  }


  while(1)
  {
    
    //set TMP116 pointer to CONFIG register and set alert to HIGH polarity
    reg_tx[0] = TMP116_REG_CONFIG;
    reg_tx[1] = 0x02;
    reg_tx[2] = 0x28;
    nrf_drv_twi_tx(&m_twi, TMP116_ADDR, reg_tx, 3, false);
    nrf_delay_ms(500);
    nrf_gpio_pin_write(p_LED, nrf_gpio_pin_read(p_INT_TMP));

    //set TMP116 pointer to CONFIG register and set alert to LOW polarity
    reg_tx[0] = TMP116_REG_CONFIG;
    reg_tx[1] = 0x02;
    reg_tx[2] = 0x20;
    nrf_drv_twi_tx(&m_twi, TMP116_ADDR, reg_tx, 3, false);
    nrf_delay_ms(500);
    nrf_gpio_pin_write(p_LED, nrf_gpio_pin_read(p_INT_TMP));
  }

    while (true)
    {
      //read config register
      nrf_drv_twi_rx(&m_twi, TMP116_ADDR, &reg, 2);
      nrf_delay_ms(10);
      

      word = reg[0] << 8 + reg[1];

      //if config register is 0x0220 flash for 0.5 second. If not, flash for 0.1 second.
      if (word == 0x0220)
      {
        bsp_board_led_on(3);
        nrf_delay_ms(500);
        bsp_board_led_off(3);
        nrf_delay_ms(500);
      }
      else
      {
        bsp_board_led_on(3);
        nrf_delay_ms(900);
        bsp_board_led_off(3);
        nrf_delay_ms(100);
      }
    }*/

}

/** @} */
