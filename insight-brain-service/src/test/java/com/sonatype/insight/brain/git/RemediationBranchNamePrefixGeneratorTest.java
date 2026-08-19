/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RemediationBranchNamePrefixGeneratorTest
{
  @Test
  public void test() {
    RemediationBranchNamePrefixGenerator prefixGenerator = new RemediationBranchNamePrefixGenerator();

    assertThat(prefixGenerator.generatePrefixForApplication("12345678")).isEqualTo("123456");
    assertThat(prefixGenerator.generatePrefixForApplication(null)).isEqualTo("");
    assertThat(prefixGenerator.generatePrefixForApplication("")).isEqualTo("");
    assertThat(prefixGenerator.generatePrefixForApplication("123")).isEqualTo("123");
    assertThat(prefixGenerator.generatePrefixForApplication("  ")).isEqualTo("");
    assertThat(prefixGenerator.generatePrefixForApplication(" 1 2 3 4 5 6  ")).isEqualTo("1 2 3");
  }
}
