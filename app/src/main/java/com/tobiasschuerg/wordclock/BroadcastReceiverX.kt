package com.tobiasschuerg.wordclock

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.channels.SendChannel
import timber.log.Timber


class BroadcastReceiverX(private val channel: SendChannel<BluetoothDevice>) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Timber.v("Received broadcast")
        if (BluetoothDevice.ACTION_FOUND == intent?.action) {
            val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
            device.createRfcommSocketToServiceRecord(Config.MY_UUID_SECURE)

            // Add the name and address to an array adapter to show in a Toast
            val derp = device.name + " - " + device.address
            Timber.d("Bluetooth device found $derp")
            Toast.makeText(context, derp, Toast.LENGTH_LONG).show()

            if (device.name?.contains("wordclock", true) == true) {
                channel.offer(device)
            }
        }
    }
}