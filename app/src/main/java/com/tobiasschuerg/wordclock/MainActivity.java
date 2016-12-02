package com.tobiasschuerg.wordclock;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.tobiasschuerg.wordclock.values.BackgroundColor;
import com.tobiasschuerg.wordclock.values.ClockValue;
import com.tobiasschuerg.wordclock.values.Effect;
import com.tobiasschuerg.wordclock.values.ForegroundColor;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

public class MainActivity extends AppCompatActivity {

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE   = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    @BindView(R.id.progress)          ProgressBar     progressBar;
    @BindView(R.id.btn_back_color)    AppCompatButton backgroundColorBtn;
    @BindView(R.id.btn_primary_color) AppCompatButton primaryColorBtn;
    @BindView(R.id.es_ist)            CheckBox        checkBox;

    @Nullable private Subscription connectSubscription;

    private Subscription timer;
    private Subscription stateObservable;
    private ReplaySubject<ClockValue> updateSubject = ReplaySubject.create();

    private int backgroundColor = Color.BLUE;
    private int primaryColor    = Color.RED;

    // private RxBleClient rxBleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        connectWordClock();
    }

    @OnCheckedChanged(R.id.es_ist)
    void esIst(boolean checked) {
        updateSubject.onNext(new Effect(checked));
    }

    @OnClick(R.id.btn_primary_color)
    void pickPrimaryColor() {
        ColorPickerDialogBuilder
                .with(MainActivity.this)
                .setTitle("Choose color")
                .initialColor(primaryColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(6)
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        float[] hsv = new float[3];
                        Color.colorToHSV(selectedColor, hsv);
                        if (hsv[2] > 0.8) {
                            hsv[2] = 0.8f;
                        }
                        selectedColor = Color.HSVToColor(hsv);
                        primaryColor = selectedColor;
                        updateSubject.onNext(new ForegroundColor(selectedColor));
                        primaryColorBtn.setBackgroundColor(selectedColor);
                    }
                })
                .build()
                .show();
    }

    @OnClick(R.id.btn_back_color)
    void pickSecondaryColor() {
        ColorPickerDialogBuilder
                .with(MainActivity.this)
                .setTitle("Secondary Color")
                .initialColor(backgroundColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(6)
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        float[] hsv = new float[3];
                        Color.colorToHSV(selectedColor, hsv);
                        if (hsv[2] > 0.7) {
                            hsv[2] = 0.7f;
                        }
                        selectedColor = Color.HSVToColor(hsv);
                        backgroundColor = selectedColor;
                        updateSubject.onNext(new BackgroundColor(selectedColor));
                        backgroundColorBtn.setBackgroundColor(backgroundColor);
                    }
                })
                .build()
                .show();
    }

    private void connectWordClock() {
        log("going to connect with a word clock");
        RxBluetooth rxb = new RxBluetooth(getApplicationContext());
        if (!rxb.isBluetoothAvailable()) {
            log("no bluetooth");
            // to enable bluetooth via startActivityForResult()
            rxb.enableBluetooth(this, 0);
        } else {
            rxb.cancelDiscovery();

            Set<BluetoothDevice> devices = rxb.getBondedDevices();
            BluetoothDevice device = null;
            // TODO: ask user for a specific device
            for (BluetoothDevice btd : devices) {
                log("bluetooth device found: " + btd.getName());
                if (btd.getName().toLowerCase().contains("wordclock")) {
                    device = btd;
                }
            }
            if (device == null) {
                Toast.makeText(this, "No word clock bluetooth signal found!", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
            log("Going to connect to " + device.getAddress() + " - " + device.getName());

            connectSubscription = rxb
                    .observeConnectDevice(device, MY_UUID_SECURE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .retry(3)
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
                            progressBar.setVisibility(View.GONE);
                            onConnected(commander);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            log("Could not connect to word clock");
                            setMessage(throwable.getMessage());
                            throwable.printStackTrace();
                            log("stopping");
                            AnimatedVectorDrawable rotateDrawable = (AnimatedVectorDrawable) progressBar.getIndeterminateDrawable();
                            rotateDrawable.stop();
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

        timer = updateSubject.subscribe(new Subscriber<ClockValue>() {
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
