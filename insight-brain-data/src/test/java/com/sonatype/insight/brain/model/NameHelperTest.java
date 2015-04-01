/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static com.sonatype.insight.brain.model.ExceptionMessageMatcher.hasMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class NameHelperTest
{
  /**
   * A sample of characters not allowed.
   */
  public static final String[] INVALID_CHARACTERS = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "+" };

  public static final String[] INVALID_SPACING_NAMES = {
      " leading space", "trailing space ", " leading and trailing space ",
      "double  space", "  starts with double space", "ends with double space  "
  };

  // The names must be case-insensitive unique in order to avoid test failures due to entity names being
  // case-insensitive unique.
  public static final String[] VALID_NAMES = { "abcdefghijklmnopqrstuvwxyz", "BACDEFGHIJKLMNOPQRSTUVWXYZ",
      "1234567890", "-", ".", "_", "a b" };

  @Test
  public void validateNameCanNotBeBlank() throws Exception {
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
  public void validateNameCanNotHaveSpecialCharacters() throws Exception {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      verifyNameHasBadCharacter(name, name.charAt(0));
    }

    verifyNameHasBadCharacter("tab\tspace", '\t'); // maybe should be a whitespace validation error
  }

  @Test
  public void validateInvalidWhitespaceForName() {
    for (String name: NameHelperTest.INVALID_SPACING_NAMES) {
      verifyNameHasBadWhitespace(name);
    }
  }

  @Test
  public void validateInvalidNameLength() {
    try {
      NameHelper.validate("test-field-name", "test-field-value", 2);
    }
    catch (InvalidNameException e) {
      assertThat(e, hasMessage("test-field-name must be 2 characters or less."));
    }
  }

  private void verifyNameHasBadWhitespace(String name) {
    try {
      NameHelper.validate(name);
      fail("Expected validation exception for bad whitespace in name");
    }
    catch (InvalidNameException validationException) {
      assertThat(validationException,
          hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row."));
    }
  }

  private void verifyNameHasBadCharacter(String name, char c) {
    try {
      NameHelper.validate(name);
      fail("Expected validation exception for bad characters in name");
    }
    catch (InvalidNameException validationException) {
      assertThat(validationException, hasMessage(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", c)));
    }
  }

  private void verifyNameRequired(String name) {
    try {
      NameHelper.validate(name);
      fail("Expected validation exception for required name");
    }
    catch (InvalidNameException validationException) {
      assertThat(validationException, hasMessage("Name is required."));
    }
  }
}

class ExceptionMessageMatcher
    extends TypeSafeMatcher<Exception>
{
  private final Matcher<String> messageMatcher;

  public static ExceptionMessageMatcher hasMessage(String expectedMessage) {
    return new ExceptionMessageMatcher(expectedMessage);
  }

  public ExceptionMessageMatcher(String expectedMessage) {
    this.messageMatcher = CoreMatchers.is(expectedMessage);
  }

  @Override
  protected boolean matchesSafely(Exception e) {
    return messageMatcher.matches(e.getMessage());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("exception with message that ").appendDescriptionOf(
        messageMatcher);
  }
}
