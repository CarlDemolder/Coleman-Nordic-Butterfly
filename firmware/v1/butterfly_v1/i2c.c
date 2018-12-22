#include "i2c.h"

/* TWI instance ID. */
#define TWI_INSTANCE_ID     0

/* Indicates if operation on TWI has ended. */
static volatile bool m_xfer_done = false;

/* TWI instance. */
static const nrf_drv_twi_t m_twi_0 = NRF_DRV_TWI_INSTANCE(TWI_INSTANCE_ID);
/**
 * @brief TWI events handler.
 * The point of the TWI event handler is that it is used for current consumption
 * When the MCU is transferring data over I2C, it can go into sleep mode
 */
void twi_handler(nrf_drv_twi_evt_t const * p_event, void * p_context)
{
    NRF_LOG_DEBUG("TWI_HANDLER Event Occured");
    switch (p_event->type)
    {
        case NRF_DRV_TWI_EVT_DONE:
            if (p_event->xfer_desc.type == NRF_DRV_TWI_XFER_RX)
            {
                NRF_LOG_INFO("I2C Receiving Data");
            }
            m_xfer_done = true;
            break;
        default:
            break;
    }
}

/*
 * Initialize I2C module, called Two Wire Interface (TWI)
 * Setting Pin P0.00 to SDA
 * Setting Pin P0.05 to SCL
 * Setting I2C frequency to 100 kHz
 * Giving it a low priority to not disturb BLE and Power Interrupts
 */
void twi_init(void)
{
    NRF_LOG_INFO("twi_init");
    ret_code_t err_code;

    const nrf_drv_twi_config_t twi_TMP116_config = 
    {
       .scl                = 5,
       .sda                = 0,
       .frequency          = NRF_TWI_FREQ_100K,
       .interrupt_priority = APP_IRQ_PRIORITY_HIGH,
       .clear_bus_init     = false
    };

    err_code = nrf_drv_twi_init(&m_twi_0, &twi_TMP116_config, twi_handler, NULL);
    APP_ERROR_CHECK(err_code);

    nrf_drv_twi_enable(&m_twi_0);
}

void i2c_write(uint8_t slave_address, uint8_t const* array_data, uint8_t array_size) 
{
    m_xfer_done = false;
    NRF_LOG_INFO("I2C_write");
    ret_code_t error_code = nrf_drv_twi_tx(&m_twi_0, slave_address, array_data, array_size, false);

    APP_ERROR_CHECK(error_code);
    while(m_xfer_done == false);
    nrf_delay_ms(10);
}

void i2c_read(uint8_t slave_address, uint8_t register_address, uint8_t* array_data, uint8_t array_size)
{
    m_xfer_done = false;
    NRF_LOG_INFO("I2C_read");
    ret_code_t error_code = nrf_drv_twi_tx(&m_twi_0, slave_address, &register_address, 1, false);
    APP_ERROR_CHECK(error_code);
    while(m_xfer_done == false);
    if(NRF_SUCCESS == error_code)
    {
      error_code = nrf_drv_twi_rx(&m_twi_0, slave_address, array_data, array_size);
    }
    APP_ERROR_CHECK(error_code);
    while(m_xfer_done == false);
    nrf_delay_ms(10);
}