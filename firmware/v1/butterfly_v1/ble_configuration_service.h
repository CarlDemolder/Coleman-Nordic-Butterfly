#ifndef BLE_CONFIGURATION_SERVICE_H__
#define BLE_CONFIGURATION_SERVICE_H__

#include "ble_srv_common.h"
#include "ble.h"

#include "common.h"

// Macro for defining a ble instance
#define BLE_CONFIGURATION_SERVICE_DEF(_configuration_name) static ble_configuration_service_t _configuration_name;

#define CUSTOM_CONFIGURATION_SERVICE_UUID_BASE         {0xCE, 0x13, 0x10, 0xB0, 0xF0, 0x74, 0x3E, 0xA1, 0xEE, 0x45, 0x30, 0x38, 0x96, 0x6B, 0xAD, 0x8A}

// Defining the custom service 16 bit UUID
#define CONFIGURATION_SERVICE_UUID     0x04D0

// Forward declaration of the ble_cus_t type.
typedef struct ble_configuration_service_s ble_configuration_service_t;

/**@brief Custom Service init structure. This contains all options and data needed for
 *        initialization of the service.*/
typedef struct
{
    uint8_t                       initial_custom_value;           /**< Initial custom value */
    ble_srv_cccd_security_mode_t  custom_value_char_attr_md;     /**< Initial security level for Custom characteristics attribute */
} ble_configuration_service_init_t;

/**@brief Custom Service structure. This contains various status information for the service. */
struct ble_configuration_service_s
{
    uint16_t                      service_handle;                 /**< Handle of Custom Service (as provided by the BLE stack). */
    ble_gatts_char_handles_t      custom_value_handles;           /**< Handles related to the Custom Value characteristic. */
    uint16_t                      conn_handle;                    /**< Handle of the current connection (as provided by the BLE stack, is BLE_CONN_HANDLE_INVALID if not in a connection). */
    uint8_t                       uuid_type; 
};

uint32_t ble_configuration_service_initialize(ble_configuration_service_t * p_cus, const ble_configuration_service_init_t * p_cus_init);

#endif // BLE_CONFIGURATION_H


