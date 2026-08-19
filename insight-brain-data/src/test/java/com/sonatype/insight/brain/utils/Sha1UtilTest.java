/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Sha1UtilTest
{
  @Test
  public void testCalculateSHA1() {
    String input = "example";
    String expectedHash = "c3499c2729730a7f807efb8676a92dcb6f8a3f8f";

    assertThat(Sha1Util.sha1(input)).isEqualTo(expectedHash);
  }

  @Test
  public void testCalculateHalfSHA1() {
    String input = "example";
    String expectedHash = "c3499c2729730a7f807e";

    assertThat(Sha1Util.halfSha1(input)).isEqualTo(expectedHash);
  }
}
