/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HashHelperTest
{
  @Test
  public void testTruncateHash() {
    String longHash = "123456789012345678901";
    assertThat(longHash.length()).isGreaterThan(HashHelper.MAX_LENGTH);
    assertThat(HashHelper.truncateHash(longHash)).isEqualTo(longHash.substring(0, 20));
  }
}
