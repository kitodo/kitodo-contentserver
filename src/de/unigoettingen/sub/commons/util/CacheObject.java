package de.unigoettingen.sub.commons.util;

import java.io.Serializable;

public class CacheObject implements Serializable {

    private static final long serialVersionUID = 5644747498518951177L;
    private byte[] data;

    public CacheObject(byte[] in) {
        data = in;
    }

    public byte[] getData() {
        return data;
    }
}
