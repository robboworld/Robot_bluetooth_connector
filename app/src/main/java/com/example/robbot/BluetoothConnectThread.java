package com.example.robbot;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BluetoothConnectThread extends Thread {
    private BluetoothSocket socketToRobbo = null;
    private Handler handler;
    InOutBluetoothThread inOutBluetoothThread = null;

    BluetoothConnectThread(BluetoothDevice device, Handler h){
        handler = h;
        try {
            Method method = device.getClass().getMethod("createRfcommSocket", int.class);
            socketToRobbo = (BluetoothSocket) method.invoke(device, 1);
        } catch (NoSuchMethodException e){
            e.printStackTrace();
        }catch (InvocationTargetException e){
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            socketToRobbo.connect();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socketToRobbo.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }
        inOutBluetoothThread = new InOutBluetoothThread(socketToRobbo, handler);
        inOutBluetoothThread.start();
    }

    void cancel(){
        try {
            socketToRobbo.close();
            if(inOutBluetoothThread!=null) inOutBluetoothThread.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
