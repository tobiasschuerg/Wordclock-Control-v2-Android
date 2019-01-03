package com.tobiasschuerg.wordclock.values

import android.graphics.Color
import android.support.annotation.ColorInt

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

class BackgroundColor(@param:ColorInt private val color: Int) : ClockValue {

    override fun toByte(): ByteArray = byteArrayOf(
            'B'.toByte(),
            Color.red(color).toByte(),
            Color.green(color).toByte(),
            Color.blue(color).toByte()
    )

}
