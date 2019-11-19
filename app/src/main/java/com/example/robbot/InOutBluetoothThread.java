package com.example.robbot;

import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.os.Handler;

public class InOutBluetoothThread extends Thread {
    private BluetoothSocket socketToRobbo;
    private InputStream in = null;
    private OutputStream out = null;
    private Handler handler;

    InOutBluetoothThread(BluetoothSocket socket, Handler h){
        socketToRobbo = socket;
        handler = h;
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        byte[] buffer = new byte[1024];
        int bytes;

        while (true){
            try {
                bytes = in.read(buffer);
                handler.sendMessage(handler.obtainMessage(1, bytes, -1, buffer));
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    void cancel(){
        try{
            socketToRobbo.close();
            if(in!=null)in.close();
            if(out!=null)out.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    void write(byte[] bytes){
        try {
            out.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
