/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Locale;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

public class NameHelper
{
  public static final String INVALID_CHAR_MESSAGE = "%s contains an invalid character: '%c'.";

  public static final int MAX_NAME_LENGTH = 60;

  public static final int MAX_NAME_LENGTH_APP_ORG = 200;

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

  public static void validate(String name) throws InvalidNameException {
    validate("Name", name);
  }

  public static void validate(String fieldName, String fieldValue) throws InvalidNameException {
    validate(fieldName, fieldValue, MAX_NAME_LENGTH);
  }

  public static void validate(String fieldName, String fieldValue, int maxNameLength) {
    if (fieldValue == null || fieldValue.trim().isEmpty()) {
      throw new InvalidNameException(fieldName + " is required.");
    }
    for (char c : fieldValue.toCharArray()) {
      if (!Character.isLetterOrDigit(c) && "-._ ".indexOf(c) < 0) {
        throw new InvalidNameException(String.format(INVALID_CHAR_MESSAGE, fieldName, c));
      }
    }
    if (fieldValue.startsWith(" ") || fieldValue.endsWith(" ") || fieldValue.indexOf("  ") > 0) {
      throw new InvalidNameException(fieldName
          + " must not have leading or trailing spaces, or have two spaces in a row.");
    }
    if (fieldValue.length() > maxNameLength) {
      throw new InvalidNameException(fieldName + " must be " + maxNameLength + " characters or less.");
    }
  }

  public static String convertContainerImageToApplicationPublicIdAndName(
      String repositoryId,
      String namespace,
      String name,
      String version)
  {
    if (StringUtils.isAnyBlank(repositoryId, namespace, name, version)) {
      throw new InvalidNameException(
          "repositoryId, namespace, name and version are all required for a container image");
    }

    try {
      // The same container image can be pulled from multiple repos, using the repositoryId as prefix allows uniqueness
      // Using the Base 64 representation reduces the length of the UUID bytes from 32 to 22
      byte[] bytes = Hex.decodeHex(repositoryId);
      String repositoryIdPrefix = Base64.encodeBase64URLSafeString(bytes) + "-";

      String applicationPublicId = String.format("%s-%s-%s", namespace, name, version);
      applicationPublicId = StringUtils.deleteWhitespace(applicationPublicId);
      applicationPublicId =
          StringUtils.right(applicationPublicId, MAX_NAME_LENGTH_APP_ORG - repositoryIdPrefix.length());
      applicationPublicId = repositoryIdPrefix + applicationPublicId;
      applicationPublicId = applicationPublicId.replaceAll("[^a-zA-Z0-9\\-._]", "_");

      return applicationPublicId;
    }
    catch (DecoderException e) {
      throw new IllegalArgumentException("Invalid repositoryId: " + repositoryId, e);
    }
  }

  public static String convertToValidName(String value) {
    if (StringUtils.isBlank(value)) {
      throw new InvalidNameException("A name cannot be empty or blank");
    }

    String name = value.trim().replaceAll("[^a-zA-Z0-9\\-._]", "_");
    name = StringUtils.left(name, MAX_NAME_LENGTH);

    return name;
  }
}
