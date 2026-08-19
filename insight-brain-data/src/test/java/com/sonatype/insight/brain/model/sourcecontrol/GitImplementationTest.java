/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GitImplementationTest
{
  @Test
  public void testToString() {
    assertThat(GitImplementation.JAVA).hasToString("java");
    assertThat(GitImplementation.NATIVE).hasToString("native");
  }

  @Test
  public void testFromString() {
    assertThat(GitImplementation.fromString(null)).isNull();
    assertThat(GitImplementation.fromString("jAvA")).isEqualTo(GitImplementation.JAVA);
    assertThat(GitImplementation.fromString("nAtIvE")).isEqualTo(GitImplementation.NATIVE);
  }
}
