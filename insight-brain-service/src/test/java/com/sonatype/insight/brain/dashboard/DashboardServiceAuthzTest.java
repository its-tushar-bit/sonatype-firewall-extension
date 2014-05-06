/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.common.collect.Sets;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DashboardServiceAuthzTest
    extends AbstractServiceAuthzTest
{

  @Inject
  private DashboardService dashboardService;

  @Test
  public void testGetPolicyViolations() throws Exception {
    login();

    List<PolicyViolationDTO> result = dashboardService.getPolicyViolations(null, null, null, null, false);
    // We don't have read permissions for any application.
    assertThat(result, empty());

    grantReadPermission(app.getId());

    PolicyViolation violation = createPolicyViolation(app.getId());
    result = dashboardService.getPolicyViolations(null, null, null, null, false);
    assertThat(result, hasSize(1));
    PolicyViolationDTO dto = result.get(0);
    assertThat(dto.id, is(violation.getId()));
  }

  @Test
  public void testGetPolicyViolationsByApplicationIds() throws Exception {
    try {
      dashboardService.getPolicyViolationsByApplicationIds(Sets.newHashSet(app.getPublicId()), null, null, null, null,
          false);
      fail("Should throw an UnauthenticatedException as we haven't logged in.");
    }
    catch (UnauthenticatedException e) {
      // Properly thrown exception.
    }

    login();

    try {
      dashboardService.getPolicyViolationsByApplicationIds(Sets.newHashSet(app.getPublicId()), null, null, null, null,
          false);
      fail("Should throw an UnauthorizedException as the application does not have read permissions.");
    }
    catch (UnauthorizedException e) {
      // Properly thrown exception.
    }

    grantReadPermission(app.getId());

    PolicyViolation violation = createPolicyViolation(app.getId());
    List<PolicyViolationDTO> result = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app.getPublicId()), null, null, null, null, false);
    assertThat(result, hasSize(1));
    PolicyViolationDTO dto = result.get(0);
    assertThat(dto.id, is(violation.getId()));

    Application application = tempEntity.newApplication("nonReadableApplicationId", org.getId());

    try {
      dashboardService.getPolicyViolationsByApplicationIds(
          Sets.newHashSet(app.getPublicId(), application.getPublicId()), null, null, null, null, false);
      fail("Should throw an UnauthorizedException as one of the applications does not have read permissions.");
    }
    catch (UnauthorizedException e) {
      // Properly thrown exception.
    }
  }

  private PolicyViolation createPolicyViolation(String appId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "test scan id");
    return tempEntity.newPolicyViolation(evaluation.getId(), tempEntity.newPolicy(app.getId(), "test policy name"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentRisks_ExplicitApplicationFilter_Unauthenticated() {
    dashboardService.getComponentRisks(Collections.singleton(app.getPublicId()), null, null, null, null, 1);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentRisks_ExplicitApplicationFilter_Unauthorized() {
    login();
    dashboardService.getComponentRisks(Collections.singleton(app.getPublicId()), null, null, null, null, 1);
  }

  @Test
  public void testGetComponentRisks_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    dashboardService.getComponentRisks(Collections.singleton(app.getPublicId()), null, null, null, null, 1);
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId());
    assertThat(dashboardService.getComponentRisks(null, null, null, null, null, 1), hasSize(0));
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId());
    login();
    assertThat(dashboardService.getComponentRisks(null, null, null, null, null, 1), hasSize(0));
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Authorized() {
    createPolicyViolation(app.getId());
    grantReadPermission(app.getId());
    assertThat(dashboardService.getComponentRisks(null, null, null, null, null, 1), hasSize(1));
  }
}
