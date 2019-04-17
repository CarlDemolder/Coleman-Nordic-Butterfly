package com.example.intern.ble_ex1;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.Alignment;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

class Excel
{
    private final static String TAG = Excel.class.getSimpleName();
    private String outputFile;
    private String outputFileName;
    private String outputFileLocation;
    private int file_counter;
    private Boolean file_creation;
    private WritableWorkbook excelWorkbook;
    private ArrayList<WritableSheet> excelSheet;
    private File excelFile;

    private ArrayList<BleDevice> connectedBleDevices;

    Excel(ArrayList<BleDevice> BleDevices)
    {
        file_counter = 0;
        file_creation = false;
        connectedBleDevices = BleDevices;
        excelSheet = new ArrayList<>();
    }
    //Input the file name for the Excel File
    void setOutputFile()
    {
        Log.d(TAG, "UV: setOutputFile");
        if(connectedBleDevices.get(0).getTempArray().size() > 1)
        {
            outputFile = "Temperature";
        }
    }

    void createWorkbook()
    {
        Log.d(TAG, "UV: createWorkbook");
        //exports must use a temp file while writing to avoid memory hogging
        WorkbookSettings wbSettings = new WorkbookSettings();
        wbSettings.setUseTemporaryFileDuringWrite(true);
        //Set the language to English
        wbSettings.setLocale(new Locale("en", "EN"));

        File internalData = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SAVED_DATA");
        if(!internalData.mkdirs())
        {
            Log.d(TAG, "UV: Directories not created");
        }

        //add on the your app's path
        Log.d(TAG, String.format("UV: %s", internalData.toString()));
        //make them in case they're not there
        if(!internalData.isDirectory())
        {
            boolean status = internalData.mkdirs();
            Log.d(TAG, String.format("UV: Make Directories: %b", status));
        }

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);            // Simple Date Format to get Day, Month, Year, Etc...
        String currentDate = sdf.format(cal.getTime());

        while(!file_creation)
        {
            //create a standard java.io.File object for the Workbook to use
            outputFileName= outputFile+"_"+currentDate+"_"+String.valueOf(file_counter)+".xls";
            excelFile = new File(internalData,(outputFileName));
            if(!excelFile.exists())
            {
                file_creation = true;
                Log.d(TAG, "UV: File did not exist");
            }
            else
            {
                file_counter = file_counter + 1;                    // File Counter to increase for multiple data samples
            }
        }

        excelWorkbook = null;
        try
        {
            //create a new WritableWorkbook using the java.io.File and
            //WorkbookSettings from above
            excelWorkbook = Workbook.createWorkbook(excelFile, wbSettings);
        }
        catch(IOException ex)
        {
            Log.d(TAG, String.format("UV: IOException Thrown %s", ex.getMessage()));
        }
        outputFileLocation = excelFile.getAbsolutePath();
        Log.d(TAG, String.format("UV: %s", outputFileLocation));
    }

    void createSheets()
    {
        Log.d(TAG, "UV: createSheet");
        try
        {
            for(int i = 0; i < connectedBleDevices.size(); i++)
            {
                excelSheet.add(excelWorkbook.createSheet(connectedBleDevices.get(i).getBleName(), i));
                WritableFont headerFont = new WritableFont(WritableFont.TIMES, 12, WritableFont.BOLD);
                WritableCellFormat headerFormat = new WritableCellFormat(headerFont);
                headerFormat.setAlignment(Alignment.CENTRE);

                //Label = column and row
                Label timeLabel = new Label(0, 0, "Timestamp");
                Label counterLabel = new Label(1, 0, "Counter");
                Label temperatureHeadingLabel = new Label(2, 0, "Temperature (C)");

                timeLabel.setCellFormat(headerFormat);
                counterLabel.setCellFormat(headerFormat);
                temperatureHeadingLabel.setCellFormat(headerFormat);

                excelSheet.get(i).addCell(timeLabel);
                excelSheet.get(i).addCell(counterLabel);
                excelSheet.get(i).addCell(temperatureHeadingLabel);
            }
        }
        catch(WriteException e)
        {
            e.printStackTrace();
        }
    }

    void writeData()
    {
        WritableFont dataFont = new WritableFont(WritableFont.TIMES, 10, WritableFont.NO_BOLD);
        WritableCellFormat dataFormat = new WritableCellFormat(dataFont);
        int row;
        for(int j = 0; j < connectedBleDevices.size(); j++)
        {
            Log.d(TAG, String.format("UV: Writing Counter/Time Data %s", connectedBleDevices.get(j).getBleName()));
            for (int i = 0; i < connectedBleDevices.get(j).getCounterArray().size(); i++)
            {
                row = i + 1;
                //Column, Row
                Label time1 = new Label(0, row, connectedBleDevices.get(j).getTimeArray().get(i));
                time1.setCellFormat(dataFormat);
                Number counter1 = new Number(1, row, connectedBleDevices.get(j).getCounterArray().get(i));
                counter1.setCellFormat(dataFormat);
                try
                {
                    excelSheet.get(j).addCell(time1);
                    excelSheet.get(j).addCell(counter1);
                }
                catch (WriteException e)
                {
                    Log.d(TAG, "UV: write Counter/Time Data didn't work");
                    e.printStackTrace();
                }
            }
            if (connectedBleDevices.get(j).getTempArray().size() > 1)
            {
                for (int i = 0; i < connectedBleDevices.get(j).getCounterArray().size(); i++)
                {
                    row = i + 1;
                    //Column, Row
                    Number tempData1 = new Number(2, row, connectedBleDevices.get(j).getTempArray().get(i));
                    tempData1.setCellFormat(dataFormat);
                    try
                    {
                        excelSheet.get(j).addCell(tempData1);
                    }
                    catch (WriteException e)
                    {
                        Log.d(TAG, "UV: write Temperature Data didn't work");
                        e.printStackTrace();
                    }
                }
            }
        }
        try
        {
            excelWorkbook.write();
            Log.d(TAG, "UV: Finished writing to Excel Workbook Successfully");
        }
        catch(IOException e)
        {
            e.printStackTrace();
            Log.d(TAG, "UV: exception occured when excel Workbook writing");
        }
    }

    void closeWorkbook()
    {
        Log.d(TAG, "UV: closeWorkbook");
        try
        {
            excelWorkbook.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.d(TAG, "UV: IOException Thrown");
        }
        catch (WriteException e)
        {
            e.printStackTrace();
            Log.d(TAG, "UV: WriteException Thrown");
        }
    }

    File getExcelFile()
    {
        return excelFile;
    }

    String getOutputFileLocation()
    {
        return outputFileLocation;
    }
}
