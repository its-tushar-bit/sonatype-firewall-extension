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

import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.utils.ThreatLevel.CRITICAL;
import static com.sonatype.insight.brain.utils.ThreatLevel.LOW;
import static com.sonatype.insight.brain.utils.ThreatLevel.MODERATE;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ApiMetricsReportingAggregationDTOV2Test
{
  @Test
  public void testConstructor() {
    Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> discovered = emptyMap();
    Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> fixed = emptyMap();
    Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> waived = emptyMap();

    discovered.get(SECURITY).put(LOW, 1);
    discovered.get(LICENSE).put(MODERATE, 5);
    fixed.get(QUALITY).put(SEVERE, 2);
    waived.get(OTHER).put(CRITICAL, 100);

    ApiMetricsReportingAggregationDTOV2 dto = new ApiMetricsReportingAggregationDTOV2( //
        "2017-10-01", //
        20L, 500L, 2L, 6L, //
        discovered, //
        fixed, //
        waived, //
        10, //
        1, 2, 3, 4);

    assertThat(dto.timePeriodStart, is("2017-10-01"));

    assertThat(dto.mttrLowThreat, is(20L));
    assertThat(dto.mttrModerateThreat, is(500L));
    assertThat(dto.mttrSevereThreat, is(2L));
    assertThat(dto.mttrCriticalThreat, is(6L));

    assertThat(dto.discoveredCounts.get(SECURITY).get(LOW), is(1));
    assertThat(dto.discoveredCounts.get(SECURITY).get(MODERATE), is(0));
    assertThat(dto.discoveredCounts.get(SECURITY).get(SEVERE), is(0));
    assertThat(dto.discoveredCounts.get(SECURITY).get(CRITICAL), is(0));
    assertThat(dto.discoveredCounts.get(LICENSE).get(LOW), is(0));
    assertThat(dto.discoveredCounts.get(LICENSE).get(MODERATE), is(5));
    assertThat(dto.discoveredCounts.get(LICENSE).get(SEVERE), is(0));
    assertThat(dto.discoveredCounts.get(LICENSE).get(CRITICAL), is(0));
    assertThat(dto.discoveredCounts.get(QUALITY).get(LOW), is(0));
    assertThat(dto.discoveredCounts.get(QUALITY).get(MODERATE), is(0));
    assertThat(dto.discoveredCounts.get(QUALITY).get(SEVERE), is(0));
    assertThat(dto.discoveredCounts.get(QUALITY).get(CRITICAL), is(0));
    assertThat(dto.discoveredCounts.get(OTHER).get(LOW), is(0));
    assertThat(dto.discoveredCounts.get(OTHER).get(MODERATE), is(0));
    assertThat(dto.discoveredCounts.get(OTHER).get(SEVERE), is(0));
    assertThat(dto.discoveredCounts.get(OTHER).get(CRITICAL), is(0));

    assertThat(dto.fixedCounts.get(SECURITY).get(LOW), is(0));
    assertThat(dto.fixedCounts.get(SECURITY).get(MODERATE), is(0));
    assertThat(dto.fixedCounts.get(SECURITY).get(SEVERE), is(0));
    assertThat(dto.fixedCounts.get(SECURITY).get(CRITICAL), is(0));
    assertThat(dto.fixedCounts.get(LICENSE).get(LOW), is(0));
    assertThat(dto.fixedCounts.get(LICENSE).get(MODERATE), is(0));
    assertThat(dto.fixedCounts.get(LICENSE).get(SEVERE), is(0));
    assertThat(dto.fixedCounts.get(LICENSE).get(CRITICAL), is(0));
    assertThat(dto.fixedCounts.get(QUALITY).get(LOW), is(0));
    assertThat(dto.fixedCounts.get(QUALITY).get(MODERATE), is(0));
    assertThat(dto.fixedCounts.get(QUALITY).get(SEVERE), is(2));
    assertThat(dto.fixedCounts.get(QUALITY).get(CRITICAL), is(0));
    assertThat(dto.fixedCounts.get(OTHER).get(LOW), is(0));
    assertThat(dto.fixedCounts.get(OTHER).get(MODERATE), is(0));
    assertThat(dto.fixedCounts.get(OTHER).get(SEVERE), is(0));
    assertThat(dto.fixedCounts.get(OTHER).get(CRITICAL), is(0));

    assertThat(dto.waivedCounts.get(SECURITY).get(LOW), is(0));
    assertThat(dto.waivedCounts.get(SECURITY).get(MODERATE), is(0));
    assertThat(dto.waivedCounts.get(SECURITY).get(SEVERE), is(0));
    assertThat(dto.waivedCounts.get(SECURITY).get(CRITICAL), is(0));
    assertThat(dto.waivedCounts.get(LICENSE).get(LOW), is(0));
    assertThat(dto.waivedCounts.get(LICENSE).get(MODERATE), is(0));
    assertThat(dto.waivedCounts.get(LICENSE).get(SEVERE), is(0));
    assertThat(dto.waivedCounts.get(LICENSE).get(CRITICAL), is(0));
    assertThat(dto.waivedCounts.get(QUALITY).get(LOW), is(0));
    assertThat(dto.waivedCounts.get(QUALITY).get(MODERATE), is(0));
    assertThat(dto.waivedCounts.get(QUALITY).get(SEVERE), is(0));
    assertThat(dto.waivedCounts.get(QUALITY).get(CRITICAL), is(0));
    assertThat(dto.waivedCounts.get(OTHER).get(LOW), is(0));
    assertThat(dto.waivedCounts.get(OTHER).get(MODERATE), is(0));
    assertThat(dto.waivedCounts.get(OTHER).get(SEVERE), is(0));
    assertThat(dto.waivedCounts.get(OTHER).get(CRITICAL), is(100));

    assertThat(dto.openCountSecurity, is(1));
    assertThat(dto.openCountLicense, is(2));
    assertThat(dto.openCountQuality, is(3));
    assertThat(dto.openCountOther, is(4));

    assertThat(dto.evaluationCount, is(10));
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
