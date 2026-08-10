/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class PolicyEvaluationSummaryServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private PolicyEvaluationSummaryService policyEvaluationSummaryService;

  private final Stage stage = new Stage(Stage.ID_BUILD);

  @BeforeEach
  public void setup() {
    String scanId = "test-scanid";
    tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_Authorized() {
    grantReadPermission(app.getId());
    policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(app.getId(), stage);
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(app.getId(), stage));
  }

  @Test
  public void testGetEvaluationSummaryByApplicationId_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(app.getId(), stage));
  }
}
