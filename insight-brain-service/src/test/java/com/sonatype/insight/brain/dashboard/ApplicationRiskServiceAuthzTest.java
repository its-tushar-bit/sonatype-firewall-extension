/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class ApplicationRiskServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private Application app;

  @Inject
  private ApplicationRiskService applicationRiskService;

  @Before
  public void setup() {
    Organization org = tempEntity.newOrganization();
    app = tempEntity.newApplication("app1", "app1", org.getId());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(tempEntity
        .newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId", new Date(System.currentTimeMillis() - 1000)),
        tempEntity.newPolicy(org.getId(), "policy", 3));
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app.getId(), BuildStageType.ID);
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthenticated() {
    List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs = applicationRiskService
        .getApplicationRisks(Collections.singleton(app.getId()), null, null, null, null, 1);
    assertThat(applicationRiskScoreDTOs, hasSize(0));
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthorized() {
    login();
    List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs = applicationRiskService
        .getApplicationRisks(Collections.singleton(app.getId()), null, null, null, null, 1);
    assertThat(applicationRiskScoreDTOs, hasSize(0));
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs = applicationRiskService.getApplicationRisks(
        Collections.singleton(app.getId()), null, null, null, null, 1);
    assertThat(applicationRiskScoreDTOs, hasSize(1));
  }
}
