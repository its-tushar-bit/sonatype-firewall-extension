/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest
{
  @Test
  public void testNormalizeUsername_Null() {
    assertThat(User.normalizeUsername(null)).isNull();
  }

  @Test
  public void testNormalizeUsername_Empty() {
    assertThat(User.normalizeUsername("")).isEqualTo("");
  }

  @Test
  public void testNormalizeUsername_WhiteSpaceOnly() {
    assertThat(User.normalizeUsername(" ")).isEqualTo(" ");
  }

  @Test
  public void testNormalizeUsername() {
    assertThat(User.normalizeUsername("John Doe")).isEqualTo("john doe");
  }
}
