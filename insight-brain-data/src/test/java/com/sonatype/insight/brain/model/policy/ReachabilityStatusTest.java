/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReachabilityStatusTest
{
  @Test
  public void testFromString() {
    assertThat(ReachabilityStatus.fromString("reachable")).isEqualTo(ReachabilityStatus.REACHABLE);
    assertThat(ReachabilityStatus.fromString("non-reachable")).isEqualTo(ReachabilityStatus.NON_REACHABLE);
    assertThat(ReachabilityStatus.fromString(null)).isNull();
    assertThat(ReachabilityStatus.fromString("")).isNull();
    assertThatThrownBy(() -> ReachabilityStatus.fromString("unknown"))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown reachability status with name: unknown");
  }
}
