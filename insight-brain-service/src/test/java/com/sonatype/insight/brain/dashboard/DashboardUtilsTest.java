/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DashboardUtilsTest
    extends AbstractComponentTest
{
  @Inject
  private DashboardUtils dashboardUtils;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testGetStageTypes_StageTypeIdsNull() {
    assertThat(dashboardUtils.getStageTypes(null)).containsExactly(StageTypes.SOURCE, StageTypes.BUILD,
        StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE);
  }

  @Test
  public void testGetStageTypes_StageTypeIdsEmpty() {
    assertThat(dashboardUtils.getStageTypes(Collections.emptySet())).containsExactly(StageTypes.SOURCE,
        StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE);
  }

  @Test
  public void testGetStageTypes_InvalidStageTypeId() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dashboardUtils.getStageTypes(Collections.singleton("invalid-stage-type-id"));
    }).withMessage("Invalid stage type: invalid-stage-type-id.");
  }

  @Test
  public void testGetStageTypes_UnlicensedStageTypeId() throws Exception {
    testProductLicense.setStageTypes(StageTypes.RELEASE);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dashboardUtils.getStageTypes(Collections.singleton(StageTypes.BUILD.getId()));
    }).withMessage("Current license does not support stage type: build.");
  }
}
