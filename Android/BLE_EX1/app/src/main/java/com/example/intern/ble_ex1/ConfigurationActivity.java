package com.example.intern.ble_ex1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;

public class ConfigurationActivity extends AppCompatActivity
{
    private final static String TAG = ConfigurationActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private ArrayList<String> deviceArrayNames;
    private ArrayList<String> deviceArrayAddresses;
    private ArrayList<BleDevice> bleSensors;

    private int spinnerSelectedItemNumber;

    private Intent mainIntent;      // Intent to create the Main Activity and pass through elements of the Bluetooth Devices

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration_page);

        //Connect U.I Elements
        CheckBox temp_CheckBox = findViewById(R.id.temp_check);  //Temperature Checkbox

        Button start_Button = findViewById(R.id.startBtn);         //Start button to start graphing and enable notifications of Selected Values

        initializeSpinner();        // Create a spinner to let the user select the sampling rate

        mainIntent = new Intent(this, MainActivity.class);

        updateBluetoothInfo();      // Get Bluetooth Names and Addresses from Device Scan Activity
        initializeBleDevice();      // Create Arraylist containing BLE Devices
        checkClicked(temp_CheckBox);     // Function used to determine whether the CheckBoxes have been selected
        buttonClicked(start_Button);                // Button used to start the main activity once all settings have been used
    }

    private void updateBluetoothInfo()
    {
        deviceArrayNames = new ArrayList<>();       // Initializing the ArrayList of device Names
        deviceArrayAddresses = new ArrayList<>();   // Initializing the ArrayList of device Addresses
        final Intent startIntent = getIntent();
        deviceArrayNames = startIntent.getStringArrayListExtra(EXTRAS_DEVICE_NAME);    //Get Device Name from Previous Intent
        deviceArrayAddresses = startIntent.getStringArrayListExtra(EXTRAS_DEVICE_ADDRESS);  //Get Device Address from Previous Intent

        int lengthBLEDevices = deviceArrayAddresses.size();
        Log.d(TAG, String.format("UV: %d", lengthBLEDevices));
        for(int i = 0; i < deviceArrayAddresses.size(); i++)
        {
            Log.d(TAG, String.format("UV: %s", deviceArrayAddresses.get(i)));
            Log.d(TAG, String.format("UV: %s", deviceArrayNames.get(i)));
        }
    }

    // Initialize an array of Ble Devices
    private void initializeBleDevice()
    {
        bleSensors = new ArrayList<>();             // Initializing the ArrayList of BLE Devices
        for(int i = 0; i < deviceArrayNames.size(); i++)
        {
            BleDevice tempDevice = new BleDevice(deviceArrayAddresses.get(i), deviceArrayNames.get(i));
            bleSensors.add(tempDevice);
        }
    }

    // Initialize the Spinner to let the User selected the sampling rate for the device
    private void initializeSpinner()
    {
        spinnerSelectedItemNumber = 0;      // Preseting the spinner Selected Item Number to the shortest Sampling Interval
        Spinner sampling_rate_Spinner = findViewById(R.id.sampling_rate_spinner);      // Spinner used to select Sampling Rate of Notifications
        ArrayList<String> spinnerArrayList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.sampling_rate_array)));
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArrayList);
        sampling_rate_Spinner.setAdapter(spinnerArrayAdapter);
        spinnerItemSelected(sampling_rate_Spinner);
    }

    private void checkClicked(CheckBox temp_CheckBox)
    {
        temp_CheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(((CheckBox)view).isChecked())
                {
                    for(int i = 0; i < bleSensors.size(); i++)
                    {
                        bleSensors.get(i).setTemperatureNotification(true);
                    }
                }
                else
                {
                    for(int i = 0; i < bleSensors.size(); i++)
                    {
                        bleSensors.get(i).setTemperatureNotification(false);
                    }
                }
            }

        });
    }

    private void buttonClicked(Button start_Button)
    {
        start_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Bundle configurationBundle = new Bundle();
                configurationBundle.putParcelableArrayList(MainActivity.EXTRAS_BLE_DEVICES, bleSensors);
                mainIntent.putExtras(configurationBundle);
                startActivity(mainIntent);
            }
        });
    }

    private void spinnerItemSelected(Spinner sampling_rate_Spinner)
    {
        sampling_rate_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
            {
                try
                {
                    spinnerSelectedItemNumber = position;
                    Log.d(TAG, String.format("UV: %d", spinnerSelectedItemNumber));
                    for(int i = 0; i < bleSensors.size(); i++)
                    {
                        bleSensors.get(i).setSamplingRate(spinnerSelectedItemNumber);
                    }
                }
                catch (Exception e)
                {
                    Log.d(TAG, "UV: Spinner Item Select Exception Thrown");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView)
            {

            }
        });
    }
}
