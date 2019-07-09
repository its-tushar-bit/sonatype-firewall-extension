/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyEvaluationSummaryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyEvaluationSummaryService policyEvaluationSummaryService;

  private CLMLicenseManager licenseManager;

  @Override
  public void configure(Binder binder) {
    licenseManager = mock(CLMLicenseManager.class);
    binder.bind(CLMLicenseManager.class).toInstance(licenseManager);
    super.configure(binder);
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId() throws Exception {
    when(licenseManager.hasFeature(LicensedFeature.QUALITY)).thenReturn(true);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(),
        scanId);
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    PolicyEvaluationSummary policyEvaluationSummary = policyEvaluationSummaryService
        .getEvaluationSummaryByApplicationId(application.getId(), stage);

    assertThat(policyEvaluationSummary).isNotNull();
    assertThat(policyEvaluationSummary.getReportUrl())
        .isEqualTo("ui/links/application/" + application.getPublicId() + "/report/" + scanId);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_NoApplication() throws Exception {
    when(licenseManager.hasFeature(LicensedFeature.QUALITY)).thenReturn(true);

    Stage stage = new Stage(Stage.ID_BUILD);
    String appId = "invalidAppId";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(appId, stage);
    }).withMessageContaining("not find an application with ID " + appId);
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_NoPolicyEvaluationAvailable() throws Exception {
    when(licenseManager.hasFeature(LicensedFeature.QUALITY)).thenReturn(true);

    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    PolicyEvaluationSummary policyEvaluationSummary = policyEvaluationSummaryService
        .getEvaluationSummaryByApplicationId(application.getId(), stage);
    assertThat(policyEvaluationSummary).isNull();
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_Unlicensed() throws Exception {
    when(licenseManager.hasFeature(LicensedFeature.QUALITY)).thenReturn(false);

    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(application.getId(), stage);
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }
}
