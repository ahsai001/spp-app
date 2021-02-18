package com.ahsailabs.sppapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class SecondFragment extends Fragment {
    BluetoothSPP bt;
    private TextView tvMessage;
    private String clientName;
    private TextInputEditText etMessage = null;

    private DatabaseReference firebaseDatabase;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String info = etMessage.getText().toString();
                bt.send(info, true);

                etMessage.setText("");
                showInfo("Me : "+info);
            }
        });

        view.findViewById(R.id.btnSelectDevices).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bt.getServiceState() == BluetoothState.STATE_CONNECTED){
                    bt.disconnect();
                } else {
                    bt.startDiscovery();
                    Intent intent = new Intent(view.getContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                }
            }
        });

        tvMessage = view.findViewById(R.id.tvMessage);
        TextInputEditText etView = view.findViewById(R.id.etMessage);;
        etMessage = etView;

        view.findViewById(R.id.btnGetList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get list from collection firestore
                firebaseDatabase.child("spplogs")
                        .child("sensor1")
                        .child("data")
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                            @Override
                            public void onSuccess(DataSnapshot dataSnapshot) {
                                for (DataSnapshot document : dataSnapshot.getChildren()){
                                    Map<String, Object> object = (Map<String, Object>) document.getValue();
                                    String data = "data : " + object.get("value");
                                    showInfo(data);
                                }
                            }
                        });
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.first_fragment_label));

        firebaseDatabase = FirebaseDatabase.getInstance().getReference();

        bt = new BluetoothSPP(getActivity());
        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                clientName = name+"("+address+")";
                showInfo("onDeviceConnected "+clientName);
            }

            @Override
            public void onDeviceDisconnected() {
                showInfo("onDeviceDisconnected");
            }

            @Override
            public void onDeviceConnectionFailed() {
                showInfo("onDeviceConnectionFailed");
            }
        });

        bt.setBluetoothStateListener(new BluetoothSPP.BluetoothStateListener() {
            @Override
            public void onServiceStateChanged(int state) {
                if(state == BluetoothState.STATE_CONNECTED) {
                    // Do something when successfully connected
                    showInfo("STATE_CONNECTED");
                } else if(state == BluetoothState.STATE_CONNECTING) {
                    // Do something while connecting
                    showInfo("STATE_CONNECTING");
                } else if(state == BluetoothState.STATE_LISTEN) {
                    // Do something when device is waiting for connection
                    showInfo("STATE_LISTEN");
                } else if(state == BluetoothState.STATE_NONE) {
                    // Do something when device don't have any connection
                    showInfo("STATE_NONE");
                }
            }
        });

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            @Override
            public void onDataReceived(byte[] data, String message) {
                showInfo(clientName+" : "+message);
                save(message);
            }
        });

        if(!bt.isBluetoothAvailable()){
            //TODO : bluetooth is not available
            showInfo("bluetooth is not available");
        } else {
            if(!bt.isBluetoothEnabled()){
                //TODO : request enable bluetooth
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
            } else {
                if(!bt.isServiceAvailable()){
                    setupAndStartService();
                }
            }
        }
    }

    private void save(String message) {
        Map<String, Object> newValue = new HashMap<>();
        newValue.put("value", message);
        newValue.put("client", clientName);
        newValue.put("year", 2021);
        newValue.put("date", new Date());

        firebaseDatabase.child("spplogs")
                .child("sensor1")
                .child("data")
                .push()
                .setValue(newValue)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("SPP", "New saved data id : "+ aVoid);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("SPP", "Error adding new data", e);
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BluetoothState.REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                setupAndStartService();
            } else {
                showInfo("bluetooth is not enabled, please enable it");
            }
        } else if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE){
            if(resultCode == Activity.RESULT_OK){
                bt.connect(data);
            } else {
                showInfo("ada masalah");
            }
        }
    }

    private void setupAndStartService() {
        //TODO : what we want
        bt.setupService();
        bt.startService(BluetoothState.DEVICE_ANDROID);
    }

    private void showInfo(String info) {
        //TODO : what we want
        tvMessage.append("\n"+info);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bt.stopService();
    }
}