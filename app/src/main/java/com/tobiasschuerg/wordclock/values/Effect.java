package com.tobiasschuerg.wordclock.values;

/**
 * Created by Tobias Schürg on 27.11.2016.
 */

public class Effect implements ClockValue {

    private final boolean esIstEnabled;
    private final int     effect;

    public Effect(int effect, boolean esIstEnabled) {
        this.esIstEnabled = esIstEnabled;
        this.effect = effect;
    }

    @Override
    public byte[] toByte() {
        final byte esIst = (byte) (esIstEnabled ? 1 : 0);
        return new byte[]{'E', (byte) effect, esIst, 0};
    }
}
