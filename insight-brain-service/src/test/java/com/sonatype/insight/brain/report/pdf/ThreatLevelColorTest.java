/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ThreatLevelColorTest
{
  @Test
  public void testGet() {
    assertThat(ThreatLevelColor.get(10)).isEqualTo(ThreatLevelColor.CRITICAL_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(9)).isEqualTo(ThreatLevelColor.CRITICAL_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(8)).isEqualTo(ThreatLevelColor.CRITICAL_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(7)).isEqualTo(ThreatLevelColor.SEVERE_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(6)).isEqualTo(ThreatLevelColor.SEVERE_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(5)).isEqualTo(ThreatLevelColor.SEVERE_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(4)).isEqualTo(ThreatLevelColor.SEVERE_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(3)).isEqualTo(ThreatLevelColor.MODERATE_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(2)).isEqualTo(ThreatLevelColor.MODERATE_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(1)).isEqualTo(ThreatLevelColor.LOW_THREAT_COLOR);
    assertThat(ThreatLevelColor.get(0)).isEqualTo(ThreatLevelColor.NO_THREAT_COLOR);
  }

  @Test
  public void testGet_UnknownThreatLevel() {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> ThreatLevelColor.get(11))
        .withMessage("Unknown threat level 11.");
  }
}
