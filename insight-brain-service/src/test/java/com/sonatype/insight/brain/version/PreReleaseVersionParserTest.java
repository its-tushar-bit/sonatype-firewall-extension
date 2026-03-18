/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.version;

import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PreReleaseVersionParserTest
{
  private static final List<String> PRE_RELEASE_VERSIONS = List.of(
      "1.2.3alpha",
      "0.0.17-alpha.0",
      "ALPHA123",
      "alpha-123",
      "1.2.3beta",
      "1.2.3-beta.4",
      "BETA123",
      "beta-123",
      "1.2.3.4-milestone",
      "1.1.3.4-RC8",
      "1.1.3.4-CR.1",
      "1.2.3.rc-1",
      "1.2cr",
      "1.2.3-rc+build.1",
      "1.2-SNAPSHOT",
      "2.3.2.eap",
      "2.0-eap2",
      "5.4gamma",
      "6.0-GAMMA",
      "1.2.3-DEV",
      "1.2.3.4.dev1",
      "1.0.dev-1",
      "1.2-preview",
      "1.1.pre-release",
      "1.4.1prerel",
      "demo-1.2.3",
      "5demo-1.2.3",
      "5demo-1.2.3alpha",
      "2.2.1.0_FC",
      "0.2-fc1",
      "feature-505-pom-d693030",
      "feature_1850_01",
      "1.2-a1",
      "1.2-b3",
      "1.2-m1",
      "1.2.3-canary",
      "2.0.0-canary.1",
      "3.1.4-canary-2",
      "1.2.3-nightly",
      "4.0.0-nightly.1",
      "6.0.2-nightly-2",
      "1.2.3-ea",
      "2.0.0-ea1",
      "3.1.4-ea-2");

  private static final List<String> RELEASE_VERSIONS = List.of(
      "1",
      "1.0",
      "1.2.3",
      "1.2.3.4",
      "1.0.2-v20150114",
      "4.3.2.Final",
      "4.3.2-release",
      "10.0.6+7-e2ba6752",
      "0.2.4.23-1~deb7u1",
      "3.0-JBoss-4.0.2_03",
      "12.1.2-2-1",
      "1.1.3.3_min",
      "RELEASE112",
      "1.2.sp1",
      "3.0.0-SP",
      "2.6.1.ga",
      "1.0-G",
      "2.9.8-android",
      "1.0-core",
      "2.0.CORE",
      "1.2.3-marcos-release",
      "1.2.1+102-efcf0a38",
      "2.3.0-1-cfcb3657");

  @Test
  public void testIsPreReleaseVersion_MatchPreReleaseVersions() {
    PRE_RELEASE_VERSIONS.forEach(version -> {
      assertThat(PreReleaseVersionParser.isPreReleaseVersion(version)).isTrue();
      assertThat(PreReleaseVersionParser.isStable(version)).isFalse();
    });
  }

  @Test
  public void testIsPreReleaseVersion_DoNotMatchReleaseVersions() {
    RELEASE_VERSIONS.forEach(version -> {
      assertThat(PreReleaseVersionParser.isPreReleaseVersion(version)).isFalse();
      assertThat(PreReleaseVersionParser.isStable(version)).isTrue();
    });
  }

  @Test
  public void testIsPreReleaseVersion_NullVersion() {
    assertThatThrownBy(() -> PreReleaseVersionParser.isPreReleaseVersion(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Version cannot be null or empty");

    assertThatThrownBy(() -> PreReleaseVersionParser.isPreReleaseVersion(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Version cannot be null or empty");
  }
}
