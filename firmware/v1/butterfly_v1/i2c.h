#ifndef I2C_H
#define I2C_H

#include "common.h"
#include "nrfx_twim.h"

void twi_init(void);
void i2c_write(uint8_t slave_address, uint8_t const* array_data, uint8_t array_size);
void i2c_read(uint8_t slave_address, uint8_t register_address, uint8_t* array_data, uint8_t array_size); 
void i2c_stop(void);
void i2c_start(void);


#endif // __I2C_H