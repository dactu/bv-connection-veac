package com.vht.connection.Asterix;

import lombok.extern.log4j.Log4j2;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Log4j2
public class AsterixCat48Encoder {
    private static AsterixCat48Encoder instance;

    public static AsterixCat48Encoder getInstance() {
        synchronized (AsterixCat48Encoder.class) {
            if (instance == null) {
                instance = new AsterixCat48Encoder();
            }
        }
        return instance;
    }

    public String encodeAsterixCat48 (int number, double azimuth, double range, double velocity, double heading, double altitude) {
        ByteBuffer buffer = ByteBuffer.allocate(23);

        // Header
        buffer.put((byte) 0x30);
        buffer.putShort((short) 23);

        // FSPEC
        byte[] fspec = new byte[3];
        fspec[0] = (byte) 0b11010001;
        fspec[1] = (byte) 0b00010101;
        fspec[2] = (byte) 0b00001000;
        buffer.put(fspec);

        // FRN 1: Data Source Identifier
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x02);

        // FRN 2: TOD
        int value = 1000*128;
        byte byte1 = (byte) ((value >> 16) & 0xFF);
        byte byte2 = (byte) ((value >> 8) & 0xFF);
        byte byte3 = (byte) (value & 0xFF);
        buffer.put(byte1);
        buffer.put(byte2);
        buffer.put(byte3);

        // FRN 4: Measured Position in Slant Polar Coordinates
        short rho = (short) (range * (256.0/1852.0));
        buffer.putShort(rho);
        short theta = (short) (azimuth * ((1<<16)/360.0));
        buffer.putShort(theta);

        // FRN 11: Track Number
        short val2ue = (short)number;
        buffer.putShort(val2ue);

        // FRN 13: Calculated Track Velocity in Polar
        short speed = (short) (velocity*((1<<14)/1852.0));
        buffer.putShort(speed);
        short head = (short) (heading*((1<<16)/360.0));
        buffer.putShort(head);

        // FRN 19: Height Measured by 3D Radar
        short height = (short) ((short)(altitude/7.62) & 0x3FFF);
        buffer.putShort(height);

        return new String(buffer.array(), StandardCharsets.ISO_8859_1);
    }
}
