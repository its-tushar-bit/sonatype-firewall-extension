/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class ThreatLevelTest
{
  @Test
  public void testFrom_mapsPolicyUiRange() {
    assertThat(ThreatLevel.from(-1)).isEqualTo(ThreatLevel.LOW);
    assertThat(ThreatLevel.from(0)).isEqualTo(ThreatLevel.LOW);
    assertThat(ThreatLevel.from(1)).isEqualTo(ThreatLevel.LOW);
    assertThat(ThreatLevel.from(2)).isEqualTo(ThreatLevel.MODERATE);
    assertThat(ThreatLevel.from(3)).isEqualTo(ThreatLevel.MODERATE);
    assertThat(ThreatLevel.from(4)).isEqualTo(ThreatLevel.SEVERE);
    assertThat(ThreatLevel.from(7)).isEqualTo(ThreatLevel.SEVERE);
    assertThat(ThreatLevel.from(8)).isEqualTo(ThreatLevel.CRITICAL);
    assertThat(ThreatLevel.from(10)).isEqualTo(ThreatLevel.CRITICAL);
    assertThat(ThreatLevel.from(15)).isEqualTo(ThreatLevel.CRITICAL);
  }

  @Test
  public void testSearchAggregationBands_coverContiguousPolicyUiRange() {
    Map<String, int[]> bands = ThreatLevel.searchAggregationBands();

    assertThat(bands.keySet()).containsExactly("low", "moderate", "severe", "critical");
    assertThat(bands.get("low")[0]).isEqualTo(Integer.MIN_VALUE);
    assertThat(bands.get("low")[1]).isEqualTo(1);
    assertThat(bands.get("moderate")).containsExactly(2, 3);
    assertThat(bands.get("severe")).containsExactly(4, 7);
    assertThat(bands.get("critical")[0]).isEqualTo(8);
    assertThat(bands.get("critical")[1]).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void testSearchAggregationBands_returnsDefensiveArrayCopies() {
    Map<String, int[]> bands = ThreatLevel.searchAggregationBands();
    int originalCriticalMin = bands.get("critical")[0];
    bands.get("critical")[0] = -999;

    Map<String, int[]> freshBands = ThreatLevel.searchAggregationBands();
    assertThat(freshBands.get("critical")[0]).isEqualTo(originalCriticalMin);
    assertThat(freshBands.get("critical")).isNotSameAs(bands.get("critical"));
  }
}
