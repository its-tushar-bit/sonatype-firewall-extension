/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.MissingReachableComponentVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.PresentReachableComponentVulnerabilities;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ReachableComponentVulnerabilitiesTest
{
  @Test
  public void testReachableComponentVulnerabilities_NullVulnerabilitySignatures() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(
        () -> new PresentReachableComponentVulnerabilities(null));
  }

  @Test
  public void testReachableComponentVulnerabilities_EmptyVulnerabilitySignatures() {
    PresentReachableComponentVulnerabilities presentReachableComponentVulnerabilities =
        new PresentReachableComponentVulnerabilities(new HashSet<>());

    assertThat(presentReachableComponentVulnerabilities.references()).isEmpty();
  }

  @Test
  public void testReachableComponentVulnerabilities_NonEmptyVulnerabilitySignatures() {
    PresentReachableComponentVulnerabilities presentReachableComponentVulnerabilities =
        new PresentReachableComponentVulnerabilities(Set.of("v1"));

    assertThat(presentReachableComponentVulnerabilities.references()).containsExactly("v1");
  }

  @Test
  public void testReachableComponentVulnerabilities_Immutable() {
    PresentReachableComponentVulnerabilities presentReachableComponentVulnerabilities =
        new PresentReachableComponentVulnerabilities(Set.of("v1"));

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
        () -> presentReachableComponentVulnerabilities.references().add("v2"));
  }

  @Test
  public void testCombine() {
    ReachableComponentVulnerabilities r1 = null;
    ReachableComponentVulnerabilities r2 = MissingReachableComponentVulnerabilities.INSTANCE;
    ReachableComponentVulnerabilities r3 = new PresentReachableComponentVulnerabilities(Set.of());
    ReachableComponentVulnerabilities r4 = new PresentReachableComponentVulnerabilities(Set.of("v1"));
    ReachableComponentVulnerabilities r5 = new PresentReachableComponentVulnerabilities(Set.of("v2"));
    ReachableComponentVulnerabilities r6 = new PresentReachableComponentVulnerabilities(Set.of("v1", "v2"));
    ReachableComponentVulnerabilities r7 = new PresentReachableComponentVulnerabilities(Set.of("v2", "v3"));
    ReachableComponentVulnerabilities r8 = new PresentReachableComponentVulnerabilities(Set.of("v1", "v2", "v3"));

    assertThat(r2.combine(r1)).isEqualTo(r2);
    assertThat(r2.combine(r2)).isEqualTo(r2);
    assertThat(r2.combine(r3)).isEqualTo(r3);
    assertThat(r2.combine(r4)).isEqualTo(r4);
    assertThat(r2.combine(r5)).isEqualTo(r5);
    assertThat(r2.combine(r6)).isEqualTo(r6);
    assertThat(r2.combine(r7)).isEqualTo(r7);

    assertThat(r3.combine(r1)).isEqualTo(r3);
    assertThat(r3.combine(r2)).isEqualTo(r3);
    assertThat(r3.combine(r3)).isEqualTo(r3);
    assertThat(r3.combine(r4)).isEqualTo(r4);
    assertThat(r3.combine(r5)).isEqualTo(r5);
    assertThat(r3.combine(r6)).isEqualTo(r6);
    assertThat(r3.combine(r7)).isEqualTo(r7);

    assertThat(r4.combine(r1)).isEqualTo(r4);
    assertThat(r4.combine(r2)).isEqualTo(r4);
    assertThat(r4.combine(r3)).isEqualTo(r4);
    assertThat(r4.combine(r4)).isEqualTo(r4);
    assertThat(r4.combine(r5)).isEqualTo(r6);
    assertThat(r4.combine(r6)).isEqualTo(r6);
    assertThat(r4.combine(r7)).isEqualTo(r8);

    assertThat(r5.combine(r1)).isEqualTo(r5);
    assertThat(r5.combine(r2)).isEqualTo(r5);
    assertThat(r5.combine(r3)).isEqualTo(r5);
    assertThat(r5.combine(r4)).isEqualTo(r6);
    assertThat(r5.combine(r5)).isEqualTo(r5);
    assertThat(r5.combine(r6)).isEqualTo(r6);
    assertThat(r5.combine(r7)).isEqualTo(r7);

    assertThat(r6.combine(r1)).isEqualTo(r6);
    assertThat(r6.combine(r2)).isEqualTo(r6);
    assertThat(r6.combine(r3)).isEqualTo(r6);
    assertThat(r6.combine(r4)).isEqualTo(r6);
    assertThat(r6.combine(r5)).isEqualTo(r6);
    assertThat(r6.combine(r6)).isEqualTo(r6);
    assertThat(r6.combine(r7)).isEqualTo(r8);

    assertThat(r7.combine(r1)).isEqualTo(r7);
    assertThat(r7.combine(r2)).isEqualTo(r7);
    assertThat(r7.combine(r3)).isEqualTo(r7);
    assertThat(r7.combine(r4)).isEqualTo(r8);
    assertThat(r7.combine(r5)).isEqualTo(r7);
    assertThat(r7.combine(r6)).isEqualTo(r8);
    assertThat(r7.combine(r7)).isEqualTo(r7);
  }

  @Test
  public void testEquals() {
    ReachableComponentVulnerabilities r1 = MissingReachableComponentVulnerabilities.INSTANCE;
    ReachableComponentVulnerabilities r2 = MissingReachableComponentVulnerabilities.INSTANCE;
    ReachableComponentVulnerabilities r3 = new PresentReachableComponentVulnerabilities(Set.of());
    ReachableComponentVulnerabilities r4 = new PresentReachableComponentVulnerabilities(Set.of());
    ReachableComponentVulnerabilities r5 = new PresentReachableComponentVulnerabilities(Set.of("v1"));
    ReachableComponentVulnerabilities r6 = new PresentReachableComponentVulnerabilities(Set.of("v1"));
    ReachableComponentVulnerabilities r7 = new PresentReachableComponentVulnerabilities(Set.of("v1", "v2"));
    ReachableComponentVulnerabilities r8 = new PresentReachableComponentVulnerabilities(Set.of("v2", "v1"));
    ReachableComponentVulnerabilities r9 = new PresentReachableComponentVulnerabilities(Set.of("v2", "v3"));
    ReachableComponentVulnerabilities r10 = new PresentReachableComponentVulnerabilities(Set.of("v3", "v2"));

    assertThat(r1.equals(null)).isFalse();
    assertThat(r1.equals(new Object())).isFalse();

    assertThat(r1.equals(r1)).isTrue();

    assertThat(r1.equals(r2)).isTrue();
    assertThat(r1.equals(r3)).isFalse();
    assertThat(r1.equals(r5)).isFalse();

    assertThat(r3.equals(r1)).isFalse();
    assertThat(r3.equals(r4)).isTrue();
    assertThat(r3.equals(r5)).isFalse();

    assertThat(r5.equals(r1)).isFalse();
    assertThat(r5.equals(r3)).isFalse();
    assertThat(r5.equals(r6)).isTrue();
    assertThat(r5.equals(r7)).isFalse();

    assertThat(r7.equals(r1)).isFalse();
    assertThat(r7.equals(r3)).isFalse();
    assertThat(r7.equals(r5)).isFalse();
    assertThat(r7.equals(r8)).isTrue();
    assertThat(r7.equals(r9)).isFalse();

    assertThat(r9.equals(r1)).isFalse();
    assertThat(r9.equals(r3)).isFalse();
    assertThat(r9.equals(r5)).isFalse();
    assertThat(r9.equals(r7)).isFalse();
    assertThat(r9.equals(r10)).isTrue();
  }

  @Test
  public void testHashCode() {
    ReachableComponentVulnerabilities r1 = MissingReachableComponentVulnerabilities.INSTANCE;
    ReachableComponentVulnerabilities r2 = MissingReachableComponentVulnerabilities.INSTANCE;
    ReachableComponentVulnerabilities r3 = new PresentReachableComponentVulnerabilities(Set.of());
    ReachableComponentVulnerabilities r4 = new PresentReachableComponentVulnerabilities(Set.of());
    ReachableComponentVulnerabilities r5 = new PresentReachableComponentVulnerabilities(Set.of("v1"));
    ReachableComponentVulnerabilities r6 = new PresentReachableComponentVulnerabilities(Set.of("v1"));
    ReachableComponentVulnerabilities r7 = new PresentReachableComponentVulnerabilities(Set.of("v1", "v2"));
    ReachableComponentVulnerabilities r8 = new PresentReachableComponentVulnerabilities(Set.of("v2", "v1"));
    ReachableComponentVulnerabilities r9 = new PresentReachableComponentVulnerabilities(Set.of("v2", "v3"));
    ReachableComponentVulnerabilities r10 = new PresentReachableComponentVulnerabilities(Set.of("v3", "v2"));

    assertThat(r1.hashCode()).isEqualTo(MissingReachableComponentVulnerabilities.INSTANCE.hashCode());
    assertThat(r1.hashCode()).isEqualTo(r1.hashCode());
    assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    assertThat(r3.hashCode()).isEqualTo(r4.hashCode());
    assertThat(r5.hashCode()).isEqualTo(r6.hashCode());
    assertThat(r7.hashCode()).isEqualTo(r8.hashCode());
    assertThat(r9.hashCode()).isEqualTo(r10.hashCode());
  }
}
