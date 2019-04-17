package com.example.intern.ble_ex1;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service
{
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    public static final String CONNECTED_GATT = "CONNECTED_GATT";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    // Bluetooth Services that need to be read/write from
    private static BluetoothGattService mTempService;       //Temperature Service

    // Bluetooth characteristics that we need to read/write
    private static BluetoothGattCharacteristic mTemperatureCharacteristic;  //Temperature Characteristic
    private static BluetoothGattCharacteristic mSamplingRateCharacteristic;     // Sampling Rate Characteristic
    private static BluetoothGattCharacteristic mHardwareVersionCharacteristic;     // Hardware Version Characteristic

    // Bluetooth Custom Client Characteristic Configuration Descriptors
    private static BluetoothGattDescriptor mTemperatureCccd; //Temperature Custom Client Characteristic Configuration Descriptor

    public final static UUID UUID_SERVICE_BODY_TEMPERATURE = UUID.fromString(GattAttributes.SERVICE_BODY_TEMPERATURE);
    public final static UUID UUID_TEMPERATURE = UUID.fromString(GattAttributes.TEMPERATURE);
    public final static UUID UUID_TEMPERATURE_CCC = UUID.fromString(GattAttributes.TEMPERATURE_CCC);
    public final static UUID UUID_SAMPLING_RATE = UUID.fromString(GattAttributes.SAMPLING_RATE);
    public final static UUID UUID_HARDWARE_VERSION = UUID.fromString(GattAttributes.HARDWARE_VERSION);

    private static String mTemperatureValue = "0";     //Temperature Notifications
    private static String mHardwareVersionValue = "v0X.0";      // Hardware Version String Initialization

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static final int MODE_TEMPERATURE = 1;
    private static final int MODE_HARDWARE_VERSION = 2;

    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();
    private Queue<BluetoothGatt> gattWriteQueue = new LinkedList<>();

    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<>();
    private Queue<BluetoothGattCharacteristic> characteristicWriteQueue = new LinkedList<>();

    private Queue<Boolean> notificationWriteQueue = new LinkedList<Boolean>();

    private Queue<BluetoothGatt> gattReadQueue = new LinkedList<>();
    private Boolean isWriting = false;
    private Boolean isReading = false;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.


    public void linkUUID(BluetoothGatt gatt)
    {
        // Linking UUID to Services
        mTempService = gatt.getService(UUID_SERVICE_BODY_TEMPERATURE);      //Body Temperature Service assigned to specific UUID

        // Linking UUID to Characteristics
        mTemperatureCharacteristic = mTempService.getCharacteristic(UUID_TEMPERATURE);                          // Body Temperature Characteristics
        mSamplingRateCharacteristic = mTempService.getCharacteristic(UUID_SAMPLING_RATE);                       // Sampling Rate Characteristics
        mHardwareVersionCharacteristic = mTempService.getCharacteristic(UUID_HARDWARE_VERSION);                 // Hardware Version Characteristics

        // Linking UUID to Custom Client Configuration Descriptors
        mTemperatureCccd = mTemperatureCharacteristic.getDescriptor(UUID_TEMPERATURE_CCC);          //Body Temperature Descriptor
    }

    public String getmTemperatureValue()
    {
        return mTemperatureValue;
    }

    public String getmHardwareVersionValue()
    {
        return mHardwareVersionValue;
    }

    public void broadcastUpdate(final String action, final String ble_address)
    {
        final Intent intent = new Intent(action);
        intent.putExtra(CONNECTED_GATT, ble_address);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder
    {
        BluetoothLeService getService()
        {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize()
    {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null)
        {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null)
            {
                Log.d(TAG, "UV: Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null)
        {
            Log.d(TAG, "UV: Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */

    public BluetoothDevice createBleDevice(final String address)
    {
        if (mBluetoothAdapter == null || address == null)
        {
            Log.d(TAG, "UV: BluetoothAdapter not initialized or unspecified address.");
            return null;
        }
        BluetoothDevice mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        Log.d(TAG, String.format("UV: createBleDevice %s", mBluetoothDevice.getAddress()));
        if(mBluetoothDevice == null)
        {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return null;
        }
        return mBluetoothDevice;
    }

    public BluetoothGatt createBleGatt(BluetoothDevice mBluetoothDevice, BluetoothGattCallback mGattCallback)
    {
        if(mBluetoothDevice == null)
        {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return null;
        }
        // We want to directly connect to the device, so we are setting the autoConnect parameter to false
        BluetoothGatt mBluetoothGatt = mBluetoothDevice.connectGatt(this, false, mGattCallback);
        refreshDeviceCache(mBluetoothGatt);
        Log.d(TAG, String.format("UV: createBleGatt %s", mBluetoothGatt.getDevice().getAddress()));
        return mBluetoothGatt;
    }

    // Previously connected device.  Try to reconnect.
    public boolean reconnectBleDevice(BluetoothGatt mBluetoothGatt, String mBluetoothDeviceAddress)
    {
        if (mBluetoothDeviceAddress != null && mBluetoothGatt != null)
        {
            Log.d(TAG, "UV: reconnectBleDevice");

            boolean reconnectGatt = mBluetoothGatt.connect();
            Log.d(TAG, "UV: reconnectGatt: " + reconnectGatt);

            Log.d(TAG, "UV: Able to reconnect");
            return true;
        }
        return false;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean disconnect(BluetoothGatt mBluetoothGatt)
    {
        Log.d(TAG, "UV: disconnect mBluetoothGatt");
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.d(TAG, "UV: BluetoothAdapter not initialized");
            return false;
        }
        mBluetoothGatt.disconnect();
        return true;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public BluetoothGatt close(BluetoothGatt mBluetoothGatt)
    {
        if (mBluetoothGatt == null)
        {
            return null;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        return  mBluetoothGatt;
    }

    public void writeCharacteristicSamplingRate(BluetoothGatt mBluetoothGatt, int sampling_rate)
    {
        Log.d(TAG, "UV: writeCharacteristicNotification");
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.d(TAG, "UV: BluetoothAdapter not initialized");
            return;
        }
        byte[] samplingRate_byteVal = new byte[1];
        samplingRate_byteVal[0] = (byte) sampling_rate;
        mSamplingRateCharacteristic.setValue(samplingRate_byteVal);
        boolean characteristic_status = writeGattCharacteristic(mBluetoothGatt, mSamplingRateCharacteristic);
        Log.d(TAG, String.format("UV: Sampling Rate Characteristic: %b", characteristic_status));
    }

    public boolean writeGattCharacteristic(BluetoothGatt mBluetoothGatt, BluetoothGattCharacteristic characteristic)
    {
        Log.d(TAG, "UV: writeGattCharacteristic");
        characteristicWriteQueue.add(characteristic);   // Adding Characteristic to the Write Queue
        gattWriteQueue.add(mBluetoothGatt);     // Adding Bluetooth Gatt to  Write Queue
        return writeNextCharacteristicFromQueue();
    }

    public void writeGattCharacteristicNotification(BluetoothGatt mBluetoothGatt, boolean temp_Notification)
    {
        Log.d(TAG, "UV: writeCharacteristicNotification");
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.d(TAG, "UV: BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(mTemperatureCharacteristic, temp_Notification);   //Enabling or Disabling Temperature Characteristic Notification
    }

    private boolean writeNextCharacteristicFromQueue()
    {
        if(isWriting)
        {
            Log.d(TAG, "UV: isWriting");
            return false;
        }
        if(characteristicWriteQueue.size() == 0)
        {
            return false;
        }
        isWriting = true;
        Log.d(TAG, String.format("UV: WriteNextCharacteristicFromQueue: %d", characteristicWriteQueue.size()));
        boolean characteristic_status = gattWriteQueue.element().writeCharacteristic(characteristicWriteQueue.element());
        if(!characteristic_status)
        {
            Log.d(TAG, "UV: Characteristic Failed");
        }
        else
        {
            Log.d(TAG, "UV: Characteristic Success!!");
        }
        return characteristic_status;
    }

    public boolean onCharacteristicWrite(int status)
    {
        isWriting = false;
        gattWriteQueue.remove();        // Remove from the Queue the Gatt that was just written to
        characteristicWriteQueue.remove();  // Remove from the Queue the Characteristic that was just written
        writeNextCharacteristicFromQueue();
        if (status == BluetoothGatt.GATT_SUCCESS)
        {
            Log.d(TAG, "UV: Callback: Wrote GATT Characteristic successfully.");
        }
        else
        {
            Log.d(TAG, "UV: Callback: Error writing GATT Characteristic: "+ status);
        }
        if(characteristicWriteQueue.size() == 0)
        {
            return true;
        }
        return false;
    }

    public void readCharacteristicHardwareVersion(BluetoothGatt mBluetoothGatt)
    {
        Log.d(TAG, "UV: readCharacteristicHardwareVersion");
        boolean characteristic_status = readGattCharacteristic(mBluetoothGatt, mHardwareVersionCharacteristic);
        Log.d(TAG, String.format("UV: Hardware Version Characteristic: %b", characteristic_status));
    }

    public boolean readGattCharacteristic(BluetoothGatt mBluetoothGatt, BluetoothGattCharacteristic characteristic)
    {
        Log.d(TAG, "UV: readGattCharacteristic");
        characteristicReadQueue.add(characteristic);   // Adding Characteristic to the Write Queue
        gattReadQueue.add(mBluetoothGatt);     // Adding Bluetooth Gatt to  Write Queue
        return readNextCharacteristicFromQueue();
    }

    private boolean readNextCharacteristicFromQueue()
    {
        if(isReading)
        {
            Log.d(TAG, "UV: isReading");
            return false;
        }
        if(characteristicReadQueue.size() == 0)
        {
            return false;
        }
        isReading = true;
        Log.d(TAG, String.format("UV: ReadNextCharacteristicFromQueue: %d", characteristicReadQueue.size()));
        boolean characteristic_status = gattReadQueue.element().readCharacteristic(characteristicReadQueue.element());
        if(!characteristic_status)
        {
            Log.d(TAG, "UV: Characteristic Read Failed");
        }
        else
        {
            Log.d(TAG, "UV: Characteristic Read Success!!");
        }
        return characteristic_status;
    }

    public boolean onCharacteristicRead(int status)
    {
        isReading = false;
        gattReadQueue.remove();        // Remove from the Queue the Gatt that was just read from
        characteristicReadQueue.remove();  // Remove from the Queue the Characteristic that was just read from
        readNextCharacteristicFromQueue();
        if (status == BluetoothGatt.GATT_SUCCESS)
        {
            Log.d(TAG, "UV: Callback: Read GATT Characteristic successfully.");
        }
        else
        {
            Log.d(TAG, "UV: Callback: Error reading GATT Characteristic: "+ status);
        }
        if(characteristicReadQueue.size() == 0)
        {
            return true;
        }
        return false;
    }

    public int onCharacteristicChanged(BluetoothGattCharacteristic characteristic)
    {
        //This is called when a characteristic notification changes
        //It broadcasts to the main activity with the changed data
        if(UUID_TEMPERATURE.equals(characteristic.getUuid()))
        {
            mTemperatureValue = characteristic.getStringValue(0);
            Log.d(TAG, String.format("UV: Temperature Value: %s", mTemperatureValue));
            return MODE_TEMPERATURE;
        }

        if(UUID_HARDWARE_VERSION.equals(characteristic.getUuid()))
        {
            mHardwareVersionValue = characteristic.getStringValue(0);
            Log.d(TAG, String.format("UV: HardwareVersionValue: %s", mHardwareVersionValue));
            return MODE_HARDWARE_VERSION;
        }
        return 0;
    }

    public void writeGattDescriptor(BluetoothGatt mBluetoothGatt, BluetoothGattDescriptor descriptor)
    {
        Log.d(TAG, "UV: writeGattDescriptor");
        descriptorWriteQueue.add(descriptor);   // Adding Descriptor to the Write Queue
        gattWriteQueue.add(mBluetoothGatt);     // Adding Bluetooth Gatt to  Write Queue
        writeNextDescriptorFromQueue();
    }

    private void writeNextDescriptorFromQueue()
    {
        if(isWriting)
        {
            Log.d(TAG, "UV: isWriting");
            return;
        }
        if(descriptorWriteQueue.size() == 0)
        {
            return;
        }
        isWriting = true;
        Log.d(TAG, String.format("UV: WriteNextValueFromQueue: %d", descriptorWriteQueue.size()));

        Boolean DescriptorBoolean = gattWriteQueue.element().writeDescriptor(descriptorWriteQueue.element());
        if(!DescriptorBoolean)
        {
            Log.d(TAG, "UV: Descriptor Failed");
        }
        else
        {
            Log.d(TAG, "UV: Descriptor Success!!");
        }
    }

    public void writeDescriptorNotification(BluetoothGatt mBluetoothGatt, boolean temp_Notification)
    {
        Log.d(TAG, "UV: writeDescriptorNotification " + temp_Notification);
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.d(TAG, "UV: BluetoothAdapter not initialized");
            return;
        }

        byte[] temp_byteVal = new byte[2];
        if(temp_Notification)
        {
            temp_byteVal[0] = 1;
        }
        else
        {
            temp_byteVal[0] = 0;
        }
        temp_byteVal[1] = 0;
        Log.d(TAG, "UV: Temperature Notification: " + temp_byteVal[0]);
        mTemperatureCccd.setValue(temp_byteVal);
        writeGattDescriptor(mBluetoothGatt, mTemperatureCccd);
    }

    public boolean onDescriptorWrite(int status)
    {
        isWriting = false;
        gattWriteQueue.remove();        // Remove from the Queue the Gatt that was just written to
        descriptorWriteQueue.remove();  // Remove from the Queue the Descriptor that was just written
        writeNextDescriptorFromQueue();

        if (status == BluetoothGatt.GATT_SUCCESS)
        {
            Log.d(TAG, "UV: Callback: Wrote GATT Descriptor successfully.");
        }
        else
        {
            Log.d(TAG, "UV: Callback: Error writing GATT Descriptor: "+ status);
        }
        if(descriptorWriteQueue.size() == 0)
        {
            return true;
        }
        return false;
    }

    private void refreshDeviceCache(BluetoothGatt localBluetoothGatt)
    {
        try {
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh");
            if(localMethod != null)
            {
                localMethod.invoke(localBluetoothGatt);
            }
        } catch(Exception localException)
        {
            Log.d(TAG, ("Exception refreshing BT cache: "+ localException.toString()));
        }
    }

}
