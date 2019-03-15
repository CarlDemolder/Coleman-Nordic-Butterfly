/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.intern.ble_ex1;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity
{
    private final static String TAG = DeviceScanActivity.class.getSimpleName();

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;

    private boolean mScanning;
    private Handler mHandler;

    private boolean enableScanning;
    private boolean currentScanning;

    private Intent connectIntent;

    private static final int REQUEST_ENABLE_BT = 1;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();

        enableScanning = false;     // Boolean used to only scan when button has been pressed
        currentScanning = false;    // Boolean used to determine if scanning is in progress

        setContentView(R.layout.ble_connect);

        Log.d(TAG, "UV: DeviceScanActivity: onCreate");

        //Connect U.I Elements
        Button  connect_Ble_Button = (Button) findViewById(R.id.ConnectBleBtn);     //Connect to BLE Device
        Button start_Scan_Button = (Button) findViewById(R.id.startScanBtn);        //Start BLE Scan
        Button stop_Scan_Button = (Button) findViewById(R.id.stopScanBtn);          //Stop BLE Scan

        mLeDeviceListAdapter = new LeDeviceListAdapter();   // Create an object a List Adapter
        setListAdapter(mLeDeviceListAdapter);        // Initialize List from ListActivity

        connectIntent = new Intent(this, ConfigurationActivity.class);

        checkBLE();     // Check if the function has BLE functionality
        initialize_bluetooth_adapter(); // Initialize Bluetooth Adapter
        enable_bluetooth(); // Enable Bluetooth
        checkBluetooth();   // Check if Bluetooth has been enabled

        buttonClicked(start_Scan_Button, stop_Scan_Button, connect_Ble_Button);     //Enable Button Click Listener
    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "UV: DeviceScanActivity: onResume");
        super.onResume();
        enable_bluetooth();
        mLeDeviceListAdapter.clear();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "UV: DeviceScanActivity: onActivityResult");

        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
        {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "UV: DeviceScanActivity: onPause");
        super.onPause();
        stopScan();
        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Log.d(TAG, "UV: onListItemClick");
        if(mScanning)
        {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
    }

    private void buttonClicked(Button start_Scan_Button, Button stop_Scan_Button, Button connect_Ble_Button)
    {
        start_Scan_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d(TAG, "UV: Scan Button Pressed");
                mLeDeviceListAdapter.clear();
                mLeDeviceListAdapter.notifyDataSetChanged();
                enableScanning = true;
                startScan();
            }
        });

        stop_Scan_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d(TAG, "UV: Stop Button Pressed");
                enableScanning = false;
                stopScan();
            }
        });

        connect_Ble_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d(TAG, "UV: Connect Button Pressed");
                enableScanning = false;
                ArrayList<String> selectedDeviceNames = mLeDeviceListAdapter.getSelectedNames();
                ArrayList<String> selectedDeviceAddresses = mLeDeviceListAdapter.getSelectedAddresses();

                connectIntent.putExtra(ConfigurationActivity.EXTRAS_DEVICE_NAME, selectedDeviceNames);
                connectIntent.putExtra(ConfigurationActivity.EXTRAS_DEVICE_ADDRESS, selectedDeviceAddresses);
                startActivity(connectIntent);
            }
        });
    }

    // This function checks if BLE is enabled on the device
    private void checkBLE()
    {
        Log.d(TAG, "UV: DeviceScanActivity: checkBLE");
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // This function checks if Bluetooth is enabled on the device
    private void checkBluetooth()
    {
        Log.d(TAG, "UV: DeviceScanActivity: CheckBluetooth");
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Initialize Bluetooth Adapter
    private void initialize_bluetooth_adapter()
    {
        // Initializes Bluetooth adapter.
        Log.d(TAG, "UV: DeviceScanActivity: Initialize Bluetooth Adapter");
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    // Eanble Bluetooth on Device
    private void enable_bluetooth()
    {
        Log.d(TAG, "UV: DeviceScanActivity: Enable Bluetooth Adapter");
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void startScan()
    {
        Log.d(TAG, "UV: DeviceScanActivity: startScan");
        if(enableScanning && !currentScanning)
        {
            scanLeDevice(true);
            currentScanning = true;
        }
    }

    private void stopScan()
    {
        Log.d(TAG, "UV: DeviceScanActivity: stopScan");
        if(!enableScanning && currentScanning)
        {
            scanLeDevice(false);
            currentScanning = false;
        }
    }

    private void scanLeDevice(final boolean enable)
    {
        Log.d(TAG, "UV: DeviceScanActivity: ScanLeDevice");
        if(enable)
        {
            Log.d(TAG, "UV: ScanLeDevice: Start Scanning");
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        else
        {
            Log.d(TAG, "UV: ScanLeDevice: Stop Scanning");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private ArrayList<BluetoothDevice> mLeDevices;      // All Available Bluetooth Devices
        private ArrayList<String> selectedNames;     // All Selected Bluetooth Devices through the Checkbox
        private ArrayList<String> selectedAddresses;     // All Selected Bluetooth Devices through the Checkbox
        private LayoutInflater mInflator;

        public LeDeviceListAdapter()
        {
            super();
            mLeDevices = new ArrayList<>();     // Initializing All Available Bluetooth Devices
            selectedNames = new ArrayList<>();     // Initializing Selected Bluetooth Device List
            selectedAddresses = new ArrayList<>();     // Initializing Selected Bluetooth Device List
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device)
        {
            if (!mLeDevices.contains(device))
            {
                mLeDevices.add(device);
            }
        }

        private void addSelectedDevice(String name, String address)
        {
            if (!selectedAddresses.contains(address))
            {
                selectedAddresses.add(address);
                selectedNames.add(name);
            }
        }

        private void removeSelectedDevice(String name, String address)
        {
            if (selectedAddresses.contains(address))
            {
                selectedAddresses.remove(address);
                selectedNames.remove(name);
            }
        }

        public ArrayList<String> getSelectedNames()
        {
            Log.d(TAG, "UV: Checkbox Checked, getSelectedNames");
            return selectedNames;
        }

        public ArrayList<String> getSelectedAddresses()
        {
            Log.d(TAG, "UV: Checkbox Checked, getSelectedAddresses");
            return selectedAddresses;
        }

        public void clear()
        {
            mLeDevices.clear();
        }

        @Override
        public int getCount()
        {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i)
        {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup)
        {
            final ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null)
            {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);   // Gets Text Parameter for Device Address in Layout
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name); // Gets Text Parameter for Device Name in Layout
                viewHolder.deviceCheckBox = (CheckBox) view.findViewById(R.id.BLECheckBox);   // Gets CheckBox Parameter for Device Selected from ArrayList
                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
            {
                viewHolder.deviceName.setText(deviceName);
            }
            else
            {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAddress.setText(device.getAddress());

            viewHolder.deviceCheckBox.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    CheckBox device_cb = (CheckBox) v;
                    if (device_cb.isChecked())
                    {
                        Log.d(TAG, String.format("UV: Checkbox Checked %s", viewHolder.deviceAddress.getText()));
                        addSelectedDevice(viewHolder.deviceName.getText().toString(), viewHolder.deviceAddress.getText().toString());
                    }
                    else
                    {
                        Log.d(TAG ,String.format("UV: Checkbox Unchecked %s", viewHolder.deviceAddress.getText()));
                        removeSelectedDevice(viewHolder.deviceName.getText().toString(), viewHolder.deviceAddress.getText().toString());
                    }
                }
            });


            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder
    {
        TextView deviceName;
        TextView deviceAddress;
        CheckBox deviceCheckBox;
    }
}