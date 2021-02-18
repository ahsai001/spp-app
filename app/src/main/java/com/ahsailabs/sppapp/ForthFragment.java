package com.ahsailabs.sppapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

public class ForthFragment extends Fragment implements SerialInputOutputManager.Listener{
    private TextView displayTextView;
    private EditText editText;
    private Button sendBtn;

    private UsbSerialPort usbSerialPort;
    private BroadcastReceiver broadcastReceiver;
    private SerialInputOutputManager usbIoManager;
    private UsbPermission usbPermission = UsbPermission.Unknown;


    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private boolean connected = false;

    private TabLayout tlPanel;
    private LinearLayout llMainPanel;
    private LinearLayout llChartPanel;
    private LineChart lcChart;
    private StringBuilder strBuilder = new StringBuilder();

    public ForthFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(INTENT_ACTION_GRANT_USB)) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_third, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        displayTextView = view.findViewById(R.id.diplayTextView);
        editText = view.findViewById(R.id.editText);
        sendBtn = view.findViewById(R.id.sendBtn);
        displayTextView.setMovementMethod(new ScrollingMovementMethod());
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String editTextString = editText.getText().toString()+"\n";
                try {
                    usbSerialPort.write(editTextString.getBytes(), WRITE_WAIT_MILLIS);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                editText.getText().clear();
            }
        });

        chartSetup(view);

        connect();

    }

    private void chartSetup(View view) {
        tlPanel = view.findViewById(R.id.tlPanel);
        llMainPanel = view.findViewById(R.id.llMainPanel);
        llChartPanel = view.findViewById(R.id.llChartPanel);
        tlPanel.addTab(tlPanel.newTab().setText("Text"));
        tlPanel.addTab(tlPanel.newTab().setText("Chart"));
        tlPanel.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getText().equals("Text")){
                    llMainPanel.setVisibility(View.VISIBLE);
                    llChartPanel.setVisibility(View.GONE);
                } else {
                    llMainPanel.setVisibility(View.GONE);
                    llChartPanel.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        lcChart = view.findViewById(R.id.lcChart);
        setupChart();
        setupAxes();
        setupData();
        setLegend();
    }

    private void setupChart() {
        // disable description text
        lcChart.getDescription().setEnabled(false);
        // enable touch gestures
        lcChart.setTouchEnabled(true);
        // if disabled, scaling can be done on x- and y-axis separately
        lcChart.setPinchZoom(true);
        // enable scaling
        lcChart.setScaleEnabled(true);
        lcChart.setDrawGridBackground(false);
        // set an alternative background color
        lcChart.setBackgroundColor(Color.DKGRAY);
    }

    private void setupAxes() {
        XAxis xl = lcChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = lcChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        //leftAxis.setAxisMaximum(TOTAL_MEMORY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = lcChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Add a limit line
        /*
        LimitLine ll = new LimitLine(LIMIT_MAX_MEMORY, "Upper Limit");
        ll.setLineWidth(2f);
        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll.setTextSize(10f);
        ll.setTextColor(Color.WHITE);
        // reset all limit lines to avoid overlapping lines
        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(ll);
        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);
         */
    }

    private void setupData() {
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        lcChart.setData(data);
    }

    private void setLegend() {
        // get the legend (only possible after setting data)
        Legend l = lcChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.CIRCLE);
        l.setTextColor(Color.WHITE);
    }


    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Serial Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColors(ColorTemplate.VORDIPLOM_COLORS[0]);
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);
        // To show values of each point
        set.setDrawValues(true);

        return set;
    }

    private void addEntry(float value) {
        LineData data = lcChart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), value), 0);

            // let the chart know it's data has changed
            data.notifyDataChanged();
            lcChart.notifyDataSetChanged();

            // limit the number of visible entries
            lcChart.setVisibleXRangeMaximum(15);

            // move to the latest entry
            lcChart.moveViewToX(data.getEntryCount());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
    }

    @Override
    public void onPause() {
        if(connected) {
            showInfo("disconnected");
            disconnect();
        }
        requireActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    private void connect() {
        // Find all available drivers from attached devices.
        UsbManager usbManager = (UsbManager) requireActivity().getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()){
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);

        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }

        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                showInfo("connection failed: permission denied");
            else
                showInfo("connection failed: open failed");
            return;
        }

        try {
            usbSerialPort = driver.getPorts().get(0); // Most devices have just one port (port 0)
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(9600, 8, 1, UsbSerialPort.PARITY_NONE);
            usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
            Executors.newSingleThreadExecutor().submit(usbIoManager);

            showInfo("connected");
            connected = true;
        } catch (Exception e) {
            showInfo("connection failed: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        if(usbIoManager != null) {
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            if(usbSerialPort != null) {
                usbSerialPort.close();
            }
        } catch (IOException ignored) {

        }
        usbSerialPort = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disconnect();
    }

    public void showInfo(final String message){
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayTextView.append(message);
            }
        });
    }

    @Override
    public void onNewData(byte[] data) {
        String newData = new String(data);
        showInfo(newData);
        addToChart(newData);
    }

    private void addToChart(String message){
        strBuilder.append(message);
        consumeFirstDataIfReady();
    }

    private void consumeFirstDataIfReady() {
        String lastString = strBuilder.toString();
        if(lastString.contains(")")){
            int startIndex = lastString.indexOf("(");
            int endIndex = lastString.indexOf(")", startIndex);
            Log.d("arduino", "check : "+lastString);
            if(startIndex < endIndex) {
                String eggString = lastString.substring(startIndex, endIndex+1);
                Log.d("arduino", "new egg : " + eggString);
                String pureEggString = eggString.trim().replace("(", "").replace(")", "");
                String[] splittedPureEgg = pureEggString.split(",");
                if (splittedPureEgg.length == 2) {
                    float newValue = Float.parseFloat(splittedPureEgg[1]);
                    Log.d("arduino", "new value : " + newValue);
                    addEntry(newValue);
                    strBuilder.delete(startIndex, endIndex+1);
                    if(startIndex > 0)
                        strBuilder.delete(0, startIndex);
                }

            }
        }
    }

    @Override
    public void onRunError(Exception e) {
        showInfo(e.getMessage());
    }


}