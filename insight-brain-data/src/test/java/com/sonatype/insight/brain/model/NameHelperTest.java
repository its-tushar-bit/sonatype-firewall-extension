/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
  @Test
  public void validateNameCanNotBeBlank() throws Exception {
    verifyNameRequired(null);
    verifyNameRequired("");
    verifyNameRequired(" ");
    verifyNameRequired("\t");
  }

  @Test
  public void validateAllowedCharactersForName() {
    // A sample of allowed characters, guessing that unicode characters are allowed too
    NameHelper.validate("abcdefghijklmnopqrstuvwxyz");
    NameHelper.validate("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    NameHelper.validate("1234567890");
    NameHelper.validate("-");
  }

  @Test
  public void validateAllowedWhitespaceForName() {
    NameHelper.validate("a b");
  }

  @Test
  public void validateNameCanNotHaveSpecialCharacters() throws Exception {
    // A small sample of characters not allowed
    verifyNameHasBadCharacter(".");
    verifyNameHasBadCharacter("*");
    verifyNameHasBadCharacter("/");
    verifyNameHasBadCharacter("_");
    verifyNameHasBadCharacter("tab\tspace");  // maybe should be a whitespace validation error
  }

  @Test
  public void validateInvalidWhitespaceForName() {
    verifyNameHasBadWhitespace(" leading");
    verifyNameHasBadWhitespace("trailing ");
    verifyNameHasBadWhitespace(" leading-and-trailing ");
    verifyNameHasBadWhitespace("double  space");
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

  private void verifyNameHasBadCharacter(String name) {
    try {
      NameHelper.validate(name);
      fail("Expected validation exception for bad characters in name");
    }
    catch (InvalidNameException validationException) {
      assertThat(validationException, hasMessage("Name must be alpha numeric."));
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

/**
 * Matches exception messages.
 */
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
