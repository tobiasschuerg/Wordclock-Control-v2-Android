package com.tobiasschuerg.wordclock;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;

import com.github.ivbaranov.rxbluetooth.RxBluetooth;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

public class MainActivity extends AppCompatActivity {

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE   = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    @BindView(R.id.progress) ProgressBar progressBar;

    @Nullable private Subscription connectSubscription;
    private           Subscription timer;
    private           Subscription stateObservable;

    // private RxBleClient rxBleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        progressBar.setIndeterminate(true);
        connectWordClock();
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    private void connectWordClock() {
        log("going to connect with word clock");
        RxBluetooth rxb = new RxBluetooth(getApplicationContext());
        if (!rxb.isBluetoothAvailable()) {
            log("no bluetooth");
            // to enable bluetooth via startActivityForResult()
            rxb.enableBluetooth(this, 0);
        } else {
            rxb.cancelDiscovery();

            Set<BluetoothDevice> devices = rxb.getBondedDevices();
            // TODO: ask user for device
            final BluetoothDevice device = devices.iterator().next();
            log("Going to connect to " + device);

            connectSubscription = rxb.observeConnectDevice(device, MY_UUID_SECURE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .first().toSingle()
                    .map(new Func1<BluetoothSocket, BtCommander>() {
                        @Override
                        public BtCommander call(BluetoothSocket bluetoothSocket) {
                            return new BtCommander(bluetoothSocket);
                        }
                    })
                    .subscribe(new Action1<BtCommander>() {
                        @Override
                        public void call(BtCommander commander) {
                            setMessage("Connected to " + commander.getName());
                            onConnected(commander);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            log("ERROR connecting");
                            setMessage(throwable.getMessage());
                            throwable.printStackTrace();
                        }
                    });
        }
    }

    private void setMessage(String message) {
        ActionBar actionbar = getSupportActionBar();
        actionbar.setSubtitle(message);
    }

    private void onConnected(final BtCommander btc) {
        log("connected");
        btc.updateTime();

        final Random r = new Random();

        Subject<ClockValue, ClockValue> s = BehaviorSubject.create();

        timer = s.subscribe(new Subscriber<ClockValue>() {
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
            public void onNext(ClockValue clockValue) {
                log("update " + clockValue.toString());
                btc.set(clockValue);
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
