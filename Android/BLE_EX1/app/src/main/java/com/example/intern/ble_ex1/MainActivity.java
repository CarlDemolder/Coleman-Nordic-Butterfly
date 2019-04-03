package com.example.intern.ble_ex1;

import android.content.*;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity
{
    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRAS_BLE_DEVICES = "BLE_DEVICES";
    public static final String CONNECTED_GATT = "CONNECTED_GATT";

    private ArrayList<BleDevice> connectedBleSensors;

    private BluetoothLeService mBluetoothLeService;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static final int DATA_TEMPERATURE = 1;
    private static final int DATA_UV = 2;
    private static final int DATA_BATTERY = 3;

    public ViewPagerAdapter viewPagerAdapter;

    private Excel excelFile;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_page);

        Intent mainActivityIntent = getIntent();

        connectedBleSensors = new ArrayList<>();             // Initializing the Connected BLE Sensors
        if(mainActivityIntent.getExtras() != null)
        {
            connectedBleSensors = mainActivityIntent.getExtras().getParcelableArrayList(EXTRAS_BLE_DEVICES);        // Getting the BLE devices from the previous Activity
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager viewPager = findViewById(R.id.view_pager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        bindGattService();
    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "UV: onResume");
        super.onResume();
//        for(int i = 0; i < connectedBleSensors.size(); i++)
//        {
//            connectedBleSensors.get(i).reconnectDevice();
//        }
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "UV: onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "UV: onDestroy");
        super.onDestroy();
        for(int i = 0; i < connectedBleSensors.size(); i++)
        {
            connectedBleSensors.get(i).destroyDevice();
        }
        this.unregisterReceiver(mGattUpdateReceiver);
        this.unbindService(mServiceConnection);
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        startActivity(new Intent(this, DeviceScanActivity.class));
        finish();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu)
//    {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//
//        menu.findItem(R.id.menu_connect).setEnabled(false);
//        menu.findItem(R.id.menu_disconnect).setEnabled(true);
//
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item)
//    {
//        switch(item.getItemId())
//        {
//            case R.id.menu_connect:
//                for(int i = 0; i < connectedBleSensors.size(); i++)
//                {
//                    connectedBleSensors.get(i).connectBleDevice();
//                }
//                return true;
//            case R.id.menu_disconnect:
//                for(int i = 0; i < connectedBleSensors.size(); i++)
//                {
//                    connectedBleSensors.get(i).disconnectBleDevice();
//                }
//                return true;
//            case android.R.id.home:
//                onBackPressed();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    private void bindGattService()
    {
        Log.d(TAG, "UV: initializeBleDevices");
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);  //Creates an intent for the Bluetooth Service
        boolean bind_status = this.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);   //Binds Service to the Gatt Service
        Log.d(TAG, String.format("UV: bind Service %b", bind_status));
        this.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());       // Bind Receiver to communicate between Service and Activity
    }

