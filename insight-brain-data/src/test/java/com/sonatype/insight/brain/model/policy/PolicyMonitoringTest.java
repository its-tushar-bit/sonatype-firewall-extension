/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.policy.stages.HostedStageType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyMonitoringTest
{
  @Test
  public void testInvalidStageTypeId_Constructor() {
    assertThatThrownBy(() -> new PolicyMonitoring("fakeOwnerId", "fakeStageTypeId"))
        .isInstanceOf(InvalidStageException.class)
        .hasMessage("Invalid stage id=fakeStageTypeId");
  }

  @Test
  public void testInvalidStageTypeId_Setter() {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring();
    assertThatThrownBy(() -> policyMonitoring.setStageTypeId("fakeStageTypeId"))
        .isInstanceOf(InvalidStageException.class)
        .hasMessage("Invalid stage id=fakeStageTypeId");
  }

  @Test
  public void testHostedStageTypeId_Constructor_Accepted() {
    // Positive coverage for the OR-branch added in CLM-39870 that accepts HostedStageType.ID.
    // The existing tests cover the rejection path only; this guards against regressions if
    // the OR-branch is dropped during a merge conflict.
    PolicyMonitoring policyMonitoring = new PolicyMonitoring("ownerId", HostedStageType.ID);
    assertThat(policyMonitoring.getStageTypeId()).isEqualTo(HostedStageType.ID);
  }

  @Test
  public void testHostedStageTypeId_Setter_Accepted() {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring();
    policyMonitoring.setStageTypeId(HostedStageType.ID);
    assertThat(policyMonitoring.getStageTypeId()).isEqualTo(HostedStageType.ID);
  }
}
