#ifndef COMMON_H
#define COMMON_H

#include <stdbool.h>
#include <stdint.h>
#include <string.h>

#include "fds.h"
#include "nrf_sdh.h"
#include "nrf_sdh_soc.h"
#include "app_error.h"
#include "boards.h"
#include "nordic_common.h"
#include "sdk_common.h"
#include "app_util_platform.h"
#include "nrf.h"
#include "nrf_delay.h"

#include "nrf_log.h"
#include "nrf_log_ctrl.h"
#include "nrf_log_default_backends.h"


#define LDO_EN_PIN                      8                                       /** Pinout for LDO Enable */
#define LED_PIN                         17                                      /** Pinout for LED */

#define I2C_SDA_PIN                     30
#define I2C_SCL_PIN                     28

void log_init(void);
void hardware_init(void);
void kill_nrf52(void);

void led_init(void);
void led_off(void);
void led_on(void);
void led_blink(uint16_t on_ms, uint16_t off_ms);

#endif // __COMMON_H