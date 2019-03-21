#include "bluetooth.h"

#define DEVICE_NAME                     "Butterfly_v1"                          /**< Name of device. Will be included in the advertising data. */
#define MANUFACTURER_NAME               "Coleman Lab"                           /**< Manufacturer. Will be passed to Device Information Service. */
#define APP_ADV_INTERVAL                1600                                     /**< The advertising interval (in units of 0.625 ms. This value corresponds to 187.5 ms). */

#define APP_ADV_DURATION                18000                                   /**< The advertising duration (180 seconds) in units of 10 milliseconds. */
#define APP_BLE_OBSERVER_PRIO           3                                       /**< Application's BLE observer priority. You shouldn't need to modify this value. */
#define APP_BLE_CONN_CFG_TAG            1                                       /**< A tag identifying the SoftDevice BLE configuration. */

#define MIN_CONN_INTERVAL               MSEC_TO_UNITS(3995, UNIT_1_25_MS)        /**< Minimum acceptable connection interval (0.1 seconds). */
#define MAX_CONN_INTERVAL               MSEC_TO_UNITS(3995, UNIT_1_25_MS)        /**< Maximum acceptable connection interval (0.2 second). */
#define SLAVE_LATENCY                   1                                        /**< Slave latency. */
#define CONN_SUP_TIMEOUT                MSEC_TO_UNITS(32000, UNIT_10_MS)         /**< Connection supervisory timeout (21 seconds). */

#define FIRST_CONN_PARAMS_UPDATE_DELAY  RTC_TIMER_TICKS(5000)                   /**< Time from initiating event (connect or start of notification) to first time sd_ble_gap_conn_param_update is called (5 seconds). */
#define NEXT_CONN_PARAMS_UPDATE_DELAY   RTC_TIMER_TICKS(30000)                  /**< Time between each call to sd_ble_gap_conn_param_update after the first call (30 seconds). */
#define MAX_CONN_PARAMS_UPDATE_COUNT    1                                       /**< Number of attempts before giving up the connection parameter negotiation. */

#define SEC_PARAM_BOND                  1                                       /**< Perform bonding. */
#define SEC_PARAM_MITM                  0                                       /**< Man In The Middle protection not required. */
#define SEC_PARAM_LESC                  0                                       /**< LE Secure Connections not enabled. */
#define SEC_PARAM_KEYPRESS              0                                       /**< Keypress notifications not enabled. */
#define SEC_PARAM_IO_CAPABILITIES       BLE_GAP_IO_CAPS_NONE                    /**< No I/O capabilities. */
#define SEC_PARAM_OOB                   0                                       /**< Out Of Band data not available. */
#define SEC_PARAM_MIN_KEY_SIZE          7                                       /**< Minimum encryption key size. */
#define SEC_PARAM_MAX_KEY_SIZE          16                                      /**< Maximum encryption key size. */


NRF_BLE_GATT_DEF(m_gatt);                                                       /**< GATT module instance. */
NRF_BLE_QWR_DEF(m_qwr);                                                         /**< Context for the Queued Write module.*/

BLE_TEMPERATURE_SERVICE_DEF(m_ble_temperature_service);                         /**< Declaring Temperature Service Structure for application */
BLE_ADVERTISING_DEF(m_advertising);                                             /**< Advertising module instance. */

ble_temperature_service_init_t    ble_temperature_init = {0};

static uint16_t m_conn_handle = BLE_CONN_HANDLE_INVALID;                        /**< Handle of the current connection. */

// YOUR_JOB: Use UUIDs for service(s) used in your application.
ble_uuid_t m_adv_uuids[] ={{TEMPERATURE_SERVICE_UUID, BLE_UUID_TYPE_VENDOR_BEGIN}};  /**< Universally unique service identifiers. */


/**@brief Function for handling Queued Write Module errors.
 *
 * @details A pointer to this function will be passed to each service which may need to inform the
 *          application about an error.
 *
 * @param[in]   nrf_error   Error code containing information about what went wrong.
 */
