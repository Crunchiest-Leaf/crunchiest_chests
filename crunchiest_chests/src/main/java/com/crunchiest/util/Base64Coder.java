package com.crunchiest.util;

import java.util.Base64;

/*
* CRUNCHIEST CHESTS
*   ____ ____  _   _ _   _  ____ _   _ ___ _____ ____ _____    ____ _   _ _____ ____ _____ ____  
*  / ___|  _ \| | | | \ | |/ ___| | | |_ _| ____/ ___|_   _|  / ___| | | | ____/ ___|_   _/ ___| 
* | |   | |_) | | | |  \| | |   | |_| || ||  _| \___ \ | |   | |   | |_| |  _| \___ \ | | \___ \ 
* | |___|  _ <| |_| | |\  | |___|  _  || || |___ ___) || |   | |___|  _  | |___ ___) || |  ___) |
*  \____|_| \_\\___/|_| \_|\____|_| |_|___|_____|____/ |_|    \____|_| |_|_____|____/ |_| |____/
*
* Author: Crunchiest_Leaf
* 
* Description: A TChest Alternative, w/ SQLite Backend
* GitHub: https://github.com/Crunchiest-Leaf/crunchiest_chests/tree/main/crunchiest_chests
*/

/**
 * Utility class for encoding and decoding Base64 data.
 */
public class Base64Coder {

    /**
     * Encodes a byte array into a Base64 string.
     *
     * @param data The byte array to encode.
     * @return A Base64-encoded string representation of the input byte array.
     */
    public static String encodeLines(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decodes a Base64 string back into a byte array.
     *
     * @param data The Base64-encoded string to decode.
     * @return A byte array representing the decoded data.
     * @throws IllegalArgumentException if the input is not a valid Base64 string.
     */
    public static byte[] decodeLines(String data) {
        return Base64.getDecoder().decode(data);
    }
}