package com.example.vpnapp.utils;

import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class HashUtils {
    private static final String TAG = "Util";
    private static final String MD5 = "MD5";
    private static final String SHA256 = "SHA-256";

    public static String convertByteArrayToHexString(byte[] bytes) {
        String result = "";

        for (int i = 0; i < bytes.length; i++)
            result += Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1);

        return result.toLowerCase(Locale.getDefault());
    }

    /**
     * Calculate file checksum
     *
     * @param filePath
     * @param algorithm
     * @return
     */
    private static byte[] createChecksum(String filePath, String algorithm) throws Exception {
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(filePath));
        byte[] buffer = new byte[4096];
        MessageDigest complete = MessageDigest.getInstance(algorithm);

        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead != -1)
                complete.update(buffer, 0, numRead);
        }
        while (numRead != -1);

        buffer = null;
        fis.close();
        System.gc();
        return complete.digest();
    }

    /**
     * Get MD5 hex string of file
     *
     * @param path
     * @return
     */
    public static String getMd5File(String path) {
        try {
            return convertByteArrayToHexString(createChecksum(path, MD5));
        } catch (Exception e) {
            return "";
        } catch (Error e) {
            return "";
        }
    }


    public static byte[] getMd5Bytes(byte[] buffer) throws Exception {
        MessageDigest complete = MessageDigest.getInstance(MD5);

        complete.update(buffer);

        return complete.digest();
    }

    public static String getSHA256(byte[] data) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return Base64.encodeToString(hash, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
        }
        return "";
    }

    public static String getMd5BytesAsString(byte[] buffer) throws Exception {
        return convertByteArrayToHexString(getMd5Bytes(buffer));
    }
}
