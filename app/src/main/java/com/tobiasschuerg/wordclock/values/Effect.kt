package com.tobiasschuerg.wordclock.values

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

data class Effect(
        val effect: Int,
        val esIstEnabled: Boolean
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
