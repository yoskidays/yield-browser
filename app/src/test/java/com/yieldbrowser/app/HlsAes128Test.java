package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;

public final class HlsAes128Test {
    @Test
    public void decryptsUsingMediaSequenceIv() throws Exception {
        byte[] key = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        byte[] plain = "yield-hls-test-segment".getBytes(StandardCharsets.UTF_8);
        long sequence = 42;
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new IvParameterSpec(HlsAes128.sequenceToIv(sequence)));
        byte[] encrypted = cipher.doFinal(plain);
        assertEquals("yield-hls-test-segment",
                new String(HlsAes128.decrypt(encrypted, key, null, sequence), StandardCharsets.UTF_8));
    }
}
