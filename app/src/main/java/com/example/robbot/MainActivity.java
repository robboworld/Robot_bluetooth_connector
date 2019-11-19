package com.example.robbot;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Button find_robbot;
    private Button button_up;
    private Button button_down;
    private Button button_left;
    private Button button_right;
    private Button disconnect;

    ArrayAdapter<String> pairedDevicesArrayAdapter;

    BluetoothAdapter bluetoothAdapter;

    Double power = 0.0;

    Set<BluetoothDevice> pairedDevices;

    BluetoothConnectThread bluetoothConnectThread = null;
    EditText editText;

    private Handler handler;

    private final static int REQUEST_ENABLE_BT = 1;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                assert device != null;
                if(!pairedDevices.contains(device))
                pairedDevicesArrayAdapter.add(device.getName()+"\n"+device.getAddress());
            }
        }
    };

    private ListView mListViewPairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initButtonsAndOther();
        addListenersToButtons();
        initBluetooth();
        pairedDevicesArrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1);
        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        askForTurnBluetoothUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        if(bluetoothConnectThread!=null) bluetoothConnectThread.cancel();
    }

    @SuppressLint("HandlerLeak")
    private void initButtonsAndOther(){
        find_robbot = findViewById(R.id.find_robbot);
        disconnect = findViewById(R.id.disconnect);
        button_up = findViewById(R.id.up_button);
        button_down = findViewById(R.id.down_button);
        button_left = findViewById(R.id.left_button);
        button_right = findViewById(R.id.right_button);
        editText = findViewById(R.id.power);
        mListViewPairedDevices = findViewById(R.id.pairedlist);
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
//                if(msg.what == 1){
//                    byte[] buffer;
//                    buffer = (byte[]) msg.obj;
//                    Toast.makeText(MainActivity.this, new String(buffer), Toast.LENGTH_LONG).show();
//                }
            }
        };
        handler.sendEmptyMessage(0);
    }

    private void addListenersToButtons(){
        find_robbot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkBluetoothEnabled()) showPairedDevices();
                else {
                    Toast.makeText(MainActivity.this, "Turn bluetooth on!!!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bluetoothConnectThread!=null) bluetoothConnectThread.cancel();
            }
        });
        button_down.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onClick(View v) {
//                Toast.makeText(MainActivity.this, "Down", Toast.LENGTH_LONG).show();
                if(String.valueOf(editText.getText()).equals("")) return;
                power = Double.parseDouble(String.valueOf(editText.getText()))*0.01;
                if(power==0){
                    write("C 0 0");
                    return;
                }
                if(power<0 || power>1) return;
                power = power*63+63;
                Integer intPower = power.intValue();
                if(bluetoothConnectThread!=null) {
                    write(String.format("c %d %d", intPower, intPower));
                }
            }
        });
        button_up.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onClick(View v) {
                if(String.valueOf(editText.getText()).equals("")) return;
                power = Double.parseDouble(String.valueOf(editText.getText()))*0.01;
                if(power==0){
                    write("C 0 0");
                    return;
                }
                if(power<=0 || power>1) return;
                power = power*63;
                Integer intPower = power.intValue();
                if(bluetoothConnectThread!=null) {
                    write(String.format("c %d %d", intPower, intPower));
                }
            }
        });
        button_left.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onClick(View v) {
                if(String.valueOf(editText.getText()).equals("")) return;
                power = Double.parseDouble(String.valueOf(editText.getText()))*0.005;
                if(power==0){
                    write("C 0 0");
                    return;
                }
                if(power<0 || power>1) return;
                power = power*63+63;
                Integer intPowerLeft = power.intValue();
                power = power-63;
                Integer intPowerRight = power.intValue();
                if(bluetoothConnectThread!=null) {
                    write(String.format("c %d %d", intPowerLeft, intPowerRight));
                }
            }
        });
        button_right.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onClick(View v) {
                if(String.valueOf(editText.getText()).equals("")) return;
                power = Double.parseDouble(String.valueOf(editText.getText()))*0.01;
                if(power==0){
                    write("C 0 0");
                    return;
                }
                if(power<0 || power>1) return;
                power = power*63;
                Integer intPowerLeft = power.intValue();
                power = power+63;
                Integer intPowerRight = power.intValue();
                if(bluetoothConnectThread!=null) {
                    write(String.format("c %d %d", intPowerLeft, intPowerRight));
                }
            }
        });
    }

    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            Toast.makeText(MainActivity.this, "Your device don't support bluetooth, sorry", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @SuppressLint("HardwareIds")
    private void askForTurnBluetoothUp(){
        if(!bluetoothAdapter.isEnabled()){
            Toast.makeText(MainActivity.this, "Turn your bluetooth device on!", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(MainActivity.this, bluetoothAdapter.getAddress()+"; "+bluetoothAdapter.getState(), Toast.LENGTH_SHORT).show();
        }
    }
    private boolean checkBluetoothEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    private void write(String command){
        String[] symbols = command.split(" ");
        byte[] buffer = new byte[symbols.length+1];
        for(int i=0; i<symbols.length; i++){
            String s = symbols[i];
            try{
                buffer[i] = ((Integer) Integer.parseInt(s)).byteValue();
            } catch (NumberFormatException e){
                buffer[0] = s.getBytes()[0];
            }
        }
        buffer[buffer.length-1] = "$".getBytes()[0];
        if(bluetoothConnectThread!=null && bluetoothConnectThread.inOutBluetoothThread!=null)
        bluetoothConnectThread.inOutBluetoothThread.write(buffer);
    }

    private void showPairedDevices(){
        if(pairedDevicesArrayAdapter!=null && !pairedDevicesArrayAdapter.isEmpty()){
            pairedDevicesArrayAdapter.clear();
        }
        bluetoothAdapter.startDiscovery();
        Toast.makeText(MainActivity.this, "discovery started", Toast.LENGTH_SHORT).show();
        pairedDevices = bluetoothAdapter.getBondedDevices();
        for(BluetoothDevice bd: pairedDevices){
            pairedDevicesArrayAdapter.add(bd.getName()+";\n"+bd.getAddress());
        }
        mListViewPairedDevices.setAdapter(pairedDevicesArrayAdapter);
        mListViewPairedDevices.setVisibility(View.VISIBLE);
        mListViewPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                Toast.makeText(MainActivity.this, "discovery canceled", Toast.LENGTH_SHORT).show();
//                    mListViewPairedDevices.setVisibility(View.GONE);
                String robbotNameAndAddress = pairedDevicesArrayAdapter.getItem(position);
                BluetoothDevice deviceRobbot = bluetoothAdapter.getRemoteDevice(robbotNameAndAddress.substring(robbotNameAndAddress.length()-17));
                bluetoothConnectThread = new BluetoothConnectThread(deviceRobbot, handler);
                bluetoothConnectThread.start();
            }
        });
    }
}