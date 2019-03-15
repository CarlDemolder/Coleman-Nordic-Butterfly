#include "common.h"

/**@brief Function for initializing the nrf log module.
 */
void log_init(void)
{
    NRF_LOG_DEBUG("NRF Log Initialized");
    ret_code_t err_code = NRF_LOG_INIT(NULL);
    APP_ERROR_CHECK(err_code);

    NRF_LOG_DEFAULT_BACKENDS_INIT();
}

void hardware_init(void)
{
    // Initialize.
    nrf_gpio_cfg_output(LDO_EN_PIN);      // set LDO_EN pin to output
    nrf_gpio_pin_write(LDO_EN_PIN, 1);     // enable the LDO to keep the MCU alive
    led_init();
    led_blink(500, 500);
}

void kill_nrf52(void)
{
    nrf_gpio_pin_write(LDO_EN_PIN, 0);     // Disabling the LDO to kill the MCU
}

void led_init(void)
{
  nrf_gpio_cfg_output(LED_PIN);   //set LED pin to output
}

void led_off(void)
{
  nrf_gpio_pin_write(LED_PIN, 0);
}

void led_on(void)
{
  nrf_gpio_pin_write(LED_PIN, 1);
}

void led_blink(uint16_t on_ms, uint16_t off_ms)
{
  led_on();
  nrf_delay_ms(on_ms);
  led_off();
  nrf_delay_ms(off_ms);
}