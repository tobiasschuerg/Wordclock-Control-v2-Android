package com.tobiasschuerg.wordclock

import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.github.ivbaranov.rxbluetooth.RxBluetooth
import com.tobiasschuerg.wordclock.values.BackgroundColor
import com.tobiasschuerg.wordclock.values.ClockValue
import com.tobiasschuerg.wordclock.values.Effect
import com.tobiasschuerg.wordclock.values.ForegroundColor
import kotlinx.android.synthetic.main.activity_main.*
import rx.Subscriber
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.ReplaySubject
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var connectSubscription: Subscription? = null

    private var updateSubscription: Subscription? = null
    private val stateObservable: Subscription? = null
    private val updateSubject = ReplaySubject.create<ClockValue>()

    private var backgroundColor = Color.BLUE
    private var primaryColor = Color.RED
    private var effect = 0

    // private RxBleClient rxBleClient;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progress.visibility = View.VISIBLE
        progress.isIndeterminate = true
        connectWordClock()

        button_primary.setOnClickListener { pickPrimaryColor() }
        button_secondary.setOnClickListener { pickSecondaryColor() }
        button_effect.setOnClickListener { toggleEffect() }
        toggle_es_ist.setOnCheckedChangeListener { _, isChecked -> updateSubject.onNext(Effect(effect, isChecked)) }
    }

    override fun onStop() {
        if (connectSubscription != null) {
            connectSubscription!!.unsubscribe()
        }
        if (updateSubscription != null) {
            updateSubscription!!.unsubscribe()
        }
        stateObservable?.unsubscribe()
        super.onStop()

    }

    private fun connectWordClock() {
        log("going to connect with a word clock")
        val rxb = RxBluetooth(applicationContext)
        if (!rxb.isBluetoothAvailable) {
            log("no bluetooth")
            // to enable bluetooth via startActivityForResult()
            rxb.enableBluetooth(this, 0)
        } else {
            rxb.cancelDiscovery()

            val devices = rxb.bondedDevices
            var device: BluetoothDevice? = null
            // TODO: ask user for a specific device
            for (btd in devices) {
                log("bluetooth device found: " + btd.name)
                if (btd.name.toLowerCase().contains("wordclock")) {
                    device = btd
                }
            }
            if (device == null) {
                Toast.makeText(this, "No word clock bluetooth signal found!", Toast.LENGTH_SHORT).show()
                return
            }
            Toast.makeText(this, "Connecting to " + device.name, Toast.LENGTH_SHORT).show()
            log("Going to connect to " + device.address + " - " + device.name)

            connectSubscription = rxb
                    .observeConnectDevice(device, Config.MY_UUID_SECURE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .doOnError { Toast.makeText(this@MainActivity, "Connecting failed. Will Retry.", Toast.LENGTH_SHORT).show() }
                    .retry(3)
                    .map { bluetoothSocket -> BtCommander(bluetoothSocket) }
                    .subscribe({ commander ->
                        setMessage("Connected to " + commander.name)
                        progress.visibility = View.GONE
                        onConnected(commander)
                    }, { throwable ->
                        log("Could not connect to word clock")
                        setMessage(throwable.toString())
                        throwable.printStackTrace()
                        log("stopping")
                        val rotateDrawable = progress.indeterminateDrawable as AnimatedVectorDrawable
                        rotateDrawable.stop()
                    })
        }
    }

    private fun log(message: String) {
        Timber.d(message)
    }

    private fun setMessage(message: String) {
        val actionbar = supportActionBar
        actionbar!!.subtitle = message
    }

    private fun onConnected(btc: BtCommander) {
        log("connected")
        btc.updateTime()

        updateSubscription = updateSubject
                .observeOn(Schedulers.io())
                .subscribe(object : Subscriber<ClockValue>() {
                    override fun onCompleted() {
                        log("completed")
                        btc.close()
                    }

                    override fun onError(e: Throwable) {
                        log("ERROR")
                        e.printStackTrace()
                    }

                    override fun onNext(clockValue: ClockValue) {
                        log("update " + clockValue.toString())
                        btc.set(clockValue)
                    }
                })

    }

    private fun pickPrimaryColor() {
        ColorPickerDialogBuilder
                .with(this@MainActivity)
                .setTitle("Choose color")
                .initialColor(primaryColor)
                .showAlphaSlider(false)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(7)
                .setPositiveButton(android.R.string.ok) { _, color, _ ->
                    var selectedColor = color
                    val hsv = FloatArray(3)
                    Color.colorToHSV(selectedColor, hsv)
                    if (hsv[2] > 0.8) {
                        hsv[2] = 0.8f
                    }
                    selectedColor = Color.HSVToColor(hsv)
                    primaryColor = selectedColor
                    updateSubject.onNext(ForegroundColor(selectedColor))
                    button_primary.setBackgroundColor(selectedColor)
                }
                .build()
                .show()
    }

    private fun pickSecondaryColor() {
        ColorPickerDialogBuilder
                .with(this@MainActivity)
                .setTitle("Secondary Color")
                .initialColor(backgroundColor)
                .showAlphaSlider(false)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(7)
                .setPositiveButton(android.R.string.ok) { _, color, _ ->
                    var selectedColor = color
                    val hsv = FloatArray(3)
                    Color.colorToHSV(selectedColor, hsv)
                    if (hsv[2] > 0.7) {
                        hsv[2] = 0.7f
                    }
                    selectedColor = Color.HSVToColor(hsv)
                    backgroundColor = selectedColor
                    updateSubject.onNext(BackgroundColor(selectedColor))
                    button_secondary.setBackgroundColor(backgroundColor)
                }
                .build()
                .show()
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

    private fun toggleEffect() {
        effect++
        effect %= Config.NUMBER_OF_EFFECTS
        button_effect.text = "Effect $effect"
        updateSubject.onNext(Effect(effect, toggle_es_ist.isChecked))
    }

}
