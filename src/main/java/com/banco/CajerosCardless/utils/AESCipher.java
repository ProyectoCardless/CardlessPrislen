/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESCipher {

    private static final String SECRET_KEY = "1234567890123456"; // Cambia esta clave para producción
    private static final String INIT_VECTOR = "RandomInitVector"; // Cambia este IV para producción

    public static String encrypt(String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
