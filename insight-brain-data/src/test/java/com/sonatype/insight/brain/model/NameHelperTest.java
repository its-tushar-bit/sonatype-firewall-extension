/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import org.junit.Test;

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
}
