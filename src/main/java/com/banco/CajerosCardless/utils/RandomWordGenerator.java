package com.banco.CajerosCardless.utils;

import java.security.SecureRandom;

public class RandomWordGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateWord(int length) {
        StringBuilder word = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            word.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return word.toString();
    }
}
