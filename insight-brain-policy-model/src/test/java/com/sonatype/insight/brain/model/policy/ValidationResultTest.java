/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ValidationResultTest
{
  @Test
  public void testNoErrorsDefaultStateIsValid() {
    assertThat(ValidationResult.noErrors().isValid(), is(true));
  }

  @Test
  public void testNoErrorsIsMutable() {
    ValidationResult result = ValidationResult.noErrors();
    result.addError("any error");
    
    assertThat(result.isValid(), is(false));
  }
}
