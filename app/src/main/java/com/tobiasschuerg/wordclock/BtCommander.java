package com.tobiasschuerg.wordclock;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.ivbaranov.rxbluetooth.BluetoothConnection;

import rx.Subscriber;
import rx.Subscription;

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

public class BtCommander {

    private final BluetoothConnection connection;
    private final Subscription        subscription;

    public BtCommander(@NonNull BluetoothConnection bluetoothConnection) {
        this.connection = bluetoothConnection;

        subscription = connection.observeByteStream().subscribe(new Subscriber<Byte>() {
            @Override
            public void onCompleted() {
                Log.d("Bt", "completed");
            }

            @Override
            public void onError(Throwable e) {
                Log.d("Bt", "error");
                e.printStackTrace();
            }

            @Override
            public void onNext(Byte aByte) {
                Log.d("Bt", "byte: " + aByte);
            }
        });
    }

    public void close() {
        connection.closeConnection();
        subscription.unsubscribe();
    }

    public boolean setBackgroundColor(@ColorInt int color) {
        byte[] out = {'B', 0, 0, 0};
        out[1] = (byte) Color.red(color);
        out[2] = (byte) Color.green(color);
        out[3] = (byte) Color.blue(color);
        return connection.send(out);
    }

    public boolean setForegroundColor(@ColorInt int color) {
        byte[] out = {'F', 0, 0, 0};
        out[1] = (byte) Color.red(color);
        out[2] = (byte) Color.green(color);
        out[3] = (byte) Color.blue(color);
        return connection.send(out);
    }
}
