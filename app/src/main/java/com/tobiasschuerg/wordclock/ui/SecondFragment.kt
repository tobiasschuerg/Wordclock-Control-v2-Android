package com.tobiasschuerg.wordclock.ui

import android.bluetooth.BluetoothSocket
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.tobiasschuerg.wordclock.BtCommander
import com.tobiasschuerg.wordclock.Config
import com.tobiasschuerg.wordclock.databinding.ActivityMainOldBinding
import com.tobiasschuerg.wordclock.values.BackgroundColor
import com.tobiasschuerg.wordclock.values.ClockValue
import com.tobiasschuerg.wordclock.values.Effect
import com.tobiasschuerg.wordclock.values.ForegroundColor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private val args: SecondFragmentArgs by navArgs()
    private val device by lazy { args.device }
    private val bluetoothSocket: BluetoothSocket by lazy {
        device.createRfcommSocketToServiceRecord(Config.MY_UUID_SECURE)
    }

    private var _binding: ActivityMainOldBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var commander: BtCommander

    private val effectChannel = ConflatedBroadcastChannel<ClockValue>(Effect(1, true))

    private var foregroundColor: Int = Color.BLUE
    private var backgroudnColor: Int = Color.YELLOW
    private var effect: Int = 1

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = ActivityMainOldBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toggleEsIst.setOnCheckedChangeListener { _, isChecked ->
            effectChannel.offer(Effect(esIstEnabled = isChecked, effect = effect))
        }
        binding.buttonEffect.setOnClickListener {
            val value = effectChannel.value
            effect = (effect + 1) % Config.NUMBER_OF_EFFECTS
            effectChannel.offer(Effect(esIstEnabled = binding.toggleEsIst.isChecked, effect = effect))
        }
        binding.buttonPrimary.setOnClickListener { pickPrimaryColor() }
        binding.buttonSecondary.setOnClickListener { pickSecondaryColor() }
    }


    lateinit var job: Job

    override fun onResume() {
        super.onResume()

        job = GlobalScope.launch {
            effectChannel.asFlow()
                    .collect {
                        with(bluetoothSocket) {
                            val commander = BtCommander(this)
                            commander.updateTime()
                            commander.set(it)
                            Timber.d("Updated")
                        }
                    }
        }
    }

    override fun onPause() {
        super.onPause()
        job.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun pickPrimaryColor() {
        ColorPickerDialogBuilder
                .with(context)
                .setTitle("Choose color")
                .initialColor(Color.BLUE)
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
                    foregroundColor = Color.HSVToColor(hsv)

                    effectChannel.offer(ForegroundColor(foregroundColor))
                    binding.buttonPrimary.setBackgroundColor(foregroundColor)
                }
                .build()
                .show()
    }

    private fun pickSecondaryColor() {
        ColorPickerDialogBuilder
                .with(context)
                .setTitle("Secondary Color")
                .initialColor(Color.GREEN)
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
                    backgroudnColor = selectedColor
                    effectChannel.offer(BackgroundColor(backgroudnColor))
                    binding.buttonSecondary.setBackgroundColor(backgroudnColor)
                }
                .build()
                .show()
    }
}