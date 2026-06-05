package com.sibam.cache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Pomožni razred za izračun SHA-256 hash vrednosti cache artefaktov.
 */
public final class Sha256 {

    private Sha256() {
    }

    /**
     * Izračuna SHA-256 hash in ga vrne kot heksadecimalni niz.
     *
     * @param bytes vsebina za hashiranje
     * @return SHA-256 v hex obliki
     */
    public static String hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
