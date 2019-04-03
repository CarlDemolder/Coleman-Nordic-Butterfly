#ifndef __BLUETOOTH_H__
#define __BLUETOOTH_H__

#include "common.h"
#include "ble_temperature_service.h"
#include "clocks.h"

#include "peer_manager.h"
#include "peer_manager_handler.h"
#include "ble_conn_state.h"
#include "nrf_ble_gatt.h"
#include "nrf_ble_qwr.h"
#include "ble_hci.h"
#include "ble_advdata.h"
#include "ble_advertising.h"
#include "ble_conn_params.h"
#include "nrf_sdh_ble.h"

void nrf_qwr_error_handler(uint32_t nrf_error);
void pm_evt_handler(pm_evt_t const * p_evt);
void gap_params_init(void);
void gap_params_update(uint16_t m_conn_handle);
void gatt_init(void);
void on_temperature_evt(ble_temperature_service_t * p_cus_service, temperature_evt_t * p_evt);
void services_init(void);
void on_conn_params_evt(ble_conn_params_evt_t * p_evt);
void conn_params_error_handler(uint32_t nrf_error);
void conn_params_init(void);
void on_adv_evt(ble_adv_evt_t ble_adv_evt);
void ble_evt_handler(ble_evt_t const * p_ble_evt, void * p_context);
void ble_stack_init(void);
void peer_manager_init(void);
void advertising_init(void);
void advertising_start(void);
void update_temperature_characteristic(uint8_t* tmp116_uint8_t);
void set_hardware_version(void);
#endif /*__BLUETOOTH_H__*/