#include "led.h"

void init_led(void)
{
  nrf_gpio_cfg_output(led_pin);   //set LED pin to output
}

void led_off(void)
{
  nrf_gpio_pin_write(led_pin, 0);
}

void led_on(void)
{
  nrf_gpio_pin_write(led_pin, 1);
}

void led_blink(uint16_t on_ms, uint16_t off_ms)
{
  led_on();
  nrf_delay_ms(on_ms);
  led_off();
  nrf_delay_ms(off_ms);
}
