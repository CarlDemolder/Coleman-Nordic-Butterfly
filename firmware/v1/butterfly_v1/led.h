#ifndef LED_H
#define LED_H

#include "common.h"

#define led_pin 12
void led_init(void);
void led_off(void);
void led_on(void);
void led_blink(uint16_t on_ms, uint16_t off_ms);


#endif // LED_H