package com.crunchiest.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Utility class for color manipulation, specifically for converting hex color codes 
 * into Minecraft legacy color format.
 */
public class ColourUtil {

    /**
     * Converts a hex color code (e.g., "#ffaa00") into a Minecraft legacy hex format 
     * (§x§f§f§a§a§0§0).
     *
     * @param hexColor The hex color code to convert.
     * @return A string representing the color in Minecraft legacy format.
     */
    private static String convertHexToLegacyColor(String hexColor) {
        StringBuilder legacyColor = new StringBuilder("§x");
        for (char c : hexColor.substring(1).toCharArray()) {
            legacyColor.append("§").append(c);
        }
        return legacyColor.toString();
    }

    /**
     * Parses a string with embedded hex codes and converts it to a Minecraft legacy colored 
     * string.
     *
     * @param input The input string containing hex color codes.
     * @return A string where hex color codes are replaced with Minecraft legacy color codes.
     */
    public static String parseColoredString(String input) {
        // Regex pattern to match the format "#RRGGBB" in the string
        String regex = "(#[0-9A-Fa-f]{6})([^#]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        StringBuilder result = new StringBuilder();
        int lastMatchEnd = 0;

        while (matcher.find()) {
            // Get the hex color and the corresponding text
            String hexColor = matcher.group(1);
            String text = matcher.group(2);  // No trimming here, preserve the spaces

            // Append the text before the match (to preserve spaces between matches)
            result.append(input, lastMatchEnd, matcher.start());

            // Convert hex color to legacy Minecraft color code format
            String legacyColor = convertHexToLegacyColor(hexColor);

            // Append the color code and text to the result string
            result.append(legacyColor).append(text);

            // Update the end position of the last match
            lastMatchEnd = matcher.end();
        }

        // Append any remaining text after the last match
        result.append(input.substring(lastMatchEnd));

        return result.toString();
    }
}