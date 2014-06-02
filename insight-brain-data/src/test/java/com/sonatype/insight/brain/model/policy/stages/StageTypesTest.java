/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.stages;

import com.sonatype.insight.brain.model.policy.StageType;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class StageTypesTest
{

  @Test
  public void testIsIgnoredForDashboard() {
    for (StageType stageType : StageTypes.getAll()) {
      assertThat(StageTypes.isIgnoredForDashboard(stageType.getId()), is(DevelopStageType.ID.equals(stageType.getId())));
    }
  }

  @Test
  public void testGetAll_ChronologicalOrdering() {
    assertThat(
        StageTypes.getAll(),
        contains(StageTypes.DEVELOP, StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

}
