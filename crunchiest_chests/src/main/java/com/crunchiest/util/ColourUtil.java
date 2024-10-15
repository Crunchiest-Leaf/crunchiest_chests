package com.crunchiest.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColourUtil {

    // Function to convert a hex color code (e.g., "#ffaa00") into a Minecraft legacy hex format (§x§f§f§a§a§0§0)
    private static String convertHexToLegacyColor(String hexColor) {
      StringBuilder legacyColor = new StringBuilder("§x");
      for (char c : hexColor.substring(1).toCharArray()) {
          legacyColor.append("§").append(c);
      }
      return legacyColor.toString();
  }

    // Method to parse the string with embedded hex codes and convert to Minecraft legacy colored string
    public static String parseColoredString(String input) {
      // Regex pattern to match the format "#RRGGBB" in the string
      String regex = "(#[0-9A-Fa-f]{6})([^#]*)";  // Notice that we removed the \\s* to avoid consuming spaces
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
