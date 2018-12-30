/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DashboardUtilsTest
    extends InjectedTest
{
  @Inject
  private DashboardUtils dashboardUtils;

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  @Test
  public void testGetStageTypes_StageTypeIdsNull() {
    assertThat(dashboardUtils.getStageTypes(null)).containsExactly(StageTypes.BUILD, StageTypes.STAGE_RELEASE,
        StageTypes.RELEASE, StageTypes.OPERATE);
  }

  @Test
  public void testGetStageTypes_StageTypeIdsEmpty() {
    assertThat(dashboardUtils.getStageTypes(Collections.emptySet())).containsExactly(StageTypes.BUILD,
        StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE);
  }

  @Test
  public void testGetStageTypes_InvalidStageTypeId() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dashboardUtils.getStageTypes(Collections.singleton("invalid-stage-type-id"));
    }).withMessage("Invalid stage type: invalid-stage-type-id.");
  }

  @Test
  public void testGetStageTypes_UnlicensedStageTypeId() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dashboardUtils.getStageTypes(Collections.singleton(StageTypes.BUILD.getId()));
    }).withMessage("Current license does not support stage type: build.");
  }
}
