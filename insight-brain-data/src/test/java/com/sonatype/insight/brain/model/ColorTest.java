/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ColorTest
{
  @Test
  public void testToValueReplacesUnderscoresWithHyphens() {
    assertThat(Color.light_red.toValue()).isEqualTo("light-red");
  }

  @Test
  public void testToValueDoesNotChangeNamesWithoutUnderscores() {
    assertThat(Color.orange.toValue()).isEqualTo("orange");
  }
}
