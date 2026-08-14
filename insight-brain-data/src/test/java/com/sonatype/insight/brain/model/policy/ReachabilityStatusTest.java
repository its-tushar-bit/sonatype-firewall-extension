/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReachabilityStatusTest
{
  @Test
  public void testFromString() {
    assertThat(ReachabilityStatus.fromString("reachable")).isEqualTo(ReachabilityStatus.REACHABLE);
    assertThat(ReachabilityStatus.fromString("non-reachable")).isEqualTo(ReachabilityStatus.NON_REACHABLE);
    assertThat(ReachabilityStatus.fromString("unknown")).isEqualTo(ReachabilityStatus.UNKNOWN);
    assertThat(ReachabilityStatus.fromString(null)).isNull();
    assertThat(ReachabilityStatus.fromString("")).isNull();
    assertThatThrownBy(() -> ReachabilityStatus.fromString("unrecognized"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unrecognized reachability status with name: unrecognized");
  }

  @Test
  public void testFromBoolean() {
    assertThat(ReachabilityStatus.fromBoolean(true)).isEqualTo(ReachabilityStatus.REACHABLE);
    assertThat(ReachabilityStatus.fromBoolean(false)).isEqualTo(ReachabilityStatus.NON_REACHABLE);
    assertThat(ReachabilityStatus.fromBoolean(null)).isEqualTo(ReachabilityStatus.UNKNOWN);
  }

  @Test
  public void testToBoolean() {
    assertThat(ReachabilityStatus.REACHABLE.toBoolean()).isTrue();
    assertThat(ReachabilityStatus.NON_REACHABLE.toBoolean()).isFalse();
    assertThat(ReachabilityStatus.UNKNOWN.toBoolean()).isNull();
  }

  @Test
  public void testCombine() {
    assertThat(ReachabilityStatus.combine(ReachabilityStatus.REACHABLE, ReachabilityStatus.REACHABLE))
        .isEqualTo(ReachabilityStatus.REACHABLE);
    assertThat(ReachabilityStatus.combine(ReachabilityStatus.REACHABLE, ReachabilityStatus.NON_REACHABLE))
        .isEqualTo(ReachabilityStatus.REACHABLE);
    assertThat(ReachabilityStatus.combine(ReachabilityStatus.REACHABLE, ReachabilityStatus.UNKNOWN))
        .isEqualTo(ReachabilityStatus.REACHABLE);

    assertThat(ReachabilityStatus.combine(ReachabilityStatus.NON_REACHABLE, ReachabilityStatus.REACHABLE))
        .isEqualTo(ReachabilityStatus.REACHABLE);
    assertThat(ReachabilityStatus.combine(ReachabilityStatus.NON_REACHABLE, ReachabilityStatus.NON_REACHABLE))
        .isEqualTo(ReachabilityStatus.NON_REACHABLE);
    assertThat(ReachabilityStatus.combine(ReachabilityStatus.NON_REACHABLE, ReachabilityStatus.UNKNOWN))
        .isEqualTo(ReachabilityStatus.UNKNOWN);

    assertThat(ReachabilityStatus.combine(ReachabilityStatus.UNKNOWN, ReachabilityStatus.REACHABLE))
        .isEqualTo(ReachabilityStatus.REACHABLE);
    assertThat(ReachabilityStatus.combine(ReachabilityStatus.UNKNOWN, ReachabilityStatus.NON_REACHABLE))
        .isEqualTo(ReachabilityStatus.UNKNOWN);
    assertThat(ReachabilityStatus.combine(ReachabilityStatus.UNKNOWN, ReachabilityStatus.UNKNOWN))
        .isEqualTo(ReachabilityStatus.UNKNOWN);
  }

  @Test
  public void testCombineStream() {
    assertThat(ReachabilityStatus.combine(Stream.of(
        ReachabilityStatus.REACHABLE,
        ReachabilityStatus.NON_REACHABLE,
        ReachabilityStatus.UNKNOWN))).isEqualTo(ReachabilityStatus.REACHABLE);

    assertThat(ReachabilityStatus.combine(Stream.of(
        ReachabilityStatus.NON_REACHABLE,
        ReachabilityStatus.NON_REACHABLE,
        ReachabilityStatus.NON_REACHABLE))).isEqualTo(ReachabilityStatus.NON_REACHABLE);

    assertThat(ReachabilityStatus.combine(Stream.of(
        ReachabilityStatus.NON_REACHABLE,
        ReachabilityStatus.NON_REACHABLE,
        ReachabilityStatus.UNKNOWN))).isEqualTo(ReachabilityStatus.UNKNOWN);

    assertThat(ReachabilityStatus.combine(Stream.of())).isEqualTo(ReachabilityStatus.UNKNOWN);
  }
}
