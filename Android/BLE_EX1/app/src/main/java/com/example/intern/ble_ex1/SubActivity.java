package com.example.intern.ble_ex1;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Locale;

public class SubActivity extends Fragment
{
    private final static String TAG = SubActivity.class.getSimpleName();

    private BleDevice connectedBleSensor;
    public static final String EXTRAS_BLE_DEVICE = "BLE_DEVICE";

    private boolean mConnectionState = false;

    private GraphView dataGraph;
    private GridLabelRenderer gridLabel;

    private LineGraphSeries<DataPoint> temp_series;

    private DataPoint[] temp_data;

    private int previously_connected;

    private static  final int maxDataSet = 10000;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private TextView dataValue;

    private TextView connectionState;
    private TextView deviceAddress;
    private TextView deviceName;

    public SubActivity()
    {

    }

    /* The system calls this when creating the fragment. Within your implementation,
    * you should initialize essential components of the fragment that you want to retain when the fragment is paused
    * or stopped, then resumed.
    */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(this.getArguments()!= null)
        {
            connectedBleSensor = this.getArguments().getParcelable(EXTRAS_BLE_DEVICE);       // Get BleSensor that is passed through from the Main Activity
        }
        if(connectedBleSensor != null)
        {
            Log.d(TAG, String.format("UV: onCreate %s", connectedBleSensor.getBleName()));
        }
    }

    /* The system calls this when it's time for the fragment to draw its user interface for the first time.
    * To draw a UI for your fragment, you must return a View from this method that is the root of your fragment's layout.
    */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(TAG, String.format("UV: OnCreateView %s", connectedBleSensor.getBleName()));
        View subTabView = inflater.inflate(R.layout.content_main, container, false);
        dataValue = subTabView.findViewById(R.id.data_value);        //Initializing Textview to display BLE Temperature Data
        dataGraph = subTabView.findViewById(R.id.graph);             //Graphview to Display Data
        gridLabel = dataGraph.getGridLabelRenderer();

        deviceName = subTabView.findViewById(R.id.ble_device_name_connected); //Sets the Text of the Device name
        deviceName.setText(connectedBleSensor.getBleName());
        deviceAddress = subTabView.findViewById(R.id.ble_device_address_connected);   //Sets the Text of the Device Address
        deviceAddress.setText(connectedBleSensor.getBleAddress());
        connectionState = subTabView.findViewById(R.id.ble_device_status);   //Sets the Text of the Device Connection Status
        connectionState.setText(connectedBleSensor.getConnectionState());

        return subTabView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, String.format("UV: OnActivityCreated %s", connectedBleSensor.getBleName()));
        createGraph();
    }

    private void createGraph()
    {
        Log.d(TAG, "UV: createGraph()");
        if(connectedBleSensor.getTemperatureNotification())
        {
            temp_data = new DataPoint[1];                                       //Initializing Temperature Data
            temp_data[0] = new DataPoint(0,0);

            //Initializing Temperature Dataset on Graph
            temp_series = new LineGraphSeries<>(temp_data);
            temp_series.setTitle("Temperature");
            temp_series.setColor(Color.RED);
            temp_series.setThickness(8);
            temp_series.setDrawDataPoints(true);        //Setting Data Points to be visible on graph
            temp_series.setDataPointsRadius(5);         //Setting the radius of the Data Point
            dataGraph.addSeries(temp_series);

            dataGraph.getViewport().setYAxisBoundsManual(true);
            dataGraph.getViewport().setMinY(20);
            dataGraph.getViewport().setMaxY(40);

            dataGraph.getViewport().setXAxisBoundsManual(true);
            dataGraph.getViewport().setMinX(0);
            dataGraph.getViewport().setMaxX(connectedBleSensor.getCounterMax());

            dataGraph.getLegendRenderer().setVisible(true);

            gridLabel.setHorizontalAxisTitle(connectedBleSensor.getCounterIntervalRange());
            gridLabel.setVerticalAxisTitle("Temperature (C)");
            dataGraph.setTitle("Body Temperature");
        }
    }


    // Convert String Data to Int Data and Save Data to Graph
    public void updateGraph()
    {
        Log.d(TAG, "UV: updateGraph");
        if(connectedBleSensor.getTemperatureFlag())
        {
            DataPoint values = new DataPoint(connectedBleSensor.getCounter(), connectedBleSensor.getTempValue());
            if(connectedBleSensor.getCounterArrayLength() < 10)
            {
                temp_series.appendData(values, false, maxDataSet);
            }
            else
            {
                temp_series.appendData(values, true, maxDataSet);
            }
            dataValue.setText(String.format(Locale.ENGLISH, "%f",connectedBleSensor.getTempValue()));
        }
    }

    public void updateConnectionState(int connectionMode)
    {
        if(connectionMode == STATE_DISCONNECTED)
        {
            connectionState.setText(R.string.disconnected);
        }
        if(connectionMode == STATE_CONNECTING)
        {
            connectionState.setText(R.string.connecting);
        }
        if(connectionMode == STATE_CONNECTED)
        {
            connectionState.setText(R.string.connected);
        }
    }
}
