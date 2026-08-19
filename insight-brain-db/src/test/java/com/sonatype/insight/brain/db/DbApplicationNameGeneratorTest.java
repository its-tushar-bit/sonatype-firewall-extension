/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DbApplicationNameGeneratorTest
{
  @Test
  public void testGenerateApplicationName() {
    DbApplicationNameGenerator dbApplicationNameGenerator = new DbApplicationNameGenerator()
    {
      @Override
      String getHostName() {
        return "somehost";
      }
    };
    String name = dbApplicationNameGenerator.generateApplicationNameWithHost("bar");
    assertThat(name).containsPattern("bar-somehost-[a-zA-Z0-9]{5}");
  }
}
