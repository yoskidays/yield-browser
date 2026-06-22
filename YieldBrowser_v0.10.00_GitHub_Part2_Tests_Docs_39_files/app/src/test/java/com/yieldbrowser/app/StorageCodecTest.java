package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class StorageCodecTest {
    @Test
    public void roundTripHandlesDelimitersAndUnicode() {
        String value = "Judul | spasi & 日本語";
        assertEquals(value, StorageCodec.decode(StorageCodec.encode(value)));
    }

    @Test
    public void nullValuesBecomeEmptyStrings() {
        assertEquals("", StorageCodec.decode(StorageCodec.encode(null)));
    }
}
