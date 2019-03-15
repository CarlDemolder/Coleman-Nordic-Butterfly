#ifndef __POWER_H__
#define __POWER_H__

#include "common.h"
#include "nrf_pwr_mgmt.h"
#include "nrfx_power.h"
#include "nrfx_power_clock.h"



void power_init(void);
void sleep_mode_on_enter(void);
void deep_sleep_mode_enter(void);
void power_handler(void);
void enable_dcdc_converter(void);

#endif /*__POWER_H__*/