/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.telemetry.ClientUserAgentUtil.UserAgent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientUserAgentUtilTest
{
  @Test
  public void testParse_NullEmpty() {
    assertThat(ClientUserAgentUtil.parse(null)).isNull();
    assertThat(ClientUserAgentUtil.parse("")).isNull();
  }

  @Test
  public void testParse_Invalid() {
    // missing start bracket:
    assertThat(ClientUserAgentUtil.parse(
        "Nexus_IQ_IDEA/1.0.1-01 Java 1.8.0_76-release; Mac OS X 10.11.6; IDEA IU-162)")).isNull();
    // missing end bracket:
    assertThat(ClientUserAgentUtil.parse(
        "Nexus_IQ_IDEA/1.0.1-01 (Java 1.8.0_76-release; Mac OS X 10.11.6")).isNull();
    // missing client version:
    assertThat(ClientUserAgentUtil.parse(
        "Nexus_IQ_IDEA (Java 1.8.0_76-release; Mac OS X 10.11.6; IDEA IU-162)")).isNull();
    // missing runtime version:
    assertThat(ClientUserAgentUtil.parse(
        "Nexus_IQ_IDEA/1.0.1-01 (Java; Mac OS X 10.11.6)")).isNull();
    // missing OS version:
    assertThat(ClientUserAgentUtil.parse(
        "Nexus_IQ_IDEA/1.0.1-01 (Java 1.8.0_76-release; Mac)")).isNull();
    // missing runtime space separator:
    assertThat(ClientUserAgentUtil.parse(
        "Nexus_IQ_IDEA/1.0.1-01 (Java1.8.0_76-release; Mac OS X 10.11.6; IDEA IU-162)")).isNull();
    // missing runtime/OS separator:
    assertThat(ClientUserAgentUtil.parse(
        "Nexus_IQ_IDEA/1.0.1-01 (Java 1.8.0_76-release Mac OS X 10.11.6; IDEA IU-162)")).isNull();
  }

  @Test
  public void testParse_GitLab() {
    assertUserAgent("GitLab_Nexus_IQ_CLI/1.133.0-02 (Java 1.8.0_322; Linux 5.10.76-linuxkit)",
        "GitLab_Nexus_IQ_CLI", "1.133.0-02",
        "Java", "1.8.0_322",
        "Linux", "5.10.76-linuxkit",
        null);
  }

  @Test
  public void testParse_IDEA() {
    assertUserAgent("Nexus_IQ_IDEA/1.0.1-01 (Java 1.8.0_76-release; Mac OS X 10.11.6; IDEA IU-162.1447.26)",
        "Nexus_IQ_IDEA", "1.0.1-01",
        "Java", "1.8.0_76-release",
        "Mac", "OS X 10.11.6",
        "IDEA IU-162.1447.26");
  }

  @Test
  public void testParse_Eclipse() {
    assertUserAgent("Sonatype_CLM_IDE_Eclipse/2.10.1.20160404-1434 (Java 1.8.0_92; Mac OS X 10.11.6; Eclipse 4.20)",
        "Sonatype_CLM_IDE_Eclipse", "2.10.1.20160404-1434",
        "Java", "1.8.0_92",
        "Mac", "OS X 10.11.6",
        "Eclipse 4.20");
  }

  @Test
  public void testParse_VisualStudio() {
    assertUserAgent(
        "Nexus_IQ_Visual_Studio/1.1.0 (.NET 4.0.30319.42000; Microsoft Windows NT 10.0.17134.0; Visual Studio 16.0)",
        "Nexus_IQ_Visual_Studio", "1.1.0",
        ".NET", "4.0.30319.42000",
        "Microsoft", "Windows NT 10.0.17134.0",
        "Visual Studio 16.0");
  }

  private static void assertUserAgent(
      final String userAgentString,
      final String expectedClient,
      final String expectedClientVersion,
      final String expectedRuntime,
      final String expectedRuntimeVersion,
      final String expectedOS,
      final String expectedOSVersion,
      final String expectedOther)
  {
    UserAgent userAgent = ClientUserAgentUtil.parse(userAgentString);
    assertThat(userAgent).isNotNull();
    assertThat(userAgent.client).isEqualTo(expectedClient);
    assertThat(userAgent.clientVersion).isEqualTo(expectedClientVersion);
    assertThat(userAgent.runtime).isEqualTo(expectedRuntime);
    assertThat(userAgent.runtimeVersion).isEqualTo(expectedRuntimeVersion);
    assertThat(userAgent.os).isEqualTo(expectedOS);
    assertThat(userAgent.osVersion).isEqualTo(expectedOSVersion);
    assertThat(userAgent.other).isEqualTo(expectedOther);
  }
}
