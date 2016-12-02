package com.tobiasschuerg.wordclock;

import java.util.UUID;

/**
 * Created by Tobias Schürg on 02.12.2016.
 */
public class Config {

    // Unique UUID for this application
    static final UUID MY_UUID_SECURE   = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Number of effects supported by word clock
    public static final int NUMBER_OF_EFFECTS = 6;

}
