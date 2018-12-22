#ifndef __TMP116_H
#define __TMP116_H

#include "common.h"
#include "i2c.h"

//TMP116 definitions

float tmp116_get_celsius(void);
uint16_t tmp116_get_uint16(void);
void tmp116_string_celsius(char *tmp116_temperature);
void tmp116_get_uint8_t(uint8_t *tmp116_uint8);
void configure_tmp116(uint8_t configuration_mode);

#endif // __TMP116_H