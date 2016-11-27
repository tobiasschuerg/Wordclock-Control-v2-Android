package com.tobiasschuerg.wordclock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.ivbaranov.rxbluetooth.BluetoothConnection;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.jakewharton.rxbinding.view.RxView;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.operators.OnSubscribeTimerPeriodically;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE   = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    @Nullable private Subscription connectSubscription;
    private           Subscription timer;
    private           Subscription stateObservable;

    // private RxBleClient rxBleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //        rxBleClient = RxBleClient.create(this);
        //        RxBleClient.setLogLevel(RxBleLog.VERBOSE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //        // polidea
        //        RxView.clicks(findViewById(R.id.btn_connnect_bluetooth))
        //                .subscribe(new Action1<Void>() {
        //                    @Override
        //                    public void call(Void aVoid) {
        //                        Set<RxBleDevice> devices = rxBleClient.getBondedDevices();
        //                        log("There are " + devices.size() + " devices.");
        //
        //                        for (RxBleDevice device : devices) {
        //                            if (device.getName().contains("Wordclock")) {
        //                                connectTo(device);
        //                            }
        //                        }
        //                    }
        //                });

        RxView.clicks(findViewById(R.id.btn_connnect_bluetooth))
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        doBluetoothStuff();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

    }

    private void doBluetoothStuff() {
        log("doing bluetooth ...");
        RxBluetooth rxb = new RxBluetooth(getApplicationContext());
        if (!rxb.isBluetoothAvailable()) {
            log("enabling bt");
            // to enable blutooth via startActivityForResult()
            rxb.enableBluetooth(this, 110);
        } else {
            log("observing bt");
            Set<BluetoothDevice> devices = rxb.getBondedDevices();
            final BluetoothDevice device = devices.iterator().next();
            log("Going to connect to " + device);
            connectSubscription = rxb.observeConnectDevice(device, MY_UUID_SECURE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .map(new Func1<BluetoothSocket, BluetoothConnection>() {
                        @Override
                        public BluetoothConnection call(BluetoothSocket bluetoothSocket) {
                            BluetoothConnection btc;
                            try {
                                btc = new BluetoothConnection(bluetoothSocket);
                            } catch (Exception e) {
                                throw new Error("Creating bluetooth connection failed.", e);
                            }
                            return btc;
                        }
                    })
                    .subscribe(new Action1<BluetoothConnection>() {
                        @Override
                        public void call(BluetoothConnection bluetoothConnection) {
                            BtCommander btc = new BtCommander(bluetoothConnection);
                            onConnected(btc);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            log("ERROR");
                            throwable.printStackTrace();
                        }
                    });

            stateObservable = rxb.observeBluetoothState()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer state) {
                            switch (state) {
                                case BluetoothAdapter.STATE_OFF:
                                    log("off");
                                    break;
                                case BluetoothAdapter.STATE_TURNING_ON:
                                    log("turning on");
                                    break;
                                case BluetoothAdapter.STATE_ON:
                                    log("on");
                                    break;
                                case BluetoothAdapter.STATE_TURNING_OFF:
                                    log("turning off");
                                    break;
                                default:
                                    log("default: " + state);
                            }
                        }
                    });
        }
    }

    private void onConnected(final BtCommander btc) {
        log("connected");
        final Random r = new Random();

        btc.setBackgroundColor(Color.RED);
        btc.setForegroundColor(Color.BLUE);

        timer = Observable.create(new OnSubscribeTimerPeriodically(5, 10, TimeUnit.SECONDS, Schedulers.io()))
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {
                        log("completed");
                        btc.close();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log("ERROR");
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Long aLong) {
                        log("update " + aLong);
                        boolean a = btc.setBackgroundColor(r.nextInt());
                        boolean b = btc.setForegroundColor(r.nextInt());
                        if (!(a && b)) {
                            onCompleted();
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        if (connectSubscription != null) {
            connectSubscription.unsubscribe();
        }
        if (timer != null) {
            timer.unsubscribe();
        }
        if (stateObservable != null) {
            stateObservable.unsubscribe();
        }
        super.onStop();

    }


    //    private void connectTo(final RxBleDevice device) {
    //
    //        final Subscription state = device.observeConnectionStateChanges().subscribe(new Subscriber<RxBleConnection.RxBleConnectionState>() {
    //            @Override
    //            public void onCompleted() {
    //
    //            }
    //
    //            @Override
    //            public void onError(Throwable e) {
    //
    //            }
    //
    //            @Override
    //            public void onNext(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
    //                log(rxBleConnectionState.toString());
    //            }
    //        });
    //
    //        connectSubscription = device.establishConnection(this, false)
    //                .subscribeOn(Schedulers.newThread())
    //                //                 .observeOn(AndroidSchedulers.mainThread())
    //                .map(new Func1<RxBleConnection, RxBleConnection>() {
    //                    @Override
    //                    public RxBleConnection call(RxBleConnection rxBleConnection) {
    //                        return rxBleConnection;
    //                    }
    //                })
    //                .subscribe(new Subscriber<RxBleConnection>() {
    //                    @Override
    //                    public void onCompleted() {
    //                        log("completed");
    //
    //                    }
    //
    //                    @Override
    //                    public void onError(Throwable e) {
    //                        log("ERROR");
    //                        e.printStackTrace();
    //                        connectSubscription.unsubscribe();
    //                        state.unsubscribe();
    //                        // connectTo(device);
    //                    }
    //
    //                    @Override
    //                    public void onNext(RxBleConnection rxBleConnection) {
    //                        log("connected to " + rxBleConnection.toString());
    //                    }
    //                });
    //    }

    private void log(String message) {
        Log.d("Main", message);
    }

}
