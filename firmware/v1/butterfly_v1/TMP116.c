#include "TMP116.h"

float tmp116_get_celsius(void)
{
    NRF_LOG_INFO("TMP116_get_celsius");
    float reg_cels_conv = 7812.5; //1 Register unit = 0.0078125 Celcius
    float celsius_counts = (float) tmp116_get_uint16();
    float tmp116_celsius = reg_cels_conv*celsius_counts/1000000;
    NRF_LOG_INFO("Temperature Value: " NRF_LOG_FLOAT_MARKER " C.", NRF_LOG_FLOAT(tmp116_celsius));
    return tmp116_celsius;
}

uint16_t tmp116_get_uint16(void)
{    
    NRF_LOG_INFO("TMP116_get_uint16_t");
    uint8_t tmp116_gnd_address = 0x48;                    //Slave Address for TMP116, ADD0 = GND
    uint8_t tmp116_temp_address = 0x00;                   //Temperature values stored in this Address of TMP116
    uint8_t i2c_temp_counts[2];
    uint8_t temp_register_byte_count = 2;                 //Number of bytes of buffer to read

    i2c_read(tmp116_gnd_address, tmp116_temp_address, i2c_temp_counts, temp_register_byte_count);
    uint16_t uint16_i2c_counts =  (i2c_temp_counts[0] << 8) | i2c_temp_counts[1];
    return uint16_i2c_counts;
}

void tmp116_string_celsius(char *tmp116_temperature)
{
    float int_temp = tmp116_get_celsius();
    sprintf(tmp116_temperature, "%.4f", int_temp);    
}

void tmp116_get_uint8_t(uint8_t *tmp116_uint8_t)
{
    char tmp116_char[5]; //Initializes the Character Array
    tmp116_string_celsius(tmp116_char);

    for(uint8_t i = 0; i<strlen(tmp116_char); i++)
    {
        tmp116_uint8_t[i] = (uint8_t) tmp116_char[i];
    }
}


void configure_tmp116(uint8_t configuration_mode)
{
    uint8_t register_byte_count = 3; //Slave Address + Register Write Value
    uint8_t tmp116_gnd_address = 0x48;                   //Slave Address for TMP116, ADD0 = GND
    
    /*TMP116 unlock EEPROM address for writing to configuration register. */
    uint8_t tmp116_eeprom_unlock_address = 0x04;         //Register for EEPROM Unlock Address
    uint8_t tmp116_eun_value_1 = 0x80;                         //EUN Value to Unlock EEPROM 
    uint8_t tmp116_eun_value_2 = 0x00;                         //EUN Value to Unlock EEPROM 
    
    uint8_t tmp116_eeprom_register_value[3]= {0};
    tmp116_eeprom_register_value[0] = tmp116_eeprom_unlock_address;
    tmp116_eeprom_register_value[1] = tmp116_eun_value_1;
    tmp116_eeprom_register_value[2] = tmp116_eun_value_2;
    uint8_t *tmp116_eeprom_register_value_pointer = tmp116_eeprom_register_value;
    
    i2c_write(tmp116_gnd_address, tmp116_eeprom_register_value_pointer, register_byte_count);

    NRF_LOG_INFO("Unlock EPROM");

    uint8_t shutdown_mode = 0;
    uint8_t continous_mode = 1;
    uint8_t tmp116_config_value_1;
    
    if(configuration_mode == shutdown_mode)
    {
        tmp116_config_value_1 = 0x04;                //Configuration Value to set TMP116 for Shutdown Mode, 0x04
    }
    else if(configuration_mode == continous_mode)
    {
        tmp116_config_value_1 = 0x00;                //Configuration Value to set TMP116 for Continous Conversion, 0x00
    }
    else
    {
        tmp116_config_value_1 = 0x04;     // Default Mode is Shutdown Mode for the TMP116
    }
    
    /*TMP116 Internal register write to Configuration Register. */
    uint8_t tmp116_config_address = 0x01;                //Register for Configuration Register

    uint8_t tmp116_config_value_2 = 0x00;                //Configuration Value to set TMP116
    // MOD[1:0] = 11, ~ Single Shot Mode
    // MOD[1:0] = 00, ~ Continous Conversion Mode
    // MOD[1:0] = 01, ~ Shutdown Mode
    // AVG[1:0] = 00, ~ No Averaging, 15.5 ms Active Conversion Time
    // CONV[2:0] = 000, ~Conversion Cycle Time in CC Mode
    
    uint8_t tmp116_config_register_value[3]= {0};
    tmp116_config_register_value[0] = tmp116_config_address;
    tmp116_config_register_value[1] = tmp116_config_value_1;
    tmp116_config_register_value[2] = tmp116_config_value_2;
    uint8_t *tmp116_config_register_value_pointer = tmp116_config_register_value;
    
    i2c_write(tmp116_gnd_address, tmp116_config_register_value_pointer, register_byte_count);

    NRF_LOG_INFO("Configure Register Function");
    
    /*General-Call Reset Function to reset TMP116 internal registers to power-up values. */
    uint8_t general_call_address = 0x00;                  //General Call Address, ADD0 = GND
    uint8_t general_byte_count = 1;                      //General Call Reset Value
    uint8_t tmp116_general_call_reset = 0x06;             //TMP116 General Call Reset Value
    
    uint8_t tmp116_general_call_reset_value[1]= {0};
    tmp116_general_call_reset_value[0] = tmp116_general_call_reset;
    uint8_t *tmp116_general_call_reset_value_pointer = tmp116_general_call_reset_value;
    

    i2c_write(general_call_address, tmp116_general_call_reset_value_pointer, general_byte_count);
      
    NRF_LOG_INFO("General-Call Reset Function");

    /*Read back the configuration register */
    uint8_t i2c_temp_counts[2];
    uint8_t temp_i2c_register_byte_count = 2; //Number of bytes of buffer to read
    
    i2c_read(tmp116_gnd_address, tmp116_config_address, i2c_temp_counts, temp_i2c_register_byte_count);
    nrf_delay_ms(100);    
    NRF_LOG_INFO("Sent Read Sensor Configuration");
}
