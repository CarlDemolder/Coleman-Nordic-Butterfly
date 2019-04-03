#ifndef BLE_TEMPERATURE_SERVICE_H__
#define BLE_TEMPERATURE_SERVICE_H__

#include "ble_srv_common.h"
#include "ble.h"

#include "app_error.h"
#include "ble_hci.h"

#include "common.h"

#include "ble_advdata.h"
#include "ble_advertising.h"
#include "ble_conn_params.h"
#include "nrf_sdh.h"
#include "nrf_sdh_soc.h"
#include "nrf_sdh_ble.h"
#include "fds.h"
#include "peer_manager.h"
#include "peer_manager_handler.h"
#include "bsp_btn_ble.h"
#include "sensorsim.h"
#include "ble_conn_state.h"
#include "nrf_ble_gatt.h"
#include "nrf_ble_qwr.h"
#include "nrf_pwr_mgmt.h"


// Macro for defining a ble instance
#define BLE_TEMPERATURE_SERVICE_DEF(_name) static ble_temperature_service_t _name; NRF_SDH_BLE_OBSERVER(_name ## _obs, BLE_HRS_BLE_OBSERVER_PRIO, ble_temperature_on_ble_evt, &_name)

#define CUSTOM_TEMPERATURE_SERVICE_UUID_BASE         {0xCE, 0x13, 0x10, 0xB0, 0xF0, 0x74, 0x3E, 0xA1, 0xEE, 0x45, 0x30, 0x38, 0x96, 0x6B, 0xAD, 0x8A}

// Defining the custom service 16 bit UUID
#define TEMPERATURE_SERVICE_UUID            0x8CD4
#define TEMPERATURE_CHAR_UUID               0x46AA
#define SAMPLING_INTERVAL_CHAR_UUID         0x46AB
#define HARDWARE_VERSION_CHAR_UUID          0x46AC

typedef struct ble_temperature_service_s ble_temperature_service_t;   // Forward declaration of the ble_cus_t type.

/**@brief Temperature Service event type. */
typedef enum
{
    TEMPERATURE_EVT_NOTIFICATION_ENABLED,                             /**< Custom value notification enabled event. */
    TEMPERATURE_EVT_NOTIFICATION_DISABLED,                             /**< Custom value notification disabled event. */
    TEMPERATURE_EVT_DISCONNECTED,
    TEMPERATURE_EVT_CONNECTED, 
    TEMPERATURE_EVT_WRITE,
    TEMPERATURE_EVT_READ, 
    TEMPERATURE_EVT_NOTIFICATION
} temperature_evt_type_t;

/**@brief Custom Service event for Temperature Service. */
typedef struct
{
    temperature_evt_type_t evt_type;                                  /**< Type of event. */
} temperature_evt_t;


/**@brief Temperature Service event handler type. */
typedef void (*temperature_evt_handler_t) (ble_temperature_service_t * p_bas, temperature_evt_t * p_evt);

/**@brief Custom Service structure. This contains various status information for the service. */
struct ble_temperature_service_s
{
    temperature_evt_handler_t     evt_handler; /**< Event handler to be called for handling events in the Custom Service. */
    uint16_t                      service_handle;                 /**< Handle of Custom Service (as provided by the BLE stack). */
    ble_gatts_char_handles_t      temperature_value_handles;           /**< Handles related to the Temperature Value characteristic. */
    ble_gatts_char_handles_t      sampling_interval_handles;           /**< Handles related to the Sampling Interval Value characteristic. */
    ble_gatts_char_handles_t      hardware_version_handles;           /**< Handles related to the Hardware Version Value characteristic. */
    uint16_t                      conn_handle;                    /**< Handle of the current connection (as provided by the BLE stack, is BLE_CONN_HANDLE_INVALID if not in a connection). */
    uint8_t                       uuid_type; 
};

/**@brief Custom Service init structure. This contains all options and data needed for
 *        initialization of the service.*/
typedef struct
{
    temperature_evt_handler_t         evt_handler; /**< Event handler to be called for handling events in the Custom Service. */
    uint8_t                           temperature_value[5];           /**< Initial custom value */
    uint8_t                           sampling_interval_value;          /**< Sampling interval value in seconds */  
    uint8_t                           hardware_version[5];           /**< Hardware Version Number */
    ble_srv_cccd_security_mode_t      custom_value_char_attr_md;     /**< Initial security level for Custom characteristics attribute */
} ble_temperature_service_init_t;


void ble_temperature_on_ble_evt(ble_evt_t const * p_ble_evt, void * p_context);
uint32_t temperature_custom_value_update(ble_temperature_service_t * p_cus, uint8_t * array_data);
uint32_t hardware_version_value_update(ble_temperature_service_t * p_cus, uint8_t * array_data);
uint32_t sampling_interval_value_update(ble_temperature_service_t * p_cus, uint8_t * new_sampling_interval);
uint32_t ble_temperature_service_initialize(ble_temperature_service_t * p_cus, const ble_temperature_service_init_t * p_cus_init);
uint32_t temperature_value_char_add(ble_temperature_service_t * p_cus, const ble_temperature_service_init_t * p_cus_init);
uint32_t hardware_version_char_add(ble_temperature_service_t * p_cus, const ble_temperature_service_init_t * p_cus_init);
uint32_t sampling_interval_value_char_add(ble_temperature_service_t * p_cus, const ble_temperature_service_init_t * p_cus_init);
#endif // BLE_TEMPERATURE_SERVICE_H__


