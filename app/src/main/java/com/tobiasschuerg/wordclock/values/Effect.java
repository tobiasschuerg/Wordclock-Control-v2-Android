package com.tobiasschuerg.wordclock.values;

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

public class Effect implements ClockValue {

    private final boolean enabled;

    public Effect(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public byte[] toByte() {
        final byte esIst = (byte) (enabled ? 1 : 0);
        return new byte[]{'E', 0, esIst,  0};
    }
}
