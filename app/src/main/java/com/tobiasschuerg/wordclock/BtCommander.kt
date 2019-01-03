package com.tobiasschuerg.wordclock

import android.bluetooth.BluetoothSocket
import android.graphics.Color
import androidx.annotation.ColorInt
import android.util.Log
import com.github.ivbaranov.rxbluetooth.BluetoothConnection
import com.tobiasschuerg.wordclock.values.ClockValue
import rx.Subscription
import java.io.IOException
import java.util.*

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

class BtCommander(private val socket: BluetoothSocket)

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
{

    private var connection: BluetoothConnection? = null
    private val subscription: Subscription? = null

    val backgroundColor: Boolean
        get() {
            val out = byteArrayOf('G'.toByte(), 'B'.toByte(), 0, 0)
            return getConnection().send(out)
        }

    val name: String
        get() = socket.remoteDevice.name

    fun close() {
        connection!!.closeConnection()
        subscription!!.unsubscribe()
    }

    private fun getConnection(): BluetoothConnection {
        if (connection == null || !socket.isConnected) {
            if (!socket.isConnected) {
                try {
                    socket.connect()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            try {
                connection = BluetoothConnection(socket)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        return connection!!
    }

    fun set(clockValue: ClockValue) {
        getConnection().send(clockValue.toByte())
    }

    fun setForegroundColor(@ColorInt color: Int): Boolean {
        val out = byteArrayOf('F'.toByte(), 0, 0, 0)
        out[1] = Color.red(color).toByte()
        out[2] = Color.green(color).toByte()
        out[3] = Color.blue(color).toByte()
        return getConnection().send(out)
    }

    fun updateTime() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)

        Log.d("time", hour.toString() + ":" + minute + ":" + second)
        val timeBytes = byteArrayOf('T'.toByte(), hour.toByte(), minute.toByte(), second.toByte())
        getConnection().send(timeBytes)

        val day = c.get(Calendar.DAY_OF_MONTH)
        val month = c.get(Calendar.MONTH) + 1 // month index is from 0 (jan) to 11 (dec)
        val year = c.get(Calendar.YEAR) - 2000

        Log.d("date", day.toString() + "-" + month + "-" + year)
        val dateBytes = byteArrayOf('D'.toByte(), day.toByte(), month.toByte(), year.toByte())
        getConnection().send(dateBytes)
    }
}