void nrf_qwr_error_handler(uint32_t nrf_error)
{
    APP_ERROR_HANDLER(nrf_error);
}

/**@brief Function for handling Peer Manager events.
 *
 * @param[in] p_evt  Peer Manager event.
 */
void pm_evt_handler(pm_evt_t const * p_evt)
{
    NRF_LOG_INFO("Peer Management Event Handler");

    ret_code_t err_code;

    switch (p_evt->evt_id)
    {
        case PM_EVT_BONDED_PEER_CONNECTED:
            NRF_LOG_INFO("Connected to a previously bonded device.");
            break;

        case PM_EVT_CONN_SEC_SUCCEEDED:
            NRF_LOG_INFO("Connection secured: role: %d, conn_handle: 0x%x, procedure: %d.",
                         ble_conn_state_role(p_evt->conn_handle),
                         p_evt->conn_handle,
                         p_evt->params.conn_sec_succeeded.procedure);
            NRF_LOG_INFO("PM_EVT_CONN_SEC_SUCCEEDED");
            break;

        case PM_EVT_CONN_SEC_FAILED:
            /* Often, when securing fails, it shouldn't be restarted, for security reasons.
             * Other times, it can be restarted directly.
             * Sometimes it can be restarted, but only after changing some Security Parameters.
             * Sometimes, it cannot be restarted until the link is disconnected and reconnected.
             * Sometimes it is impossible, to secure the link, or the peer device does not support it.
             * How to handle this error is highly application dependent. */
            NRF_LOG_INFO("PM_EVT_CONN_SEC_FAILED");
            break;

        case PM_EVT_CONN_SEC_CONFIG_REQ:
            // Reject pairing request from an already bonded peer.
//            pm_conn_sec_config_t conn_sec_config = {.allow_repairing = false};
//            pm_conn_sec_config_reply(p_evt->conn_handle, &conn_sec_config);
            NRF_LOG_INFO("PM_EVT_CONN_SEC_CONFIG_REQ");
            break;

        case PM_EVT_STORAGE_FULL:
            // Run garbage collection on the flash.
            NRF_LOG_INFO("PM_EVT_STORAGE_FULL");
            err_code = fds_gc();
            if (err_code == FDS_ERR_NO_SPACE_IN_QUEUES)
            {
                // Retry.
            }
            else
            {
                APP_ERROR_CHECK(err_code);
            }
            break;

        case PM_EVT_PEERS_DELETE_SUCCEEDED:
            NRF_LOG_INFO("PM_EVT_PEERS_DELETE_SUCCEEDED");
            advertising_start();
            break;
        case PM_EVT_PEER_DATA_UPDATE_FAILED:
            NRF_LOG_INFO("PM_EVT_PEER_DATA_UPDATE_FAILED");
            APP_ERROR_CHECK(p_evt->params.peer_data_update_failed.error);
            break;
        case PM_EVT_PEER_DELETE_FAILED:
            NRF_LOG_INFO("PM_EVT_PEER_DELETE_FAILED");
            APP_ERROR_CHECK(p_evt->params.peer_delete_failed.error);
            break;
        case PM_EVT_ERROR_UNEXPECTED:
            NRF_LOG_INFO("PM_EVT_PEER_UNEXPECTED");
            APP_ERROR_CHECK(p_evt->params.error_unexpected.error);
            break;
        case PM_EVT_CONN_SEC_START:
            NRF_LOG_INFO("PM_EVT_CONN_SEC_START");
            break;
        case PM_EVT_PEER_DATA_UPDATE_SUCCEEDED:
            NRF_LOG_INFO("PM_EVT_PEER_DATA_UPDATE_SUCCEEDED");
            break;
        case PM_EVT_PEER_DELETE_SUCCEEDED:
            NRF_LOG_INFO("PM_EVT_PEER_DELETE_SUCCEEDED");
            break;
        case PM_EVT_LOCAL_DB_CACHE_APPLIED:
            NRF_LOG_INFO("PM_EVT_LOCAL_DB_CACHE_APPLIED");
            break;
        case PM_EVT_LOCAL_DB_CACHE_APPLY_FAILED:
            // This can happen when the local DB has changed.
            NRF_LOG_INFO("PM_EVT_LOCAL_DB_CACHE_APPLY_FAILED");
            break;
        case PM_EVT_SERVICE_CHANGED_IND_SENT:
            NRF_LOG_INFO("PM_EVT_SERVICE_CHANGED_IND_SENT");
            break;
        case PM_EVT_SERVICE_CHANGED_IND_CONFIRMED:
            NRF_LOG_INFO("PM_EVT_SERVICE_CHANGED_IND_CONFIRMED");
            break;
        default:
          break;
    }
}

