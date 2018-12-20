/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationAssert
{
  public static void assertValidationResultHasNoErrors(ValidationResult result) {
    assertThat(result).isNotNull();
    assertThat(result.isValid()).isTrue();
    assertThat(result.getErrors()).isEmpty();
  }

  public static void assertValidationResultHasErrors(ValidationResult result, String... errors) {
    assertThat(result).isNotNull();
    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).containsExactly(errors);
  }
}
