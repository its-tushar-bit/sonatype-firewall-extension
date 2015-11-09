/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
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

  @After
  public void cleanup() throws Exception {
    productLicenseManager.reset();
    clmLicenseManager.installLicense(null);
  }

  @Test
  public void testGetLicensedStageTypes_RiskRemediation() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(), contains(StageTypes.PROXY, StageTypes.DEVELOP,
        StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetLicensedStageTypes_Risk() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(), contains(StageTypes.RELEASE));
  }

  @Test
  public void testGetLicensedStageTypes_Nexus() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(),
        contains(StageTypes.PROXY, StageTypes.STAGE_RELEASE, StageTypes.RELEASE));
  }

  @Test
  public void testGetLicensedStageTypes_Legacy() throws Exception {
    productLicenseManager.setProducts("");
    productLicenseManager.setEnforcementPoints(CLMEnforcementPoint.Build, CLMEnforcementPoint.Develop,
        CLMEnforcementPoint.Release, CLMEnforcementPoint.StageRelease);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(),
        contains(StageTypes.DEVELOP, StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE,
            StageTypes.OPERATE));
  }

  @Test
  public void testGetLicensedStageTypes_LegacyNexus() throws Exception {
    productLicenseManager.setProducts("");
    productLicenseManager.setEnforcementPoints(CLMEnforcementPoint.Release, CLMEnforcementPoint.StageRelease);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(),
        contains(StageTypes.STAGE_RELEASE, StageTypes.RELEASE));
  }

  @Test
  public void testGetLicensedStageTypes_RiskRemediation_ContextAll() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.ALL_CONTEXT), contains(StageTypes.PROXY,
        StageTypes.DEVELOP, StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetLicensedStageTypes_RiskRemediation_ContextDashboard() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT),
        contains(StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetLicensedStageTypes_RiskRemediation_ContextCi() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.CI_CONTEXT),
        contains(StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetLicensedStageTypes_RiskRemediation_ContextCli() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.CLI_CONTEXT),
        contains(StageTypes.DEVELOP, StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE,
            StageTypes.OPERATE));
  }

  @Test
  public void testGetLicensedStageTypes_RiskRemediation_ContextQa() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.CI_CONTEXT),
        contains(StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetLicensedStageTypes_RiskRemediation_ContextRm() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.CI_CONTEXT),
        contains(StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE));
  }

  @Test
  public void testGetLicensedStageTypes_RiskRemediation_ContextMaven() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    clmLicenseManager.installLicense(null);

    assertThat(stageTypeService.getLicensedStageTypes(StageTypeService.MAVEN_CONTEXT),
        contains(StageTypes.DEVELOP, StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE,
            StageTypes.OPERATE));
  }
}