/**@brief Function for the GAP initialization.
 *
 * @details This function sets up all the necessary GAP (Generic Access Profile) parameters of the
 *          device including the device name, appearance, and the preferred connection parameters.
 */
void gap_params_init(void)
{
    NRF_LOG_DEBUG("Gap Parameters Initialized");
    ret_code_t              err_code;
    ble_gap_conn_params_t   gap_conn_params;
    ble_gap_conn_sec_mode_t sec_mode;

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&sec_mode);

    err_code = sd_ble_gap_device_name_set(&sec_mode, (const uint8_t *)DEVICE_NAME, strlen(DEVICE_NAME));
    APP_ERROR_CHECK(err_code);

    /* YOUR_JOB: Use an appearance value matching the application's use case.
       err_code = sd_ble_gap_appearance_set(BLE_APPEARANCE_);
       APP_ERROR_CHECK(err_code); */

    memset(&gap_conn_params, 0, sizeof(gap_conn_params));

    gap_conn_params.min_conn_interval = MIN_CONN_INTERVAL;
    gap_conn_params.max_conn_interval = MAX_CONN_INTERVAL;
    gap_conn_params.slave_latency     = SLAVE_LATENCY;
    gap_conn_params.conn_sup_timeout  = CONN_SUP_TIMEOUT;

    err_code = sd_ble_gap_ppcp_set(&gap_conn_params);
    APP_ERROR_CHECK(err_code);
    if(err_code == NRF_SUCCESS)
    {
        NRF_LOG_INFO("Procedure request succeeded. Connection parameters will be negotiated as requested.");
    }
    else
    {
        NRF_LOG_INFO("Procedure request failed: %d", err_code);
    }
}

void gap_params_update(uint16_t m_conn_handle)
{
    NRF_LOG_DEBUG("Gap Parameters Initialized");
    ret_code_t              err_code;
    ble_gap_conn_params_t   gap_conn_params;

    memset(&gap_conn_params, 0, sizeof(gap_conn_params));

    gap_conn_params.min_conn_interval = MIN_CONN_INTERVAL;
    gap_conn_params.max_conn_interval = MAX_CONN_INTERVAL;
    gap_conn_params.slave_latency     = SLAVE_LATENCY;
    gap_conn_params.conn_sup_timeout  = CONN_SUP_TIMEOUT;

    err_code = ble_conn_params_change_conn_params(m_conn_handle, &gap_conn_params);
    APP_ERROR_CHECK(err_code);
    if(err_code == NRF_SUCCESS)
    {
        NRF_LOG_INFO("GAP connection parameters updated.");
    }
    else
    {
        NRF_LOG_INFO("Procedure request failed: %d", err_code);
    }
}
/**@brief Function for initializing the GATT module.
 */
