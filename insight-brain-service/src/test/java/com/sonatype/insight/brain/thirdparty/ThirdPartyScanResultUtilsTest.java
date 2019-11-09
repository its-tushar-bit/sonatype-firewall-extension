/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyScanResultUtilsTest
{
  @Test
  public void testGetVulnerabilitySourceFromReference() {
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("CVE-1234-567")).isEqualTo("CVE");
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("CVE22-1234")).isEqualTo("CVE");
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("CVE")).isEqualTo("CVE");

    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("123")).isEqualTo("123");
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("0123456789ABC")).isEqualTo("0123456789");
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference("")).isNull();
    assertThat(ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference(null)).isNull();
  }

  @Test
  public void testHash() {
    assertThat(ThirdPartyScanResultUtils.hash("pypi:django:1.11.1")).isEqualTo("41d44bac96b8c0e4f78c");
    assertThat(ThirdPartyScanResultUtils.hash(null)).isEqualTo("da39a3ee5e6b4b0d3255");
  }
}
