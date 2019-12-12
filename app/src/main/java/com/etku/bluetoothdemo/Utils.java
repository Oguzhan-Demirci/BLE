package com.etku.bluetoothdemo;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Utils {

    public static String byte2Hex(byte[] data) {
        if ((data != null) && (data.length > 0)) {
            StringBuilder sb = new StringBuilder(data.length);
            byte[] arrayOfByte = data;
            int j = data.length;
            for (int i = 0; i < j; i++) {
                byte tmp = arrayOfByte[i];
                sb.append(String.format("%02X ", new Object[]{Byte.valueOf(tmp)}));
            }
            return sb.toString();
        }
        return "no data";
    }

    public static byte[] encrypt(byte[] sSrc, byte[] sKey) {
        if (sSrc == null) {
            return null;
        }

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(sSrc);

            return encrypted;
        } catch (Exception ex) {
            return null;
        }
    }

    public static byte[] decrypt(byte[] sSrc, byte[] sKey) {

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] dncrypted = cipher.doFinal(sSrc);
            return dncrypted;

        } catch (Exception ex) {
            return null;
        }
    }

    public static byte[] getDefaultKey() {
        byte[] chars = new byte[]{58, 96, 67, 42, 92, 1, 33, 31, 41, 30, 15, 78, 12, 19, 40, 37};
        //byte[] chars = new byte[]{0x3A, 0x60, 0x43, 0x2A, 0x5C, 0x01, 0x21, 0x1F, 0x29, 0x1E, 0x0F, 0x4E, 0x0C, 0x13, 0x28, 0x25};

        //byte[] chars = new byte[]{0x20, 0x57, 0x2F, 0x52, 0x36, 0x4B, 0x3F, 0x47, 0x30, 0x50, 0x41, 0x58, 0x11, 0x63, 0x2D, 0x2B};
        //byte[] chars = new byte[]{32, 87, 47, 82, 54, 75, 63, 71, 48, 90, 65, 88, 17, 99, 45, 43};

        return chars;
    }

    public static byte[] getTokenAcquisitionCommand() {

        byte[] b = new byte[12];
        Random random = new Random();
        random.nextBytes(b);
        //byte2Hex(b);
        encrypt(b, getDefaultKey());
        byte[] tokenAcquisitionCommand = {6, 1, 1, 1, b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9], b[10], b[11]};
        byte[] encryptedCommand = encrypt(tokenAcquisitionCommand, getDefaultKey());

        return encryptedCommand;
    }

    public static String byte2String(byte[] bytes){
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
