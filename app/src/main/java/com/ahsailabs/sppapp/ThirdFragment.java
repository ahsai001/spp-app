package com.ahsailabs.sppapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

public class ThirdFragment extends Fragment implements SerialInputOutputManager.Listener{
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

    public ThirdFragment() {
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
        return inflater.inflate(R.layout.fragment_second, container, false);
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

        connect();

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
        showInfo(new String(data));
    }

    @Override
    public void onRunError(Exception e) {
        showInfo(e.getMessage());
    }


}