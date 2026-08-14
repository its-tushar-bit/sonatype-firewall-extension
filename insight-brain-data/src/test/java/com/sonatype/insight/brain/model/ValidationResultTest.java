/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationResultTest
{
  @Test
  public void testNoErrorsDefaultStateIsValid() {
    assertThat(ValidationResult.noErrors().isValid()).isTrue();
  }

  @Test
  public void testNoErrorsIsMutable() {
    ValidationResult result = ValidationResult.noErrors();
    result.addError("any error");

    assertThat(result.isValid()).isFalse();
  }
}
