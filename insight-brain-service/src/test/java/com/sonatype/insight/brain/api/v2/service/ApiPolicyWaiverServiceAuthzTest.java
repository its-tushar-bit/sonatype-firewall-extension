/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

public class ApiPolicyWaiverServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiPolicyWaiverService apiPolicyWaiverService;

  private PolicyViolation policyViolation;

  @Before
  public void setUpPolicyViolation() {
    Policy policy = tempEntity.newPolicy(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");
  }

  @Test
  public void testAddPolicyWaiver_Application_Authorized() {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiver_Application_Unauthenticated() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyWaiver_Application_UnauthorizedButAuthenticated() {
    login();
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.APPLICATION, "waiver comment");
  }

  @Test
  public void testAddPolicyWaiver_Organization_Authorized() {
    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddPolicyWaiver_Organization_Unauthenticated() {
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddWaiver_Organization_UnauthorizedButAuthenticated() {
    login();
    apiPolicyWaiverService.addPolicyWaiver(policyViolation.getId(), OwnerType.ORGANIZATION, "waiver comment");
  }
}
