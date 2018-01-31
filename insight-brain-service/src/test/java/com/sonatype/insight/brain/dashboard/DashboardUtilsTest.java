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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
    assertThat(dashboardUtils.getStageTypes(null),
        contains(StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetStageTypes_StageTypeIdsEmpty() {
    assertThat(dashboardUtils.getStageTypes(Collections.emptySet()),
        contains(StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetStageTypes_InvalidStageTypeId() {
    try {
      dashboardUtils.getStageTypes(Collections.singleton("invalid-stage-type-id"));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: invalid-stage-type-id."));
    }
  }

  @Test
  public void testGetStageTypes_UnlicensedStageTypeId() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      dashboardUtils.getStageTypes(Collections.singleton(StageTypes.BUILD.getId()));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Current license does not support stage type: build."));
    }
  }
}
