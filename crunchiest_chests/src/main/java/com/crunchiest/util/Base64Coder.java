package com.crunchiest.util;

import java.util.Base64;

public class Base64Coder {

    // Encodes a byte array into a Base64 string
    public static String encodeLines(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    // Decodes a Base64 string back into a byte array
    public static byte[] decodeLines(String data) {
        return Base64.getDecoder().decode(data);
    }
}