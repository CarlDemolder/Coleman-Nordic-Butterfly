package com.example.intern.ble_ex1;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class SettingsActivity extends Fragment
{
    private final static String TAG = SettingsActivity.class.getSimpleName();

    private Button save_Button;
    private Boolean stopButtonPressed = false;
//    private Button start_Button;
    private Button stop_Button;

//    private CheckBox temp_CheckBox;

    private static final int DATA_TEMPERATURE = 1;

    private int spinnerSelectedItemNumber;

    public SettingsActivity()
    {

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View subTabView = inflater.inflate(R.layout.settings_page, container, false);
//        start_Button = subTabView.findViewById(R.id.resetSamplingIntervalBtn);         //Start button to start graphing and enable notifications of Selected Values
        stop_Button = subTabView.findViewById(R.id.stopSettingsBtn);           //Stop button to stop graphing and disable notifications of Selected Values
        save_Button = subTabView.findViewById(R.id.saveSettingsBtn);           //Save button to save data from enabled graph and Selected Values
        buttonClicked(stop_Button, save_Button);

//        temp_CheckBox = subTabView.findViewById(R.id.temp_check);  //Temperature Checkbox
//        checkClicked(temp_CheckBox);     // Function used to determine whether the CheckBoxes have been selected

        spinnerSelectedItemNumber = 0;      // Initializing the spinner Selected Item Number to the shortest Sampling Interval
//        Spinner sampling_rate_Spinner = subTabView.findViewById(R.id.sampling_rate_spinner);      // Spinner used to select Sampling Rate of Notifications
//        ArrayList<String> spinnerArrayList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.sampling_rate_array)));
//        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArrayList);
//        sampling_rate_Spinner.setAdapter(spinnerArrayAdapter);
//        spinnerItemSelected(sampling_rate_Spinner);

        return subTabView;
    }

    private void buttonClicked(Button stop_Button, Button save_Button)
    {
//        start_Button.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View view)
//            {
//                Log.d(TAG, "UV: Restart Button Pressed");
//                ((MainActivity) getActivity()).restartDataTransmission();
//            }
//        });
        stop_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d(TAG, "UV: Stop Button Pressed");
                ((MainActivity) getActivity()).setAllNotifications(false);
                ((MainActivity) getActivity()).stopDataTransmission();
//                temp_CheckBox.setChecked(false);
                stopButtonPressed = true;
            }
        });
        save_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d(TAG, "UV: Save Button Pressed");
                if(!stopButtonPressed)
                {
                    ((MainActivity) getActivity()).setAllNotifications(false);
                    ((MainActivity) getActivity()).stopDataTransmission();
                }
//                temp_CheckBox.setChecked(false);
                ((MainActivity) getActivity()).saveData();
            }
        });
    }

//    private void checkClicked(CheckBox temp_CheckBox)
//    {
//        temp_CheckBox.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View view)
//            {
//                if(((CheckBox)view).isChecked())
//                {
//                    ((MainActivity) getActivity()).setNotifications(DATA_TEMPERATURE, true);
//                }
//                else
//                {
//                    ((MainActivity) getActivity()).setNotifications(DATA_TEMPERATURE, false);
//                }
//            }
//
//        });
//    }
//
//    private void spinnerItemSelected(Spinner sampling_rate_Spinner)
//    {
//        sampling_rate_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
//        {
//            @Override
//            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
//            {
//                try
//                {
//                    spinnerSelectedItemNumber = position + 15;
//                    Log.d(TAG, String.format("UV: %d", spinnerSelectedItemNumber));
//                    ((MainActivity) getActivity()).resetSamplingRateInterval(spinnerSelectedItemNumber);
//                }
//                catch (Exception e)
//                {
//                    Log.d(TAG, "UV: Spinner Item Select Exception Thrown");
//                }
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parentView)
//            {
//
//            }
//        });
//    }
}