//    private void reconnectGattDescriptors()
//    {
//        // This function checks to see if all BLE devices are connected and their services are discovered before enabling Notifications
//        int discoveredDevices = 0;
//        for(int i = 0; i < connectedBleSensors.size(); i++)
//        {
//            if(connectedBleSensors.get(i).getmConnectionState() == 2 && connectedBleSensors.get(i).getmGattServicesDiscovered())
//            {
//                discoveredDevices++;
//            }
//        }
//        if(discoveredDevices == connectedBleSensors.size())
//        {
//            Log.d(TAG, "UV: reconnectGattDescriptors true");
//            for(int i = 0; i < connectedBleSensors.size(); i++)
//            {
////                connectedBleSensors.get(i).reconnectGattCharacteristic();
//            }
//            for(int i = 0; i < connectedBleSensors.size(); i++)
//            {
////                connectedBleSensors.get(i).reconnectGattDescriptor();
//            }
//        }
//        else
//        {
//            Log.d(TAG, "UV: reconnectGattDescriptors false");
//        }
//    }

    private void initializeBleDevices()
    {
        for(int i = 0; i < connectedBleSensors.size(); i++)
        {
            connectedBleSensors.get(i).setBluetoothLeService(mBluetoothLeService);      // Pass the Bluetooth Service to the BLE Device Object
            connectedBleSensors.get(i).initializeBleDevice();           // Initialize the BLE Device
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize())
            {
                Log.d(TAG, "UV: Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.d(TAG, "UV: onServiceConnected");
            initializeBleDevices();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            Log.d(TAG, "UV: onServiceDisconnected");
            mBluetoothLeService = null;
        }
    };


    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.EXTRA_DATA);
        return intentFilter;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = "";
            if(intent.getAction() != null)
            {
                action = intent.getAction();
            }
            final String gattBleAddress = intent.getStringExtra(CONNECTED_GATT);   // Get the corresponding GATT from the BluetoothLeService
            for(int i = 0; i < connectedBleSensors.size(); i++)
            {
                if(connectedBleSensors.get(i).getBleAddress().equals(gattBleAddress))
                {
                    Log.d(TAG, String.format("UV: onReceive %s", connectedBleSensors.get(i).getBleName()));
                    switch(action)
                    {
                        case BluetoothLeService.ACTION_GATT_CONNECTED:
                            connectedBleSensors.get(i).setmConnectionState(STATE_CONNECTED);
                            connectedBleSensors.get(i).setmPreviouslyConnected(true);
                            ((SubActivity) viewPagerAdapter.getItem(i)).updateConnectionState(STATE_CONNECTING);
                            Log.d(TAG, String.format("UV: BLE GATT Connected %s", connectedBleSensors.get(i).getBleName()));
                            break;

                        case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                            connectedBleSensors.get(i).disconnectBleDevice();
                            connectedBleSensors.get(i).setmConnectionState(STATE_DISCONNECTED);
                            connectedBleSensors.get(i).setmGattServicesDiscovered(false);
                            Log.d(TAG, String.format("UV: BLE GATT Disconnected %s", connectedBleSensors.get(i).getBleName()));
                            ((SubActivity) viewPagerAdapter.getItem(i)).updateConnectionState(STATE_DISCONNECTED);
                            connectedBleSensors.get(i).reconnectDevice();
                            break;

                        case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                            // Show all the supported services and characteristics on the user interface.
                            Log.d(TAG, String.format("UV: BLE GATT Services Discovered %s", connectedBleSensors.get(i).getBleName()));
                            connectedBleSensors.get(i).setmGattServicesDiscovered(true);
                            ((SubActivity) viewPagerAdapter.getItem(i)).updateConnectionState(STATE_CONNECTED);
                            connectedBleSensors.get(i).initializeGattServices();       // Enables Characteristic Notification, then writes value to Descriptor
                            break;

                        case BluetoothLeService.ACTION_DATA_AVAILABLE:
                            Log.d(TAG, String.format("UV: BLE GATT Action Data Available %s", connectedBleSensors.get(i).getBleName()));
                            ((SubActivity) viewPagerAdapter.getItem(i)).updateGraph();      // Update Graph
                            break;

                        case BluetoothLeService.EXTRA_DATA:
                            Log.d(TAG, String.format("UV: BLE GATT Extra Data Available %s", connectedBleSensors.get(i).getBleName()));
                            ((SubActivity) viewPagerAdapter.getItem(i)).updateHardwareVersion();      // Update Hardware Version
                        default:
                            break;
                    }
                }
            }
        }
    };


    private void setupViewPager(final ViewPager viewPager)
    {
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        for(int i = 0; i < connectedBleSensors.size(); i++)
        {
            Bundle subActivityBundle = new Bundle();
            subActivityBundle.putParcelable(SubActivity.EXTRAS_BLE_DEVICE, connectedBleSensors.get(i));
            Fragment subActivityFragment = new SubActivity();
            subActivityFragment.setArguments(subActivityBundle);
            viewPagerAdapter.addFragment(subActivityFragment, connectedBleSensors.get(i).getBleName());
            Log.d(TAG, String.format("UV: setupViewPager %s", connectedBleSensors.get(i).getBleName()));
        }
        Fragment settingsActivityFragment = new SettingsActivity();
        viewPagerAdapter.addFragment(settingsActivityFragment, getResources().getString(R.string.action_settings));

        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(connectedBleSensors.size()+1);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
//                Log.d(TAG, "UV: onPageScrolled");
            }

            @Override
            public void onPageSelected(int position)
            {
//                Log.d(TAG, "UV: onPageSelected");
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {
//                Log.d(TAG, "UV: onPageScrollStateChanged");
            }
        });
    }

    public void updateViewPagerAdapter()
    {
        Log.d(TAG, "UV: updateViewPagerAdapter");
        viewPagerAdapter.notifyDataSetChanged();
    }

    public class ViewPagerAdapter extends FragmentStatePagerAdapter
    {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager fragmentManager)
        {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position)
        {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount()
        {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title)
        {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return mFragmentTitleList.get(position);
        }

        @Override
        public int getItemPosition(Object object)
        {
//            return POSITION_NONE;
            return POSITION_UNCHANGED;
        }
    }

    public void setNotifications(int notification_type, boolean notification_state)
    {
        if(notification_type == DATA_TEMPERATURE)
        {
            for(int i = 0; i < connectedBleSensors.size(); i++)
            {
                connectedBleSensors.get(i).setTemperatureNotification(notification_state);
            }
        }
    }

    public void setAllNotifications(boolean notification_state)
    {
        setNotifications(DATA_TEMPERATURE, notification_state);     // Set All Temperature Notifications to notification_state
    }

    public void emailExcelFile()
    {
        File savedExcelFile = excelFile.getExcelFile();
        Log.d(TAG, "UV: Email Excel Testing File to Email");
        try
        {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            Uri path = Uri.fromFile(savedExcelFile);
//            Intent emailIntent = new Intent(Intent.ACTION_SEND, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", savedExcelFile));
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            emailIntent.setType("vnd.android.cursor.dir/email");        // set the type to 'email'
//            String to[] = {"CarlDemolder@gmail.com"};
            String to[] = {"Linda.Franck@ucsf.edu"};
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_STREAM, path);        // the attachment
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Temperature Test Data");     // the mail subject
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void restartDataTransmission()
    {
        // Only Writes to Descriptors, not the characteristic to enable Notifications from the device to the phone
        for(int i = 0; i < connectedBleSensors.size(); i++)
        {
            connectedBleSensors.get(i).reconnectGattDescriptor();       // Enables Characteristic Notification, then writes value to Descriptor
        }
        updateViewPagerAdapter();
    }

    public void resetSamplingRateInterval(int sampling_rate)
    {
        Log.d(TAG, "UV: resetSamplingRateInterval");
        for(int i = 0; i < connectedBleSensors.size(); i++)
        {
            connectedBleSensors.get(i).setSamplingRate(sampling_rate);
        }
    }

    public void stopDataTransmission()
    {
        // Only Writes to Descriptors, not the characteristic to enable Notifications from the device to the phone
        for(int i = 0; i < connectedBleSensors.size(); i++)
        {
            connectedBleSensors.get(i).resetGattDescriptors();
//            connectedBleSensors.get(i).destroyDevice();
        }
    }

    public void destroyDevices()
    {
        for(int i = 0; i < connectedBleSensors.size(); i++)
        {
            connectedBleSensors.get(i).destroyDevice();
        }
    }

    public void saveData()
    {
        Log.d(TAG, "UV: saveData");
        excelFile = new Excel(connectedBleSensors);        //Creating an instance of an Excel Spreadsheet to store values properly
        excelFile.setOutputFile();
        excelFile.createWorkbook();
        excelFile.createSheets();
        excelFile.writeData();
        excelFile.closeWorkbook();
        emailExcelFile();
        for(int i = 0; i < connectedBleSensors.size(); i++)
        {
            connectedBleSensors.get(i).initializeDeviceData();
        }
    }
}
