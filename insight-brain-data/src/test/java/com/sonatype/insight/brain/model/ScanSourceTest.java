/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanSourceTest
{
  @Test
  public void testFromHeader_recognizedValues() {
    assertThat(ScanSource.fromHeader("BROWSER_EXTENSION")).isEqualTo(ScanSource.BROWSER_EXTENSION);
    assertThat(ScanSource.fromHeader("FIREWALL_PROXY")).isEqualTo(ScanSource.FIREWALL_PROXY);
    assertThat(ScanSource.fromHeader("IDE")).isEqualTo(ScanSource.IDE);
    assertThat(ScanSource.fromHeader("CI")).isEqualTo(ScanSource.CI);
  }

  @Test
  public void testFromHeader_isCaseInsensitiveAndTrimmed() {
    assertThat(ScanSource.fromHeader("browser_extension")).isEqualTo(ScanSource.BROWSER_EXTENSION);
    assertThat(ScanSource.fromHeader("Browser_Extension")).isEqualTo(ScanSource.BROWSER_EXTENSION);
    assertThat(ScanSource.fromHeader("  BROWSER_EXTENSION  ")).isEqualTo(ScanSource.BROWSER_EXTENSION);
  }

  @Test
  public void testFromHeader_absentOrBlankDefaultsToFirewallProxy() {
    assertThat(ScanSource.fromHeader(null)).isEqualTo(ScanSource.FIREWALL_PROXY);
    assertThat(ScanSource.fromHeader("")).isEqualTo(ScanSource.FIREWALL_PROXY);
    assertThat(ScanSource.fromHeader("   ")).isEqualTo(ScanSource.FIREWALL_PROXY);
  }

  @Test
  public void testFromHeader_unrecognizedDefaultsToFirewallProxyAndDoesNotThrow() {
    assertThat(ScanSource.fromHeader("CARRIER_PIGEON")).isEqualTo(ScanSource.FIREWALL_PROXY);
    assertThat(ScanSource.fromHeader("'; DROP TABLE policy_waiver_request; --"))
        .isEqualTo(ScanSource.FIREWALL_PROXY);
  }
}
