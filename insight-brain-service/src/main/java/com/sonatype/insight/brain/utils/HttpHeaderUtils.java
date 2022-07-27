/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

public class HttpHeaderUtils
{
  private HttpHeaderUtils() {
    // Utility class
  }

  public static String buildContentDispositionHeaderValue(String filename) {
    return String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s", escapeQuotationsInFilename(filename),
        encodeFilenameUtf8(filename));
  }

  // Visible for testing
  static String escapeQuotationsInFilename(String filename) {
    if (StringUtils.isBlank(filename) || (filename.indexOf('"') == -1 && filename.indexOf('\\') == -1)) {
      return filename;
    }
    boolean escaped = false;
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < filename.length(); i++) {
      char c = filename.charAt(i);
      if (!escaped && c == '"') {
        result.append("\\\"");
      }
      else {
        result.append(c);
      }
      escaped = !escaped && c == '\\';
    }
    // Remove backslash at the end.
    if (escaped) {
      result.deleteCharAt(result.length() - 1);
    }
    return result.toString();
  }

  // Visible for testing
  static String encodeFilenameUtf8(String input) {
    if (input == null) {
      return input;
    }
    StringBuilder result = new StringBuilder();
    for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
      if (isRFC5987AttrChar(b)) {
        result.append((char) b);
      }
      else {
        result.append('%');
        char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
        char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
        result.append(hex1);
        result.append(hex2);
      }
    }
    return result.toString();
  }

  private static boolean isRFC5987AttrChar(byte c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
        c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
        c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
  }
}
