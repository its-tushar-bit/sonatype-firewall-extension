/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.stages;

import com.sonatype.insight.brain.model.policy.StageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StageTypesTest
{
  @Test
  public void testIsIgnoredForDashboard() {
    for (StageType stageType : StageTypes.getAll()) {
      switch (stageType.getId()) {
        case DevelopStageType.ID:
        case ProxyStageType.ID:
        case ComplianceStageType.ID:
          assertThat(StageTypes.isIgnoredForDashboard(stageType.getId())).isTrue();
          break;
        default:
          assertThat(StageTypes.isIgnoredForDashboard(stageType.getId())).isFalse();
      }
    }
  }

  @Test
  public void testIsIgnoredForPolicyViolationAggregation() {
    for (StageType stageType : StageTypes.getAll()) {
      switch (stageType.getId()) {
        case DevelopStageType.ID:
        case ComplianceStageType.ID:
        case ProxyStageType.ID:
          assertThat(StageTypes.isIgnoredForPolicyViolationAggregation(stageType.getId())).isTrue();
          break;
        default:
          assertThat(StageTypes.isIgnoredForPolicyViolationAggregation(stageType.getId())).isFalse();
      }
    }
  }

  @Test
  public void testGetAll_ChronologicalOrdering() {
    assertThat(StageTypes.getAll()).containsExactly(StageTypes.PROXY, StageTypes.HOSTED, StageTypes.DEVELOP,
        StageTypes.SOURCE, StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE,
        StageTypes.COMPLIANCE);
  }

  @Test
  public void testHostedStageTypeRegistered() {
    assertThat(StageTypes.getById(HostedStageType.ID)).isSameAs(StageTypes.HOSTED);
    assertThat(StageTypes.HOSTED.getId()).isEqualTo("hosted");
    assertThat(StageTypes.HOSTED.getName()).isEqualTo("Hosted");
  }

  @Test
  public void testHostedStageTypeNotIgnoredByDefault() {
    // Hosted enforcement violations appear on dashboards and aggregations by default.
    // If product requirements later change this, adjust both StageTypes and this assertion.
    assertThat(StageTypes.isIgnoredForDashboard(HostedStageType.ID)).isFalse();
    assertThat(StageTypes.isIgnoredForPolicyViolationAggregation(HostedStageType.ID)).isFalse();
  }
}
