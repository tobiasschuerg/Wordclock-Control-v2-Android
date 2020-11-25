package com.tobiasschuerg.wordclock.ui

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tobiasschuerg.wordclock.BroadcastReceiverX
import com.tobiasschuerg.wordclock.databinding.FragmentConnectBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import timber.log.Timber


/**
 * [Fragment] for helping connect to a word clock.
 */
class ConnectFragment : Fragment() {

    private var binding: FragmentConnectBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentConnectBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding!!.buttonConnect.setOnClickListener {
            GlobalScope.launch {
                connect()
            }
        }
    }

    var x = 1

    private suspend fun connect() {
        val device = scan()
                .onEach { Timber.i("Wordclock found: ${it.name}") }
                .firstOrNull()
        if (device != null) {
            withContext(Dispatchers.Main) {
                findNavController().navigate(ConnectFragmentDirections.connectFragmentToConfigureFragment(device))
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No wordclock found", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun scan() = channelFlow<BluetoothDevice> {
        val bluetoothManager = context!!.getSystemService(Service.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = bluetoothManager.adapter
        mBluetoothAdapter.startDiscovery()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val broadcastReceiverX = BroadcastReceiverX(channel)
        activity?.registerReceiver(broadcastReceiverX, filter)

        delay(5000)
        activity?.unregisterReceiver(broadcastReceiverX)
        mBluetoothAdapter.cancelDiscovery()
        Timber.i("Receiver unregistered")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}