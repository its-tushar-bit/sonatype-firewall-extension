/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Locale;

public class NameHelper
{
  public static final int MAX_NAME_LENGTH = 60;

  private NameHelper() {
  }

  public static String normalize(String name) {
    if (name != null) {
      // The name is whitespace and case insensitive
      return name.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH);
    }
    else {
      return null;
    }
  }

  public static boolean equals(String name1, String name2) {
    return (name1 != null) ? normalize(name1).equals(normalize(name2)) : name2 == null;
  }

  public static void validate(String name) {
    validate("Name", name);
  }

  public static void validate(String fieldName, String fieldValue) {
    if (fieldValue == null || fieldValue.trim().isEmpty()) {
      throw new InvalidNameException(fieldName + " is required.");
    }
    for (char c : fieldValue.toCharArray()) {
      if (!Character.isLetterOrDigit(c) && c != '-' && c != ' ') {
        throw new InvalidNameException(fieldName + " must be alpha numeric.");
      }
    }
    if (fieldValue.startsWith(" ") || fieldValue.endsWith(" ") || fieldValue.indexOf("  ") > 0) {
      throw new InvalidNameException(fieldName
          + " must not have leading or trailing spaces, or have two spaces in a row.");
    }
    if (fieldValue.length() > MAX_NAME_LENGTH) {
      throw new InvalidNameException(fieldName + " must be " + NameHelper.MAX_NAME_LENGTH + " characters or less.");
    }
  }
}
