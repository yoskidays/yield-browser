package com.yieldbrowser.app;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-128 CBC decryption used by standard HLS EXT-X-KEY playlists. */
final class HlsAes128 {
    private HlsAes128() {
    }

    static byte[] decrypt(byte[] encrypted, byte[] key, byte[] explicitIv, long sequence)
            throws Exception {
        if (encrypted == null || encrypted.length == 0) {
            throw new Exception("Segmen HLS terenkripsi kosong");
        }
        if (key == null || key.length != 16) {
            throw new Exception("Kunci AES-128 HLS tidak valid");
        }
        byte[] iv = explicitIv != null ? explicitIv : sequenceToIv(sequence);
        if (iv.length != 16) throw new Exception("IV AES-128 HLS tidak valid");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }

    static byte[] sequenceToIv(long sequence) {
        byte[] iv = new byte[16];
        for (int index = 15; index >= 0 && sequence != 0; index--) {
            iv[index] = (byte) (sequence & 0xffL);
            sequence >>>= 8;
        }
        return iv;
    }
}
