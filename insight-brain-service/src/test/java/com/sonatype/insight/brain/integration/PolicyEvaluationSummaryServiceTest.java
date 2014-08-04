/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import com.google.inject.Inject;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
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
    super.configure(binder);
    licenseManager = mock(CLMLicenseManager.class);
    binder.bind(CLMLicenseManager.class).toInstance(licenseManager);
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId() throws Exception {
    when(licenseManager.hasQuality()).thenReturn(true);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");
    PolicyEvaluation policyEvaluation = tempEntity
        .newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application.getId(), "test-policy");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    PolicyEvaluationSummary policyEvaluationSummary = policyEvaluationSummaryService
        .getEvaluationSummaryByApplicationId(application.getId(), stage);

    assertThat(policyEvaluationSummary, notNullValue());
    assertThat(policyEvaluationSummary.getReportUrl(),
        is("ui/links/application/" + application.getPublicId() + "/report/" + scanId));
    assertThat(policyEvaluationSummary.getAffectedComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getCriticalComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getModerateComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getSevereComponentCount(), is(1));
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_NoApplication() throws Exception {
    when(licenseManager.hasQuality()).thenReturn(true);

    Stage stage = new Stage(Stage.ID_BUILD);
    String appId = "invalidAppId";
    try {
      policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(appId, stage);
      fail("Expected exception " + NotFoundException.class.getSimpleName());
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Could not find an application with ID " + appId + "."));
    }
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_NoPolicyEvaluationAvailable() throws Exception {
    when(licenseManager.hasQuality()).thenReturn(true);

    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    PolicyEvaluationSummary policyEvaluationSummary = policyEvaluationSummaryService
        .getEvaluationSummaryByApplicationId(application.getId(), stage);
    assertThat(policyEvaluationSummary, nullValue());
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_Unlicensed() throws Exception {
    when(licenseManager.hasQuality()).thenReturn(false);

    Stage stage = new Stage(Stage.ID_BUILD);
    Application application = tempEntity.newApplicationWithParent("test-app");
    try {
      policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(application.getId(), stage);
      fail("Expected exception " + InvalidLicenseException.class.getSimpleName());
    }
    catch (InvalidLicenseException e) {
      assertThat(e.getMessage(), is("Invalid license for the Quality feature"));
    }
  }
}
