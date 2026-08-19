/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO.RawThreatLevelCount;
import com.sonatype.insight.brain.utils.ThreatLevel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardViolationThreatBandMapperTest
{
  private final DashboardViolationThreatBandMapper underTest = new DashboardViolationThreatBandMapper();

  @Test
  public void levelZeroMapsToLow() {
    assertThat(underTest.map(List.of(new RawThreatLevelCount((short) 0, 1))))
        .containsEntry("low", 1L);
  }

  @Test
  public void boundariesMatchThreatLevelSearchAggregationBands() {
    Map<String, Long> result = underTest.map(List.of(
        new RawThreatLevelCount((short) 0, 1),
        new RawThreatLevelCount((short) 1, 1),
        new RawThreatLevelCount((short) 2, 1),
        new RawThreatLevelCount((short) 3, 1),
        new RawThreatLevelCount((short) 4, 1),
        new RawThreatLevelCount((short) 7, 1),
        new RawThreatLevelCount((short) 8, 1),
        new RawThreatLevelCount((short) 10, 1)));

    assertThat(result)
        .containsEntry("low", 2L)
        .containsEntry("moderate", 2L)
        .containsEntry("severe", 2L)
        .containsEntry("critical", 2L);
    assertThat(result.keySet()).containsExactlyElementsOf(ThreatLevel.searchAggregationBands().keySet());
  }

  @Test
  public void totalEqualsSumOfFourBands() {
    Map<String, Long> result = underTest.map(List.of(
        new RawThreatLevelCount((short) 0, 2),
        new RawThreatLevelCount((short) 4, 3),
        new RawThreatLevelCount((short) 10, 5)));

    assertThat(result.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(10L);
  }

  @Test
  public void everyShortThreatLevelMapsToExactlyOneBand() {
    Map<String, Long> result = underTest.map(List.of(
        new RawThreatLevelCount(Short.MIN_VALUE, 1),
        new RawThreatLevelCount(Short.MAX_VALUE, 2)));

    assertThat(result.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(3L);
    assertThat(result.get("low") + result.get("moderate") + result.get("severe") + result.get("critical"))
        .isEqualTo(3L);
  }
}
