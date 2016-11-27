package com.tobiasschuerg.wordclock;

import android.graphics.Color;
import android.support.annotation.ColorInt;

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

public class ForegroundColor implements ClockValue {

    private final int color;

    public ForegroundColor(@ColorInt int color) {
        this.color = color;
    }

    @Override
    public byte[] toByte() {
        byte[] out = {'F', 0, 0, 0};
        out[1] = (byte) Color.red(color);
        out[2] = (byte) Color.green(color);
        out[3] = (byte) Color.blue(color);
        return out;
    }
}