void gatt_init(void)
{
    NRF_LOG_DEBUG("GATT Initialized");
    ret_code_t err_code = nrf_ble_gatt_init(&m_gatt, NULL);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for handling the Custom Service Service events.
 *
 * @details This function will be called for all Custom Service events which are passed to
 *          the application.
 *
 * @param[in]   p_cus_service  Custom Service structure.
 * @param[in]   p_evt          Event received from the Custom Service.
 *
 */
void on_temperature_evt(ble_temperature_service_t * p_cus_service, temperature_evt_t * p_evt)
{
    ret_code_t err_code;
    NRF_LOG_DEBUG("ON_TEMPERATURE_EVENT Occurred");
    switch(p_evt->evt_type)
    {
        case TEMPERATURE_EVT_NOTIFICATION_ENABLED:
            NRF_LOG_DEBUG("ON_TEMPERATURE_EVENT NOTIFICATION ENABLED");
            gap_params_update(m_conn_handle);
            rtc_start();
            break;

        case TEMPERATURE_EVT_NOTIFICATION_DISABLED:
            NRF_LOG_DEBUG("ON_TEMPERATURE_EVENT NOTIFICATION DISABLED");
            rtc_stop();
            kill_nrf52();
            break;

        case TEMPERATURE_EVT_CONNECTED:
            NRF_LOG_DEBUG("ON_TEMPERATURE_EVENT SERVICE CONNECTED");
            rtc_restart();    // Restart the RTC Timer if the phone and sensor are disconnected
            break;

        case TEMPERATURE_EVT_DISCONNECTED:
            NRF_LOG_DEBUG("ON_TEMPERATURE_EVENT SERVICE DISCONNECTED");
            rtc_stop();
            break;
        
        case TEMPERATURE_EVT_WRITE:
            NRF_LOG_DEBUG("ON_TEMPERATURE_EVENT CHARACTERISTIC WRITTEN");
            sampling_interval_value_update(p_cus_service, &ble_temperature_init.sampling_interval_value);
            rtc_stop();
            rtc_set_counter(ble_temperature_init.sampling_interval_value);

        default:
              break;
    }
}

/**@brief Function for initializing services that will be used by the application.
 */
void services_init(void)
{
    NRF_LOG_DEBUG("Service Initialized");
    ret_code_t            err_code;
    nrf_ble_qwr_init_t qwr_init = {0};
    
    // Initialize Queued Write Module.
    qwr_init.error_handler = nrf_qwr_error_handler;

    err_code = nrf_ble_qwr_init(&m_qwr, &qwr_init);
    APP_ERROR_CHECK(err_code);

    // Initialize Temperature Service
    ble_temperature_init.evt_handler = on_temperature_evt;
//    ble_temperature_init.sampling_interval_value = 0x38;    // Setting the initial Sampling interval to 8 seconds
//    uint8_t init_temp_value[5] = {0};
//    memcpy(ble_temperature_init.temperature_value, init_temp_value, 5);

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&ble_temperature_init.custom_value_char_attr_md.cccd_write_perm);
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&ble_temperature_init.custom_value_char_attr_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&ble_temperature_init.custom_value_char_attr_md.write_perm);

    err_code = ble_temperature_service_initialize(&m_ble_temperature_service, &ble_temperature_init);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for handling the Connection Parameters Module.
 *
 * @details This function will be called for all events in the Connection Parameters Module which
 *          are passed to the application.
 *          @note All this function does is to disconnect. This could have been done by simply
 *                setting the disconnect_on_fail config parameter, but instead we use the event
 *                handler mechanism to demonstrate its use.
 *
 * @param[in] p_evt  Event received from the Connection Parameters Module.
 */
void on_conn_params_evt(ble_conn_params_evt_t * p_evt)
{
    NRF_LOG_DEBUG("On Connection Parameters Events");
    ret_code_t err_code;

    if (p_evt->evt_type == BLE_CONN_PARAMS_EVT_FAILED)
    {
        NRF_LOG_DEBUG("On Connection Parameters Failed");
        err_code = sd_ble_gap_disconnect(m_conn_handle, BLE_HCI_CONN_INTERVAL_UNACCEPTABLE);
        APP_ERROR_CHECK(err_code);
    }
}

/**@brief Function for handling a Connection Parameters error.
 *
 * @param[in] nrf_error  Error code containing information about what went wrong.
 */
void conn_params_error_handler(uint32_t nrf_error)
{
    APP_ERROR_HANDLER(nrf_error);
}

/**@brief Function for initializing the Connection Parameters module.
 */
