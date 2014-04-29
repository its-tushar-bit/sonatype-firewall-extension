/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class StageTypeServiceTest
    extends AbstractComponentTest
{
  @Inject
  private StageTypeService stageTypeService;

  @Inject
  //note we solely use this to call installLicense() to flush the caches
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  @Test
  public void checkStageTypes_RiskRemediation() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(),
        containsInAnyOrder(StageTypes.getById(BuildStageType.ID), StageTypes.getById(DevelopStageType.ID),
            StageTypes.getById(OperateStageType.ID), StageTypes.getById(ReleaseStageType.ID),
            StageTypes.getById(StageReleaseStageType.ID))
    );
  }

  @Test
  public void checkStageTypes_Risk() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(),
        containsInAnyOrder(StageTypes.getById(ReleaseStageType.ID)));
  }

  @Test
  public void checkStageTypes_Nexus() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(),
        containsInAnyOrder(StageTypes.getById(ReleaseStageType.ID), StageTypes.getById(StageReleaseStageType.ID)));
  }

  @Test
  public void checkStageTypes_Legacy() throws Exception {
    productLicenseManager.setProducts("");
    productLicenseManager.setEnforcementPoints(CLMEnforcementPoint.Build, CLMEnforcementPoint.Develop,
        CLMEnforcementPoint.Release, CLMEnforcementPoint.StageRelease);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(),
        containsInAnyOrder(StageTypes.getById(BuildStageType.ID), StageTypes.getById(DevelopStageType.ID),
            StageTypes.getById(ReleaseStageType.ID), StageTypes.getById(StageReleaseStageType.ID))
    );
  }
}
