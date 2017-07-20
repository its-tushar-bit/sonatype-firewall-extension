/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.aggregation;

import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;

import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.Matchers.is;

public class PolicyViolationResolutionStateTest
{
  @Test
  public void testSetStageTypeById() {
    PolicyViolationResolutionState resolutionState = new PolicyViolationResolutionState();

    // sanity check
    assertThat(resolutionState.getDevelopStageType(), is(false));

    // test one-arg method
    resolutionState.setStageTypeById(DevelopStageType.ID);
    assertThat(resolutionState.getDevelopStageType(), is(true));
    resolutionState.setStageTypeById(BuildStageType.ID);
    assertThat(resolutionState.getBuildStageType(), is(true));
    resolutionState.setStageTypeById(StageReleaseStageType.ID);
    assertThat(resolutionState.getStageReleaseStageType(), is(true));
    resolutionState.setStageTypeById(ReleaseStageType.ID);
    assertThat(resolutionState.getReleaseStageType(), is(true));
    resolutionState.setStageTypeById(OperateStageType.ID);
    assertThat(resolutionState.getOperateStageType(), is(true));
    resolutionState.setStageTypeById(ProxyStageType.ID);
    assertThat(resolutionState.getProxyStageType(), is(true));
    try {
      resolutionState.setStageTypeById("asdf");
      fail();
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Unknown stageType"));
    }

    // test 2-arg method setting back to false
    resolutionState.setStageTypeById(DevelopStageType.ID, false);
    assertThat(resolutionState.getDevelopStageType(), is(false));
    resolutionState.setStageTypeById(BuildStageType.ID, false);
    assertThat(resolutionState.getBuildStageType(), is(false));
    resolutionState.setStageTypeById(StageReleaseStageType.ID, false);
    assertThat(resolutionState.getStageReleaseStageType(), is(false));
    resolutionState.setStageTypeById(ReleaseStageType.ID, false);
    assertThat(resolutionState.getReleaseStageType(), is(false));
    resolutionState.setStageTypeById(OperateStageType.ID, false);
    assertThat(resolutionState.getOperateStageType(), is(false));
    resolutionState.setStageTypeById(ProxyStageType.ID, false);
    assertThat(resolutionState.getProxyStageType(), is(false));
    try {
      resolutionState.setStageTypeById("asdf", false);
      fail();
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Unknown stageType"));
    }
  }

  @Test
  public void testIsClearedInAllStages() {
    PolicyViolationResolutionState resolutionState = new PolicyViolationResolutionState();

    assertThat(resolutionState.isClearedInAllStages(), is(true));

    resolutionState.setDevelopStageType(true);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setBuildStageType(true);
    resolutionState.setDevelopStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setStageReleaseStageType(true);
    resolutionState.setBuildStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setReleaseStageType(true);
    resolutionState.setStageReleaseStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setOperateStageType(true);
    resolutionState.setReleaseStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setProxyStageType(true);
    resolutionState.setOperateStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(false));

    resolutionState.setProxyStageType(false);
    assertThat(resolutionState.isClearedInAllStages(), is(true));

    resolutionState.setDevelopStageType(true);
    resolutionState.setBuildStageType(true);
    resolutionState.setStageReleaseStageType(true);
    resolutionState.setReleaseStageType(true);
    resolutionState.setOperateStageType(true);
    resolutionState.setProxyStageType(true);
    assertThat(resolutionState.isClearedInAllStages(), is(false));
  }
}
