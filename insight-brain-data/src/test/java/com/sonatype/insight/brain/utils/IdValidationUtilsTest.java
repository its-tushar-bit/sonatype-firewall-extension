/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class IdValidationUtilsTest
{
  private static final String VALID_ID = "abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLOMOPQUSTUVWXYZ-0123456789";

  private static final String[] INVALID_CHARACTERS = { ".", "\\", "/", "%" };

  @Test
  public void testValidate() {
    IdValidationUtils.validate(VALID_ID);
  }

  @Test
  public void testGetScanDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        IdValidationUtils.validate(invalidValue);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }
}
