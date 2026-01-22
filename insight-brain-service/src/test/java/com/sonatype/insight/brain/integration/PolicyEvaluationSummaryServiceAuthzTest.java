/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

public class PolicyEvaluationSummaryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PolicyEvaluationSummaryService policyEvaluationSummaryService;

  private final Stage stage = new Stage(Stage.ID_BUILD);

  @Before
  public void setup() {
    String scanId = "test-scanid";
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_Authorized() {
    grantReadPermission(app.getId());
    policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(app.getId(), stage);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetEvaluationSummaryByApplicationId_Unauthenticated() {
    policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(app.getId(), stage);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetEvaluationSummaryByApplicationId_Unauthorized() {
    login();
    policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(app.getId(), stage);
  }
}
