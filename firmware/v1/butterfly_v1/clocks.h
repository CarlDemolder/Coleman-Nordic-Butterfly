#ifndef __CLOCKS_H__
#define __CLOCKS_H__

#include "common.h"
#include "bluetooth.h"
#include "TMP116.h"
#include "i2c.h"

#include "nrfx_clock.h"
#include "nrfx_rtc.h"

typedef void (*app_rtc_handler_t)(void);
typedef void (*app_clock_handler_t)(void);

void clocks_init(void);
void hfclock_restart(void);
void hfclock_stop(void);
void lfclock_restart(void);
void lfclock_stop(void);
void rtc_config(void);
void rtc_handler(nrfx_rtc_int_type_t int_type);
void rtc_set_counter(uint8_t new_sampling_interval);
void rtc_start(void);
void rtc_stop(void);

#endif /*__CLOCKS_H__*/