/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ValidationAssert
{
  public static void assertValidationResultHasNoErrors(ValidationResult result) {
    assertThat(result, is(notNullValue()));
    assertThat(result.isValid(), is(true));
    assertThat(result.getErrors(), hasSize(0));
  }

  public static void assertValidationResultHasErrors(ValidationResult result, String... errors) {
    assertThat(result, is(notNullValue()));
    assertThat(result.isValid(), is(false));
    assertThat(result.getErrors(), contains(errors));
  }
}
