package com.tobiasschuerg.wordclock;

import android.graphics.Color;
import android.support.annotation.ColorInt;

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

public class BackgroundColor implements ClockValue {

    private final int color;

    public BackgroundColor(@ColorInt int color) {
        this.color = color;
    }

    @Override
    public byte[] toByte() {
        byte[] out = {'B', 0, 0, 0};
        out[1] = (byte) Color.red(color);
        out[2] = (byte) Color.green(color);
        out[3] = (byte) Color.blue(color);
        return out;
    }
}
