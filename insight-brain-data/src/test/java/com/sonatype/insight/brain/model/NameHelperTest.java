/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.insight.brain.db.IdUtil;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NameHelperTest
{
  /**
   * A sample of characters not allowed.
   */
  public static final String[] INVALID_CHARACTERS = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "+", "<" };

  public static final String[] INVALID_SPACING_NAMES = { " leading space", "trailing space ",
      " leading and trailing space ", "double  space", "  starts with double space", "ends with double space  " };

  // The names must be case-insensitive unique in order to avoid test failures due to entity names being
  // case-insensitive unique.
  public static final String[] VALID_NAMES = { "abcdefghijklmnopqrstuvwxyz", "BACDEFGHIJKLMNOPQRSTUVWXYZ",
      "1234567890", "-", "a.", "_", "a b" };

  @Test
  public void validateNameCanNotBeBlank() {
    verifyNameRequired(null);
    verifyNameRequired("");
    verifyNameRequired(" ");
    verifyNameRequired("\t");
  }

  @Test
  public void validateAllowedCharactersForName() {
    for (String name : VALID_NAMES) {
      NameHelper.validate(name);
    }
  }

  @Test
  public void validateAllowedWhitespaceForName() {
    NameHelper.validate("a b");
  }

  @Test
  public void validateNameCanNotHaveSpecialCharacters() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      verifyNameHasBadCharacter(name, name.charAt(0));
    }

    verifyNameHasBadCharacter("tab\tspace", '\t'); // maybe should be a whitespace validation error
  }

  @Test
  public void validateInvalidWhitespaceForName() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      verifyNameHasBadWhitespace(name);
    }
  }

  @Test
  public void validateInvalidNameLength() {
    assertThatThrownBy(() -> NameHelper.validate("test-field-name", "test-field-value", 2))
        .isInstanceOf(InvalidNameException.class).hasMessage("test-field-name must be 2 characters or less.");
  }

  @Test
  public void testConvertContainerImageToApplicationPublicIdAndName_noRepositoryId() {
    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(null, "namespace", "name", "version"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName("", "namespace", "name", "version"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(" ", "namespace", "name", "version"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");
  }

  @Test
  public void testConvertContainerImageToApplicationPublicIdAndName_noNamespace() {
    String repositoryId = IdUtil.newUUID();

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, null, "name", "version"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, "", "name", "version"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, " ", "name", "version"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");
  }

  @Test
  public void testConvertContainerImageToApplicationPublicIdAndName_noName() {
    String repositoryId = IdUtil.newUUID();

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, "namespace", null, "version"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, "namespace", "", "version"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, "namespace", " ", "version"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");
  }

  @Test
  public void testConvertContainerImageToApplicationPublicIdAndName_noVersion() {
    String repositoryId = IdUtil.newUUID();

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, "namespace", "name", null))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, "namespace", "name", ""))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");

    assertThatThrownBy(
        () -> NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, "namespace", "name", " "))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("repositoryId, namespace, name and version are all required for a container image");
  }

  @Test
  public void testConvertContainerImageToApplicationPublicIdAndName() {
    String repositoryId = IdUtil.newUUID();
    String repositoryIdBase64 = toBytesBase64(repositoryId);

    assertThat(
        NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, "namespace", "name", "version"))
            .isEqualTo(repositoryIdBase64 + "-namespace-name-version");

    assertThat(NameHelper.convertContainerImageToApplicationPublicIdAndName(
        repositoryId, "namespace 1", "name 2", "version 3"))
            .isEqualTo(repositoryIdBase64 + "-namespace1-name2-version3");

    assertThat(
        NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, "namespace?", "name&", "version="))
            .isEqualTo(repositoryIdBase64 + "-namespace_-name_-version_");

    String longNamespace = StringUtils.repeat("test", NameHelper.MAX_NAME_LENGTH_APP_ORG);
    String expectedLongResult = longNamespace + "-name-version";
    expectedLongResult =
        StringUtils.right(expectedLongResult, NameHelper.MAX_NAME_LENGTH_APP_ORG - (repositoryIdBase64.length() + 1));
    expectedLongResult = repositoryIdBase64 + "-" + expectedLongResult;

    assertThat(
        NameHelper.convertContainerImageToApplicationPublicIdAndName(repositoryId, longNamespace, "name", "version"))
            .hasSize(NameHelper.MAX_NAME_LENGTH_APP_ORG)
            .isEqualTo(expectedLongResult);
  }

  @Test
  public void testConvertToValidName_empty() {
    assertThatThrownBy(() -> NameHelper.convertToValidName(""))
        .isInstanceOf(InvalidNameException.class)
        .hasMessage("A name cannot be empty or blank");

    assertThatThrownBy(() -> NameHelper.convertToValidName(" "))
        .isInstanceOf(InvalidNameException.class)
        .hasMessage("A name cannot be empty or blank");
  }

  @Test
  public void testConvertToValidName() {
    assertThat(NameHelper.convertToValidName("test")).isEqualTo("test");
    assertThat(NameHelper.convertToValidName("test?")).isEqualTo("test_");

    String longName = "test".repeat(NameHelper.MAX_NAME_LENGTH + 10);
    assertThat(NameHelper.convertToValidName(longName)).isEqualTo(longName.substring(0, NameHelper.MAX_NAME_LENGTH));
  }

  private void verifyNameHasBadWhitespace(String name) {
    assertThatThrownBy(() -> NameHelper.validate(name)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
  }

  private void verifyNameHasBadCharacter(String name, char c) {
    assertThatThrownBy(() -> NameHelper.validate(name)).isInstanceOf(InvalidNameException.class)
        .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", c);
  }

  private void verifyNameRequired(String name) {
    assertThatThrownBy(() -> NameHelper.validate(name)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name is required.");
  }

  private String toBytesBase64(String value) {
    try {
      byte[] bytes = Hex.decodeHex(value);
      return Base64.encodeBase64URLSafeString(bytes);
    }
    catch (DecoderException e) {
      throw new RuntimeException(e);
    }
  }
}
