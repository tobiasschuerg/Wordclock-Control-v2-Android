package com.tobiasschuerg.wordclock;

import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.ivbaranov.rxbluetooth.BluetoothConnection;

import java.io.IOException;
import java.util.Calendar;

import rx.Subscription;

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

public class BtCommander {

    @Nullable private BluetoothConnection connection;

    final private BluetoothSocket socket;
    private       Subscription    subscription;

    public BtCommander(@NonNull BluetoothSocket socket) {
        this.socket = socket;

        //        subscription = connection.observeByteStream()
        //                .subscribe(new Subscriber<Byte>() {
        //                    @Override
        //                    public void onCompleted() {
        //                        Log.d("Bt", "completed");
        //                    }
        //
        //                    @Override
        //                    public void onError(Throwable e) {
        //                        Log.d("Bt", "error");
        //                        e.printStackTrace();
        //                    }
        //
        //                    @Override
        //                    public void onNext(Byte aByte) {
        //                        Log.d("Bt", "byte: " + aByte);
        //                    }
        //                });
    }

    public void close() {
        connection.closeConnection();
        subscription.unsubscribe();
    }

    public boolean getBackgroundColor() {
        byte[] out = {'G', 'B', 0, 0};
        return getConnection().send(out);
    }

    @NonNull
    private BluetoothConnection getConnection() {
        if (connection == null || !socket.isConnected()) {
            if (!socket.isConnected()) {
                try {
                    socket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                connection = new BluetoothConnection(socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public String getName() {
        return socket.getRemoteDevice().getName();
    }

    public void set(ClockValue clockValue) {
        getConnection().send(clockValue.toByte());
    }

    public boolean setForegroundColor(@ColorInt int color) {
        byte[] out = {'F', 0, 0, 0};
        out[1] = (byte) Color.red(color);
        out[2] = (byte) Color.green(color);
        out[3] = (byte) Color.blue(color);
        return getConnection().send(out);
    }

    void updateTime() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        Log.d("time", hour + ":" + minute + ":" + second);
        byte[] timeBytes = {'T', (byte) hour, (byte) minute, (byte) second};
        getConnection().send(timeBytes);

        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH) + 1; // month index is from 0 (jan) to 11 (dec)
        int year = c.get(Calendar.YEAR) - 2000;

        Log.d("date", day + "-" + month + "-" + year);
        byte[] dateBytes = {'D', (byte) day, (byte) month, (byte) year};
        getConnection().send(dateBytes);
    }
}
