#include "power.h"

/**@brief Function for initializing power management.
 */
void power_init(void)
{
    NRF_LOG_DEBUG("Power Management Initialized");
    ret_code_t err_code;
    err_code = nrf_pwr_mgmt_init();
    APP_ERROR_CHECK(err_code);
}

void sleep_mode_on_enter(void)
{
    NRF_LOG_DEBUG("Entering Sleep Mode via System On");
    ret_code_t err_code;

    // Go to system-on mode
//    err_code = sd_power_system_on();
//    APP_ERROR_CHECK(err_code);
}

/**@brief Function for putting the chip into sleep mode.
 *
 * @note This function will not return.
 */
void deep_sleep_mode_enter(void)
{
    NRF_LOG_DEBUG("Entering Sleep Mode");
    ret_code_t err_code;

    // Go to system-off mode (this function will not return; wakeup will cause a reset).
    err_code = sd_power_system_off();
    APP_ERROR_CHECK(err_code);
}


/**@brief Function for handling the idle state (main loop).
 *
 * @details If there is no pending log operation, then sleep until next the next event occurs.
 */
void power_handler(void)
{
    if (NRF_LOG_PROCESS() == false)
    {
        nrf_pwr_mgmt_run();
    }
}
  
void enable_dcdc_converter(void)
{
    sd_power_dcdc_mode_set(NRF_POWER_DCDC_ENABLE);
}