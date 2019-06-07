package com.example.intern.ble_ex1;

import android.bluetooth.*;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class BleDevice implements Parcelable
{
    private final static String TAG = BleDevice.class.getSimpleName();

    private BluetoothLeService mBluetoothLeService;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;

    private boolean mGattServicesDiscovered;
    private boolean mPreviouslyConnected;
    private boolean writingDescriptor;

    private String connectionState;

    private String bleAddress;
    private String bleName;

    private String hardwareVersion;

    private boolean temp_Notification;

    private boolean temp_Recording;

    private int samplingRate;

    private int recordingDuration;

    private String patientName;

    private double counter;

    private int maxDataSet;

    private ArrayList<Double> temp_data_array;
    private ArrayList<String> time_array;
    private ArrayList<Double> counter_array;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private int mConnectionState = STATE_DISCONNECTED;

    private int mDataMode;      // data_mode is used to determine which variable is changing state at a current set point
    private static final int MODE_TEMPERATURE = 1;
    private static final int MODE_HARDWARE_VERSION = 2;

    @Override
    public int describeContents()
    {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel parcelDestination, int flags)
    {
        parcelDestination.writeString(bleAddress);      // Place BLE Address into Parcel
        parcelDestination.writeString(bleName);         // Place BLE Name into Parcel
        parcelDestination.writeString(connectionState);     // Place BLE Connection State into Parcel
        parcelDestination.writeString(hardwareVersion);     // Place Hardware Version into Parcel
        parcelDestination.writeString(patientName);         // Place Patient Name into Parcel
        parcelDestination.writeByte((byte) (temp_Notification ? 1 : 0));        // Place Temperature Notification into Parcel
        parcelDestination.writeByte((byte) (temp_Recording ? 1 : 0));        // Place Temperature Recording Flag into Parcel
        parcelDestination.writeInt(samplingRate);               // Place Sampling Rate Int into Parcel
        parcelDestination.writeInt(recordingDuration);               // Place Recording Duration Int into Parcel
        parcelDestination.writeDouble(counter);         // Place Counter into Parcel
        parcelDestination.writeInt(maxDataSet);         // Place maxDataSet into Parcel
        parcelDestination.writeList(temp_data_array);   // Place Temperature Data ArrayList into Parcel
        parcelDestination.writeList(time_array);        // Place Time ArrayList into Parcel
        parcelDestination.writeList(counter_array);        // Place Counter ArrayList into Parcel
    }

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private BleDevice(Parcel parcel)
    {
        bleAddress = parcel.readString();                   // Get the BLE Address from the Parcel
        bleName = parcel.readString();                      // Get the BLE Name from the Parcel
        connectionState = parcel.readString();              // Get the BLE Connection State into Parcel
        hardwareVersion = parcel.readString();              // Get the Hardware Version into Parcel
        patientName = parcel.readString();                  // Get the Patient Name into Parcel
        temp_Notification = parcel.readByte() != 0;         // Get the Temperature Notification from the Parcel
        temp_Recording = parcel.readByte() != 0;            // Get the Temperature Recording Flag from the Parcel
        samplingRate = parcel.readInt();                    // Get the Sampling Rate from the Parcel
        recordingDuration = parcel.readInt();                    // Get the Recording Duration from the Parcel
        counter = parcel.readDouble();                      // Get the Counter from the Parcel
        maxDataSet = parcel.readInt();                      // Get the maxDataSet from the Parcels

        temp_data_array = new ArrayList<>();
        parcel.readList(temp_data_array, Double.class.getClassLoader());     // Get the Temperature Data ArrayList from the Parcel
        time_array = new ArrayList<>();
        parcel.readList(time_array, String.class.getClassLoader());      // Get the Time ArrayList from the Parcel
        counter_array = new ArrayList<>();
        parcel.readList(counter_array, Double.class.getClassLoader());      // Get the Counter ArrayList from the Parcel
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<BleDevice> CREATOR = new Parcelable.Creator<BleDevice>()
    {
        @Override
        public BleDevice createFromParcel(Parcel parcel)
        {
            return new BleDevice(parcel);
        }

        @Override
        public BleDevice[] newArray(int size)
        {
            return new BleDevice[size];
        }
    };

    BleDevice(String address, String name)
    {
        bleAddress = address;
        bleName = name;
        initializeDeviceData();
    }

    void setBluetoothLeService(BluetoothLeService mBluetoothLeService)
    {
        this.mBluetoothLeService = mBluetoothLeService;
        Log.d(TAG, "UV: setBluetoothLeService");
    }

    void initializeDeviceData()
    {
        counter = 0;
        maxDataSet = 10000;
        recordingDuration = 8;
        hardwareVersion = "v0X.0";
        patientName = "";
        temp_Notification = false;

        temp_Recording = false;

        temp_data_array = new ArrayList<>();
        time_array = new ArrayList<>();
        counter_array = new ArrayList<>();
    }

    void initializeBleDevice()
    {
        createBleDevice();              // Local function to create a BLE Device using the address
        createBleGatt();                // Local function to create a BLE Gatt to Connect to the BLE Device
        setmPreviouslyConnected(false);     // Initializing the previously Connected Boolean to False
        Log.d(TAG, String.format("UV: initializeBleDevice, Device: %s, Connection State %b", bleName, mConnectionState));
    }

    private boolean createBleDevice()
    {
        mBluetoothDevice = mBluetoothLeService.createBleDevice(bleAddress);
        return !(mBluetoothDevice == null);
    }

    private boolean createBleGatt()
    {
        mBluetoothGatt = mBluetoothLeService.createBleGatt(mBluetoothDevice, mGattCallback);
        return !(mBluetoothGatt==null);
    }

    void connectBleDevice()
    {
        boolean bleDeviceBoolean = createBleDevice();
        boolean bleGattBoolean = createBleGatt();
        if(bleDeviceBoolean && bleGattBoolean)
        {
            setmPreviouslyConnected(true);
            setmConnectionState(STATE_CONNECTED);
        }
        else
        {
            setmConnectionState(STATE_DISCONNECTED);
        }
        Log.d(TAG, String.format("UV: Device: %s, Connection State %b", bleName, mConnectionState));
    }

    void disconnectBleDevice()
    {
        if(mConnectionState==2)
        {
            if(mBluetoothLeService.disconnect(mBluetoothGatt))       // Returns True if it was successfully able to disconnect
            {
                mConnectionState = 0;
            }
            Log.d(TAG, String.format("UV: Device: %s, Connection State %b", bleName, mConnectionState));
        }
    }

    void reconnectDevice()
    {
        if(mConnectionState == 0)
        {
            if (mBluetoothLeService != null)
            {
                Log.d(TAG, String.format("UV: reconnectDevice Device: %s, Connection State %b", bleName, mConnectionState));
                boolean result = mBluetoothLeService.reconnectBleDevice(mBluetoothGatt, bleAddress);
                Log.d(TAG, "UV: Connect request result=" + result);
            }
        }
    }

    void initializeGattServices()
    {
        if(mPreviouslyConnected)
        {
            Log.d(TAG, String.format("UV: reconnectGattDescriptor: Write Characteristic Notification %s", bleAddress));
            mBluetoothLeService.writeCharacteristicSamplingRate(mBluetoothGatt, getSamplingRate());
            mBluetoothLeService.writeGattCharacteristicNotification(mBluetoothGatt, getTemperatureNotification());
            setRecordingFlags();
            writingDescriptor = false;
        }
    }

    void reconnectGattDescriptor()
    {
        Log.d(TAG, "UV: reconnectGattDescriptor");
//        mBluetoothLeService.reconnectBleDevice(mBluetoothGatt, bleAddress);

        Log.d(TAG, String.format("UV: Connection State %b",mBluetoothGatt.getConnectionState(mBluetoothDevice)));
        setRecordingFlags();
    }

    void resetGattDescriptors()
    {
        Log.d(TAG, "UV: resetGattDescriptors");
        setRecordingFlags();
        mBluetoothLeService.writeDescriptorNotification(mBluetoothGatt, temp_Notification);
    }

    void destroyDevice()
    {
        Log.d(TAG, "UV: destroyDevice");

        if(mBluetoothLeService != null)
        {
            mBluetoothLeService.close(mBluetoothGatt);
//            mBluetoothLeService = null;
        }
        setmPreviouslyConnected(false);
    }

    void setmConnectionState(int updatedConnectionState)
    {
        mConnectionState = updatedConnectionState;
    }

    int getmConnectionState()
    {
        return mConnectionState;
    }

    void setmPreviouslyConnected(boolean updatedPreviouslyConnected)
    {
        mPreviouslyConnected = updatedPreviouslyConnected;
    }

    boolean getmPreviouslyConnected()
    {
        return mPreviouslyConnected;
    }

    void setmGattServicesDiscovered(boolean updatedGattServicesDiscovered)
    {
        mGattServicesDiscovered = updatedGattServicesDiscovered;
    }

    boolean getmGattServicesDiscovered()
    {
        return mGattServicesDiscovered;
    }

    void setTemperatureNotification(boolean temp_Boolean)
    {
        temp_Notification = temp_Boolean;
        Log.d(TAG, "UV: setTemperatureNotification " + temp_Boolean);
    }

    boolean getTemperatureNotification()
    {
        return temp_Notification;
    }

    private void setTemperatureFlag(boolean temp_flag)
    {
        temp_Recording = temp_flag;
    }

    boolean getTemperatureFlag()
    {
        return temp_Recording;
    }

    private void setRecordingFlags()
    {
        setTemperatureFlag(temp_Notification);
    }

    double getCounter()
    {
        return counter;
    }

    double getTempValue()
    {
        return temp_data_array.get(temp_data_array.size()-1);
    }

    ArrayList<Double> getTempArray()
    {
        return temp_data_array;
    }

    ArrayList<String> getTimeArray()
    {
        return time_array;
    }

    ArrayList<Double> getCounterArray()
    {
        return counter_array;
    }

    int getCounterArrayLength()
    {
        return counter_array.size();
    }

    void setHardwareVersion(String tempHardwareVersion)
    {
        Log.d(TAG, "UV: Setting Hardware Version");
        hardwareVersion = tempHardwareVersion;
    }

    String getHardwareVersion()
    {
        return hardwareVersion;
    }

    void setPatientName(String tempPatientName)
    {
        patientName = tempPatientName;
    }

    String getPatientName()
    {
        return patientName;
    }

    // Convert String Data to Int Data and Save Data to Graph
    private void recordData(String gattValue, int characteristic_type)
    {
        if(temp_Recording && characteristic_type == MODE_TEMPERATURE)
        {
            double tempValue = Double.parseDouble(gattValue);
            temp_data_array.add(tempValue);
        }

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String currentTime1 = sdf.format(cal.getTime());

        time_array.add(currentTime1);
        counter_array.add(counter);
        counter = counter + getCounterInterval();
    }

    void setSamplingRate(int tempSamplingRate)
    {
        samplingRate = tempSamplingRate;
    }

    private int getSamplingRate()
    {
        return samplingRate;
    }

    private double getCounterInterval()
    {
        ArrayList<Double> counter_interval = new ArrayList<>(Arrays.asList(10.0, 30.0, 1.0, 5.0, 10.0, 30.0, 1.0, 5.0));
        return counter_interval.get(samplingRate);
    }

    String getCounterIntervalRange()
    {
        if(samplingRate <= 1)
        {
            return "Time (Seconds)";
        }
        else if(samplingRate <= 5)
        {
            return "Time (Minutes)";
        }
        else
        {
            return "Time (Hours)";
        }
    }

    double getCounterMax()
    {
        ArrayList<Integer> max_range = new ArrayList<>(Arrays.asList(10, 30, 1, 5, 10, 30, 1, 5));
        return max_range.get(samplingRate)*10;
    }

    void setRecordingDuration(int tempRecordingDuration)
    {
        recordingDuration = tempRecordingDuration;
    }

    long getRecordingDuration()
    {
        ArrayList<Long> recording_duration = new ArrayList<>(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));
        return recording_duration.get(recordingDuration) * 3600000L;
    }

    String getBleAddress()
    {
        return bleAddress;
    }

    String getBleName()
    {
        return bleName;
    }

    // Implement another read to see if it is currently connecting to the BLE Device
    String getConnectionState()
    {
        Log.d(TAG, "UV: getConnectionState");
        if(mConnectionState == 2 && mGattServicesDiscovered)
        {
            connectionState = "Connected";
            Log.d(TAG, "UV: getConnectionState Connected");
        }
        else if(mConnectionState == 2 && !mGattServicesDiscovered)
        {
            connectionState = "Connecting";
            Log.d(TAG, "UV: getConnectionState Connecting");
        }
        else
        {
            connectionState = "Disconnected";
            Log.d(TAG, "UV: getConnectionState disconnected");
        }
        return connectionState;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt mBluetoothGatt, int status, int newState)
        {
            super.onConnectionStateChange(mBluetoothGatt, status, newState);
            Log.d(TAG, "UV: onConnectionStateChange " + newState);
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                mBluetoothLeService.broadcastUpdate(intentAction, mBluetoothGatt.getDevice().getAddress());
                boolean service_discovery = mBluetoothGatt.discoverServices();  //Discovering Services after successful connection
                Log.d(TAG, "UV: Attempting to start service discovery:" + service_discovery);   //Printing "Discovering Services" Results
            }

            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.d(TAG, "UV: Disconnected from GATT server.");
                mBluetoothLeService.broadcastUpdate(intentAction, mBluetoothGatt.getDevice().getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt mBluetoothGatt, int status)
        {
            super.onServicesDiscovered(mBluetoothGatt, status);
            String intentAction;
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();
                for (BluetoothGattService gattService : gattServices)
                {
                    String serviceUUID = gattService.getUuid().toString();
                    Log.d(TAG, "UV: Service uuid "+serviceUUID);
                }
                intentAction = ACTION_GATT_SERVICES_DISCOVERED;
                Log.d(TAG, "UV: onServicesDiscovered");
                mBluetoothLeService.linkUUID(mBluetoothGatt);
                mBluetoothLeService.broadcastUpdate(intentAction, mBluetoothGatt.getDevice().getAddress());
            }
            else
            {
                Log.d(TAG, "UV: onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt mBluetoothGatt, BluetoothGattCharacteristic characteristic)
        {
            if(!writingDescriptor)
            {
                Log.d(TAG, String.format("UV: onCharacteristicChanged %s", mBluetoothGatt.getDevice().getAddress()));
                super.onCharacteristicChanged(mBluetoothGatt, characteristic);
                int characteristic_mode = mBluetoothLeService.onCharacteristicChanged(characteristic);
                if (characteristic_mode == MODE_TEMPERATURE)
                {
                    String gattValue = mBluetoothLeService.getmTemperatureValue();
                    recordData(gattValue, characteristic_mode);
                    mBluetoothLeService.broadcastUpdate(ACTION_DATA_AVAILABLE, mBluetoothGatt.getDevice().getAddress());
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt mBluetoothGatt, BluetoothGattCharacteristic characteristic, int status)
        {
            Log.d(TAG, "UV: onCharacteristicWrite");
            super.onCharacteristicWrite(mBluetoothGatt, characteristic, status);
            if(mBluetoothLeService.onCharacteristicWrite(status))
            {
                Log.d(TAG, "UV: all Characteristics have been written");
                writingDescriptor = true;
                mBluetoothLeService.writeDescriptorNotification(mBluetoothGatt, getTemperatureNotification());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt mBluetoothGatt, BluetoothGattCharacteristic characteristic, int status)
        {
            Log.d(TAG, "UV: onCharacteristicRead");
            super.onCharacteristicRead(mBluetoothGatt, characteristic, status);
            if(mBluetoothLeService.onCharacteristicRead(status))
            {
                Log.d(TAG, "UV: all Characteristics have been Read from");
                int characteristic_mode = mBluetoothLeService.onCharacteristicChanged(characteristic);
                if(characteristic_mode == MODE_HARDWARE_VERSION)
                {
                    String gattValue = mBluetoothLeService.getmHardwareVersionValue();
                    setHardwareVersion(gattValue);
                    mBluetoothLeService.broadcastUpdate(EXTRA_DATA, mBluetoothGatt.getDevice().getAddress());
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt mBluetoothGatt, BluetoothGattDescriptor descriptor, int status)
        {
            Log.d(TAG, "UV: onDescriptorWrite");
            super.onDescriptorWrite(mBluetoothGatt, descriptor, status);
            if(mBluetoothLeService.onDescriptorWrite(status))
            {
                Log.d(TAG, "UV: all Descriptors have been written");
                writingDescriptor = false;
                if(getTemperatureNotification())
                {
                    mBluetoothLeService.readCharacteristicHardwareVersion(mBluetoothGatt);
                }
            }
        }
    };
}