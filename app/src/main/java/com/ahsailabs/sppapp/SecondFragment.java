package com.ahsailabs.sppapp;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

public class SecondFragment extends Fragment implements ArduinoListener {
    private Arduino arduino;

    private TextView displayTextView;
    private EditText editText;
    private Button sendBtn;

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
                arduino.send(editTextString.getBytes());
                editText.getText().clear();
            }
        });

        arduino = new Arduino(requireContext());
        arduino.setArduinoListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        arduino.unsetArduinoListener();
        arduino.close();
    }

    private void showInfo(final String message){
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayTextView.append(message);
            }
        });
    }

    @Override
    public void onArduinoAttached(UsbDevice device) {
        showInfo("device attached...");
        arduino.open(device);
    }

    @Override
    public void onArduinoDetached() {
        showInfo("device detached.");
    }

    @Override
    public void onArduinoMessage(byte[] bytes) {
        showInfo(new String(bytes));
    }

    @Override
    public void onArduinoOpened() {
        String str = "device opened...";
        arduino.send(str.getBytes());
    }
}