void conn_params_init(void)
{
    NRF_LOG_DEBUG("Connection Parameters Initialized");
    ret_code_t             err_code;
    ble_conn_params_init_t cp_init;

    memset(&cp_init, 0, sizeof(cp_init));

    cp_init.p_conn_params                  = NULL;
    cp_init.first_conn_params_update_delay = FIRST_CONN_PARAMS_UPDATE_DELAY;
    cp_init.next_conn_params_update_delay  = NEXT_CONN_PARAMS_UPDATE_DELAY;
    cp_init.max_conn_params_update_count   = MAX_CONN_PARAMS_UPDATE_COUNT;
    cp_init.start_on_notify_cccd_handle    = BLE_GATT_HANDLE_INVALID;
    cp_init.disconnect_on_fail             = false;
    cp_init.evt_handler                    = on_conn_params_evt;
    cp_init.error_handler                  = conn_params_error_handler;

    err_code = ble_conn_params_init(&cp_init);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for handling advertising events.
 *
 * @details This function will be called for advertising events which are passed to the application.
 *
 * @param[in] ble_adv_evt  Advertising event.
 */
void on_adv_evt(ble_adv_evt_t ble_adv_evt)
{
    NRF_LOG_DEBUG("On Advertising Event");
    ret_code_t err_code;

    switch (ble_adv_evt)
    {
        case BLE_ADV_EVT_FAST:
            NRF_LOG_INFO("Fast advertising.");
            break;

        case BLE_ADV_EVT_IDLE:
            NRF_LOG_INFO("BLE advertising idle.");
//            kill_nrf52();   // Disabling the LDO to kill the MCU
            break;

        default:
            break;
    }
}

/**@brief Function for handling BLE events.
 *
 * @param[in]   p_ble_evt   Bluetooth stack event.
 * @param[in]   p_context   Unused.
 */
void ble_evt_handler(ble_evt_t const * p_ble_evt, void * p_context)
{
    ret_code_t err_code = NRF_SUCCESS;

    switch (p_ble_evt->header.evt_id)
    {
        case BLE_GAP_EVT_DISCONNECTED:
            NRF_LOG_INFO("BLE_EVT_Disconnected.");
            break;

        case BLE_GAP_EVT_CONNECTED:
            NRF_LOG_INFO("BLE_EVT_Connected.");
            m_conn_handle = p_ble_evt->evt.gap_evt.conn_handle;
            err_code = nrf_ble_qwr_conn_handle_assign(&m_qwr, m_conn_handle);
            APP_ERROR_CHECK(err_code);
            break;

        case BLE_GAP_EVT_PHY_UPDATE_REQUEST:
            NRF_LOG_DEBUG("BLE_EVT_PHY update request.");
            ble_gap_phys_t const phys = {.rx_phys = BLE_GAP_PHY_AUTO, .tx_phys = BLE_GAP_PHY_AUTO};
            err_code = sd_ble_gap_phy_update(p_ble_evt->evt.gap_evt.conn_handle, &phys);
            APP_ERROR_CHECK(err_code);
            break;

        case BLE_GATTC_EVT_TIMEOUT:
            // Disconnect on GATT Client timeout event.
            NRF_LOG_DEBUG("BLE_EVT_GATT Client Timeout.");
            err_code = sd_ble_gap_disconnect(p_ble_evt->evt.gattc_evt.conn_handle, BLE_HCI_REMOTE_USER_TERMINATED_CONNECTION);
            APP_ERROR_CHECK(err_code);
            break;

        case BLE_GATTS_EVT_TIMEOUT:
            // Disconnect on GATT Server timeout event.
            NRF_LOG_DEBUG("BLE_EVT_GATT Server Timeout.");
            err_code = sd_ble_gap_disconnect(p_ble_evt->evt.gatts_evt.conn_handle, BLE_HCI_REMOTE_USER_TERMINATED_CONNECTION);
            APP_ERROR_CHECK(err_code);
            break;

        default:
            // No implementation needed.
            break;
    }
}

/**@brief Function for initializing the BLE stack.
 *
 * @details Initializes the SoftDevice and the BLE event interrupt.
 */
void ble_stack_init(void)
{
    NRF_LOG_DEBUG("Initializing BLE Stack");
    ret_code_t err_code;

    err_code = nrf_sdh_enable_request();
    APP_ERROR_CHECK(err_code);

    // Configure the BLE stack using the default settings.
    // Fetch the start address of the application RAM.
    uint32_t ram_start = 0;
    err_code = nrf_sdh_ble_default_cfg_set(APP_BLE_CONN_CFG_TAG, &ram_start);
    APP_ERROR_CHECK(err_code);

    // Enable BLE stack.
    err_code = nrf_sdh_ble_enable(&ram_start);
    APP_ERROR_CHECK(err_code);

    // Register a handler for BLE events.
    NRF_SDH_BLE_OBSERVER(m_ble_observer, APP_BLE_OBSERVER_PRIO, ble_evt_handler, NULL);
}

/**@brief Function for the Peer Manager initialization.
 */
void peer_manager_init(void)
{
    NRF_LOG_DEBUG("Peer Manager Initialized");
    ble_gap_sec_params_t sec_param;
    ret_code_t           err_code;

    err_code = pm_init();
    APP_ERROR_CHECK(err_code);

    memset(&sec_param, 0, sizeof(ble_gap_sec_params_t));

    // Security parameters to be used for all security procedures.
    sec_param.bond           = SEC_PARAM_BOND;
    sec_param.mitm           = SEC_PARAM_MITM;
    sec_param.lesc           = SEC_PARAM_LESC;
    sec_param.keypress       = SEC_PARAM_KEYPRESS;
    sec_param.io_caps        = SEC_PARAM_IO_CAPABILITIES;
    sec_param.oob            = SEC_PARAM_OOB;
    sec_param.min_key_size   = SEC_PARAM_MIN_KEY_SIZE;
    sec_param.max_key_size   = SEC_PARAM_MAX_KEY_SIZE;
    sec_param.kdist_own.enc  = 1;
    sec_param.kdist_own.id   = 1;
    sec_param.kdist_peer.enc = 1;
    sec_param.kdist_peer.id  = 1;

    err_code = pm_sec_params_set(&sec_param);
    APP_ERROR_CHECK(err_code);

    err_code = pm_register(pm_evt_handler);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for initializing the Advertising functionality.
 */
void advertising_init(void)
{
    NRF_LOG_DEBUG("Advertising Initialized");
    ret_code_t             err_code;
    ble_advertising_init_t adv_init;

    memset(&adv_init, 0, sizeof(adv_init));

    adv_init.advdata.name_type               = BLE_ADVDATA_FULL_NAME;
    adv_init.advdata.include_appearance      = true;
    adv_init.advdata.flags                   = BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE;
//    adv_init.advdata.uuids_complete.uuid_cnt = sizeof(m_adv_uuids) / sizeof(m_adv_uuids[0]);
//    adv_init.advdata.uuids_complete.p_uuids  = m_adv_uuids;

    adv_init.config.ble_adv_fast_enabled  = true;
    adv_init.config.ble_adv_fast_interval = APP_ADV_INTERVAL;
    adv_init.config.ble_adv_fast_timeout  = APP_ADV_DURATION;

    adv_init.evt_handler = on_adv_evt;

    err_code = ble_advertising_init(&m_advertising, &adv_init);
    APP_ERROR_CHECK(err_code);

    ble_advertising_conn_cfg_tag_set(&m_advertising, APP_BLE_CONN_CFG_TAG);
}

/**@brief Function for starting advertising.
 */
void advertising_start(void)
{
    ret_code_t err_code = ble_advertising_start(&m_advertising, BLE_ADV_MODE_FAST);
    APP_ERROR_CHECK(err_code);
}

void update_temperature_characteristic(uint8_t* tmp116_uint8_t)
{
    uint32_t err_code;
    memcpy(ble_temperature_init.temperature_value, tmp116_uint8_t, 5);
    err_code = temperature_custom_value_update(&m_ble_temperature_service, tmp116_uint8_t);   // Update the TMP116 Characteristic Value
    APP_ERROR_CHECK(err_code);
}