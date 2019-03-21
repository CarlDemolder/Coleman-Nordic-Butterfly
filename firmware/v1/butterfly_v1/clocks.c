#include "clocks.h"

const nrfx_rtc_t rtc = NRFX_RTC_INSTANCE(2); /**< Declaring an instance of nrfx_rtc for RTC1. RTC0 is reserved for Soft Device */

uint8_t nrfx_rtc_is_running = 0;
uint8_t nrfx_rtc_restart = 0;    // Constant used to restart RTC counter if the phone disconnects if already connected

static app_rtc_handler_t m_app_rtc_handler;
static app_clock_handler_t m_app_clock_handler;

/**@brief Function to initialize the HF clock and the NRF Clocks
 */

void clocks_init(void)
{
    NRF_LOG_DEBUG("Clocks Init");
    // Initialize Clock module.
//    ret_code_t err_code = nrfx_clock_init(nrfx_clock_irq_handler);
//    APP_ERROR_CHECK(err_code);
//
//    nrfx_clock_hfclk_start();
    lfclock_restart();
}

/**@brief Function to restart the HF clock
 */
void hfclock_restart(void)
{
    if(!nrfx_clock_hfclk_is_running())
    {
        nrfx_clock_hfclk_start();
    }
}

/**@brief Function to stop the HF clock
 */
void hfclock_stop(void)
{
    if(nrfx_clock_hfclk_is_running())
    {
        nrfx_clock_hfclk_stop();
    }
}

/**@brief Function to restart the LF clock
 */
void lfclock_restart(void)
{
    if(!nrfx_clock_lfclk_is_running())
    {
        nrfx_clock_lfclk_start();
        NRF_LOG_INFO("NRFX Clock LFCLK Started");
    }
}

/**@brief Function to stop the LF clock
 */
void lfclock_stop(void)
{
    if(nrfx_clock_lfclk_is_running())
    {
        nrfx_clock_lfclk_stop();
    }
}


/**@brief Function for the RTC initialization.
 *
 * @details Initializes the Clocks. This creates and starts RTC timers.
 */
void rtc_config(void)
{
    uint32_t err_code;
    NRF_LOG_DEBUG("rtc_config");

    //Initialize RTC instance
    nrfx_rtc_config_t config = NRFX_RTC_DEFAULT_CONFIG;
    config.prescaler = 4095;    // This sets the frequency of the RTC to 8 Hz. 
    err_code = nrfx_rtc_init(&rtc, &config, rtc_handler);
    APP_ERROR_CHECK(err_code);

}

/**@brief Function for handling the measurement timer timeout.
 *
 * @details This function will be called each time the measurement timer expires.
 *
 */ 
void rtc_handler(nrfx_rtc_int_type_t int_type)
{
    if (int_type == NRFX_RTC_INT_COMPARE2)
    {
        nrfx_rtc_counter_clear(&rtc);
        nrfx_rtc_int_enable(&rtc, NRF_RTC_INT_COMPARE2_MASK);
        NRF_LOG_DEBUG("rtc_handler(COMPARE)");

        static uint8_t send = 1;
        NRF_LOG_INFO("rtc_handler() %d", send);
        send++;

        ret_code_t err_code;
        
        led_blink(500, 500);
        i2c_start();      // Powering on the I2C Module
        configure_tmp116(1);    // Setting the TMP116 to Automatic mode
        uint8_t tmp116_uint8_t[5];
        tmp116_get_uint8_t(tmp116_uint8_t); //Places the TMP116 Temperature in Character Vector
        update_temperature_characteristic(tmp116_uint8_t);    // Update the BLE GATT Database with the new Temperature Value
        configure_tmp116(0);    // Setting the TMP116 into Shutdown Mode 
        i2c_stop();       // Powering off the I2C Module
    }
}

void rtc_set_counter(uint8_t new_sampling_interval)
{
    NRF_LOG_INFO("RTC Set Counter");
    uint32_t err_code;
    NRF_LOG_INFO("rtc_set_counter()-new_sampling_interval %d", new_sampling_interval);
    uint32_t clock_interval_range[16] = {10, 30, 60, 300, 600, 1800, 3600, 18000}; 
    uint32_t rtc_clock_interval = (uint32_t) clock_interval_range[new_sampling_interval];
    NRF_LOG_INFO("rtc_set_counter()-rtc_clock_interval %d", rtc_clock_interval);
    // Set compare channel to trigger interrupt after sampling_interval seconds
    err_code = nrfx_rtc_cc_set(&rtc, 2, rtc_clock_interval*8, true);
    APP_ERROR_CHECK(err_code);
}

void rtc_stop(void)
{
    if(nrfx_rtc_is_running)
    {
        nrfx_rtc_disable(&rtc);        // Power off RTC instance
        nrfx_rtc_is_running = 0;
        nrfx_rtc_restart = 1;
        NRF_LOG_INFO("RTC Disabled");
    }
}

void rtc_start(void)
{
    NRF_LOG_INFO("rtc_start");
    if(!nrfx_rtc_is_running)
    {
        nrfx_rtc_enable(&rtc);        // Power on RTC instance
        nrfx_rtc_int_enable(&rtc, NRF_RTC_INT_COMPARE2_MASK);
        nrfx_rtc_tick_enable(&rtc, true);
        nrfx_rtc_is_running = 1;
        NRF_LOG_INFO("RTC Enabled");
    }
}

void rtc_restart(void)
{
    NRF_LOG_INFO("rtc_restart");
    if(!nrfx_rtc_is_running && nrfx_rtc_restart)
    {
        nrfx_rtc_enable(&rtc);        // Power on RTC instance
        nrfx_rtc_int_enable(&rtc, NRF_RTC_INT_COMPARE2_MASK);
        nrfx_rtc_tick_enable(&rtc, true);
        nrfx_rtc_is_running = 1;
        nrfx_rtc_restart = 0;
        NRF_LOG_INFO("RTC Restarted");
    }
}