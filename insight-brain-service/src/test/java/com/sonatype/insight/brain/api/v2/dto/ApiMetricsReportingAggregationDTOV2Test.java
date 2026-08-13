/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.EnumMap;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.utils.ThreatLevel;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.utils.ThreatLevel.CRITICAL;
import static com.sonatype.insight.brain.utils.ThreatLevel.LOW;
import static com.sonatype.insight.brain.utils.ThreatLevel.MODERATE;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiMetricsReportingAggregationDTOV2Test
{
  @Test
  public void testConstructor() {
    Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> discovered = emptyMap();
    Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> fixed = emptyMap();
    Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> waived = emptyMap();
    Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> open = emptyMap();

    discovered.get(SECURITY).put(LOW, 1);
    discovered.get(LICENSE).put(MODERATE, 5);
    fixed.get(QUALITY).put(SEVERE, 2);
    waived.get(OTHER).put(CRITICAL, 100);
    open.get(SECURITY).put(LOW, 2);
    open.get(LICENSE).put(MODERATE, 10);

    ApiMetricsReportingAggregationDTOV2 dto = new ApiMetricsReportingAggregationDTOV2( //
        "2017-10-01", //
        20L, 500L, 2L, 6L, //
        discovered, //
        fixed, //
        waived, //
        open, //
        10);

    assertThat(dto.timePeriodStart).isEqualTo("2017-10-01");

    assertThat(dto.mttrLowThreat).isEqualTo(20);
    assertThat(dto.mttrModerateThreat).isEqualTo(500);
    assertThat(dto.mttrSevereThreat).isEqualTo(2);
    assertThat(dto.mttrCriticalThreat).isEqualTo(6);

    assertThat(dto.discoveredCounts.get(SECURITY).get(LOW)).isEqualTo(1);
    assertThat(dto.discoveredCounts.get(SECURITY).get(MODERATE)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(SECURITY).get(SEVERE)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(SECURITY).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(LICENSE).get(LOW)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(LICENSE).get(MODERATE)).isEqualTo(5);
    assertThat(dto.discoveredCounts.get(LICENSE).get(SEVERE)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(LICENSE).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(QUALITY).get(LOW)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(QUALITY).get(MODERATE)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(QUALITY).get(SEVERE)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(QUALITY).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(OTHER).get(LOW)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(OTHER).get(MODERATE)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(OTHER).get(SEVERE)).isEqualTo(0);
    assertThat(dto.discoveredCounts.get(OTHER).get(CRITICAL)).isEqualTo(0);

    assertThat(dto.fixedCounts.get(SECURITY).get(LOW)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(SECURITY).get(MODERATE)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(SECURITY).get(SEVERE)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(SECURITY).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(LICENSE).get(LOW)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(LICENSE).get(MODERATE)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(LICENSE).get(SEVERE)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(LICENSE).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(QUALITY).get(LOW)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(QUALITY).get(MODERATE)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(QUALITY).get(SEVERE)).isEqualTo(2);
    assertThat(dto.fixedCounts.get(QUALITY).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(OTHER).get(LOW)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(OTHER).get(MODERATE)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(OTHER).get(SEVERE)).isEqualTo(0);
    assertThat(dto.fixedCounts.get(OTHER).get(CRITICAL)).isEqualTo(0);

    assertThat(dto.waivedCounts.get(SECURITY).get(LOW)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(SECURITY).get(MODERATE)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(SECURITY).get(SEVERE)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(SECURITY).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(LICENSE).get(LOW)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(LICENSE).get(MODERATE)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(LICENSE).get(SEVERE)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(LICENSE).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(QUALITY).get(LOW)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(QUALITY).get(MODERATE)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(QUALITY).get(SEVERE)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(QUALITY).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(OTHER).get(LOW)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(OTHER).get(MODERATE)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(OTHER).get(SEVERE)).isEqualTo(0);
    assertThat(dto.waivedCounts.get(OTHER).get(CRITICAL)).isEqualTo(100);

    assertThat(dto.openCountsAtTimePeriodEnd.get(SECURITY).get(LOW)).isEqualTo(2);
    assertThat(dto.openCountsAtTimePeriodEnd.get(SECURITY).get(MODERATE)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(SECURITY).get(SEVERE)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(SECURITY).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(LICENSE).get(LOW)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(LICENSE).get(MODERATE)).isEqualTo(10);
    assertThat(dto.openCountsAtTimePeriodEnd.get(LICENSE).get(SEVERE)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(LICENSE).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(QUALITY).get(LOW)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(QUALITY).get(MODERATE)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(QUALITY).get(SEVERE)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(QUALITY).get(CRITICAL)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(OTHER).get(LOW)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(OTHER).get(MODERATE)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(OTHER).get(SEVERE)).isEqualTo(0);
    assertThat(dto.openCountsAtTimePeriodEnd.get(OTHER).get(CRITICAL)).isEqualTo(0);

    assertThat(dto.evaluationCount).isEqualTo(10);
  }

  private Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> emptyMap() {
    Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> retval = new EnumMap<>(PolicyThreatCategory.class);

    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      retval.put(category, new EnumMap<>(ThreatLevel.class));

      for (ThreatLevel level : ThreatLevel.values()) {
        retval.get(category).put(level, 0);
      }
    }

    return retval;
  }
}
