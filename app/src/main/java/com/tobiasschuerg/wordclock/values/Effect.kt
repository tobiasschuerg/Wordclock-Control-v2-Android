package com.tobiasschuerg.wordclock.values

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

class Effect(
        private val effect: Int,
        private val esIstEnabled: Boolean
) : ClockValue {

    override fun toByte(): ByteArray {
        val esIst = (if (esIstEnabled) 1 else 0).toByte()
        return byteArrayOf(
                'E'.toByte(),
                effect.toByte(),
                esIst,
                0
        )
    }
}
