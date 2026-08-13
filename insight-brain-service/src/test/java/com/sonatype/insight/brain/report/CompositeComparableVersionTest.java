/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompositeComparableVersionTest
{
  @Test
  public void testCompareTo_Semantic() {
    // Basic pre-release comparison
    assertThat(semantic("1.0.0-alpha").compareTo(semantic("1.0.0"))).isLessThan(0);
    assertThat(semantic("1.0.0-alpha").compareTo(semantic("1.0.0-alpha.1"))).isLessThan(0);
    assertThat(semantic("1.0.0-beta").compareTo(semantic("1.0.0-beta.2"))).isLessThan(0);
    assertThat(semantic("1.0.0-beta.2").compareTo(semantic("1.0.0-rc.1"))).isLessThan(0);
    assertThat(semantic("1.0.0-rc.1").compareTo(semantic("1.0.0"))).isLessThan(0);

    // Metadata should not affect ordering
    assertThat(semantic("1.0.0+build.1").compareTo(semantic("1.0.0"))).isEqualTo(0);
    assertThat(semantic("1.0.0-alpha+build.1").compareTo(semantic("1.0.0-alpha"))).isEqualTo(0);

    // Leading zeros
    assertThat(semantic("01.0.0").compareTo(semantic("1.0.0"))).isEqualTo(0);
    assertThat(semantic("1.01.0").compareTo(semantic("1.1.0"))).isEqualTo(0);

    // Complex pre-release versions
    assertThat(semantic("1.0.0-alpha.1").compareTo(semantic("1.0.0-alpha.beta"))).isLessThan(0);
    assertThat(semantic("1.0.0-alpha.beta").compareTo(semantic("1.0.0-beta"))).isLessThan(0);
    assertThat(semantic("1.0.0-beta").compareTo(semantic("1.0.0-beta.2"))).isLessThan(0);
    assertThat(semantic("1.0.0-beta.11").compareTo(semantic("1.0.0-rc.1"))).isLessThan(0);
  }

  @Test
  public void testCompareTo_Generic() {
    assertThat(generic("1.0.0-SNAPSHOT").compareTo(generic("1.0.0"))).isLessThan(0);
    assertThat(generic("1.0.1-RELEASE").compareTo(generic("1.0.0"))).isGreaterThan(0);
    assertThat(generic("2.0.0.CR1").compareTo(generic("2.0.0"))).isLessThan(0);
    assertThat(generic("1.0.0.Final").compareTo(generic("1.0.0"))).isEqualTo(0);
    assertThat(generic("9.0.0-rc1-SNAPSHOT").compareTo(generic("9.0.0"))).isLessThan(0);
    assertThat(generic("0.0.1-alpha").compareTo(generic("0.0.1"))).isLessThan(0);
    assertThat(generic("0.0.0").compareTo(generic("0.0.1"))).isLessThan(0);
    assertThat(generic("999.999.999").compareTo(generic("0.0.0"))).isGreaterThan(0);
    assertThat(generic("custom-version").compareTo(generic("custom-version"))).isEqualTo(0);
    assertThat(generic("custom-version-SNAPSHOT").compareTo(generic("custom-version"))).isLessThan(0);
  }

  @Test
  public void testIsPreRelease_Semantic() {
    // Standard semantic pre-release identifiers
    assertThat(semantic("1.0.0-alpha").isPreRelease()).isTrue();
    assertThat(semantic("1.0.0-alpha.1").isPreRelease()).isTrue();
    assertThat(semantic("1.0.0-beta").isPreRelease()).isTrue();
    assertThat(semantic("1.0.0-beta.2").isPreRelease()).isTrue();
    assertThat(semantic("1.0.0-rc.1").isPreRelease()).isTrue();
    assertThat(semantic("1.0.0-0.3.7").isPreRelease()).isTrue();

    // Full release (not prerelease)
    assertThat(semantic("1.0.0").isPreRelease()).isFalse();
    assertThat(semantic("2.0.0").isPreRelease()).isFalse();

    // Metadata does not imply pre-release
    assertThat(semantic("1.0.0+build.1").isPreRelease()).isFalse();
    assertThat(semantic("1.0.0-alpha+build.1").isPreRelease()).isTrue();
  }

  @Test
  public void testIsPreRelease_Generic() {
    // Maven-style snapshot
    assertThat(generic("1.0.0-SNAPSHOT").isPreRelease()).isTrue();
    assertThat(generic("1.0.1-SNAPSHOT").isPreRelease()).isTrue();

    // Variants and suffixes
    assertThat(generic("2.3.4-beta-SNAPSHOT").isPreRelease()).isTrue();
    assertThat(generic("2.3.4-rc1-SNAPSHOT").isPreRelease()).isTrue();

    // Not a snapshot
    assertThat(generic("1.0.0").isPreRelease()).isFalse();
    assertThat(generic("1.0.0-RELEASE").isPreRelease()).isFalse();
    assertThat(generic("2.0.0-beta").isPreRelease()).isFalse();
    assertThat(generic("2.0.0-SNAPSHOT-beta").isPreRelease()).isFalse();
    assertThat(generic("3.0.0-dev").isPreRelease()).isFalse();
    assertThat(generic("3.0.0-rc1").isPreRelease()).isFalse();
    assertThat(generic("1.0.0-SNAPSHOT-final").isPreRelease()).isFalse();
    assertThat(generic("1.0.0-final").isPreRelease()).isFalse();
  }

  @Test
  public void testIsPrerelease_Semantic_EdgeCases() {
    // Null and empty
    assertThat(semantic(null).isPreRelease()).isNull();
    assertThat(semantic("").isPreRelease()).isNull();

    // Semantic with metadata only
    assertThat(semantic("1.2.3+exp.sha.5114f85").isPreRelease()).isFalse();

    // Pre-release with complex labels
    assertThat(semantic("1.0.0-alpha.beta").isPreRelease()).isTrue();
    assertThat(semantic("1.0.0-x.7.z.92").isPreRelease()).isTrue();
  }

  @Test
  public void testIsPreRelease_Generic_EdgeCases() {
    // Null and empty strings
    assertThat(generic(null).isPreRelease()).isNull();
    assertThat(generic("").isPreRelease()).isNull();

    // No separators
    assertThat(generic("snapshot").isPreRelease()).isFalse();
    assertThat(generic("1snapshot").isPreRelease()).isFalse();

    // Non-standard casing
    assertThat(generic("1.0.0-SnApShOt").isPreRelease()).isFalse();
    assertThat(generic("v1.2.3-sNapShot").isPreRelease()).isFalse();
  }

  @Test
  public void testIsMajorJump() {
    // Semantic versioning jumps
    assertThat(semantic("1.0.0").isMajorJump(semantic("2.0.0"))).isTrue();
    assertThat(semantic("2.0.0").isMajorJump(semantic("1.0.0"))).isFalse();
    assertThat(semantic("2.1.0").isMajorJump(semantic("3.0.0"))).isTrue();
    assertThat(semantic("2.1.0").isMajorJump(semantic("2.2.0"))).isFalse();

    // Same major version
    assertThat(semantic("1.0.0").isMajorJump(semantic("1.1.0"))).isFalse();
    assertThat(semantic("1.2.0").isMajorJump(semantic("1.0.0"))).isFalse();

    // Pre-release or patch change – not major
    assertThat(generic("1.0.0").isMajorJump(generic("1.0.1"))).isFalse();
    assertThat(generic("1.0.0").isMajorJump(generic("1.1.0"))).isFalse();
    assertThat(generic("2.0.0-SNAPSHOT").isMajorJump(generic("3.0.0"))).isTrue();

    // Leading zero major versions
    assertThat(generic("0.9.9").isMajorJump(generic("1.0.0"))).isTrue();
    assertThat(generic("0.1.0").isMajorJump(generic("0.2.0"))).isFalse();
  }

  @Test
  public void testIsMajorJump_EdgeCases() {
    // Null and empty versions
    assertThat(generic(null).isMajorJump(generic("1.0.0"))).isNull();
    assertThat(generic("1.0.0").isMajorJump(generic(null))).isNull();
    assertThat(generic("").isMajorJump(generic("1.0.0"))).isNull();
    assertThat(generic("1.0.0").isMajorJump(generic(""))).isNull();

    // Non-numeric prefixes
    assertThat(generic("v1.2.3").isMajorJump(generic("2.0.0"))).isTrue();
    assertThat(generic("v1.2.3").isMajorJump(generic("v2.0.0"))).isTrue();
    assertThat(generic("v2.0.0").isMajorJump(generic("v1.2.3"))).isFalse();

    // Cannot parse leading number
    assertThat(generic("release").isMajorJump(generic("1.0.0"))).isNull();
    assertThat(generic("1.0.0").isMajorJump(generic("release"))).isNull();
  }

  private CompositeComparableVersion generic(String version) {
    return CompositeComparableVersion.fromGenericVersion(version);
  }

  private CompositeComparableVersion semantic(String version) {
    return CompositeComparableVersion.fromSemanticVersion(version);
  }
}
