/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import com.sonatype.insight.error.exception.BadRequestException;

public final class ApplicationVersionValidator
{
  private ApplicationVersionValidator() {
  }

  public static String validate(String raw) {
    if (raw == null) {
      throw new BadRequestException("applicationVersion must not be null");
    }
    // Reject control chars on the RAW value before trim() — String.trim() silently strips
    // any char <= 0x20 from the ends, which would let a leading/trailing \x01 slip through.
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c <= 0x1F || c == 0x7F) {
        throw new BadRequestException(
            "applicationVersion must not contain control characters (U+0000-U+001F, U+007F)");
      }
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty() || trimmed.length() > 200) {
      throw new BadRequestException(
          "applicationVersion must be between 1 and 200 characters (was " + trimmed.length() + ")");
    }
    if (trimmed.indexOf('/') >= 0 || trimmed.indexOf('\\') >= 0 || trimmed.contains("..")) {
      throw new BadRequestException("applicationVersion must not contain path separators or '..'");
    }
    if (trimmed.indexOf('<') >= 0 || trimmed.indexOf('>') >= 0
        || trimmed.indexOf('"') >= 0 || trimmed.indexOf('\'') >= 0
        || trimmed.indexOf('&') >= 0)
    {
      throw new BadRequestException(
          "applicationVersion must not contain HTML metacharacters (< > \" ' &)");
    }
    char prev = 0;
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      boolean alnum = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
      boolean allowed = c == '.' || c == '-' || c == '_' || c == '+' || c == ':' || c == '~' || c == ' ';
      if (!alnum && !allowed) {
        throw new BadRequestException(
            "applicationVersion contains disallowed character '" + c + "' "
                + "(allowed: alphanumerics, '.', '-', '_', '+', ':', '~', and single internal spaces)");
      }
      if (c == ' ' && prev == ' ') {
        throw new BadRequestException("applicationVersion must not contain consecutive spaces");
      }
      prev = c;
    }
    return trimmed;
  }
}
