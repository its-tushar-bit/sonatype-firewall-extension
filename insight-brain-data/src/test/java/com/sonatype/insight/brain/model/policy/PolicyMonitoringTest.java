/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import org.junit.Test;

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
}
