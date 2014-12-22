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

public class ComponentRiskServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ComponentRiskService componentRiskService;

  @Test
  public void testGetPolicyViolations() throws Exception {
    login();

    List<PolicyViolationDTO> result = componentRiskService.getPolicyViolations(null, null, null);
    // We don't have read permissions for any application.
    assertThat(result, empty());

    grantReadPermission(app.getId());

    PolicyViolation violation = createPolicyViolation(app.getId());
    result = componentRiskService.getPolicyViolations(null, null, null);
    assertThat(result, hasSize(1));
    PolicyViolationDTO dto = result.get(0);
    assertThat(dto.id, is(violation.getId()));
  }

  @Test
  public void testGetPolicyViolationsByApplicationIds() throws Exception {
    try {
      componentRiskService.getPolicyViolationsByApplicationIds(Sets.newHashSet(app.getId()), null, null, null);
      fail("Should throw an UnauthenticatedException as we haven't logged in.");
    }
    catch (UnauthenticatedException e) {
      // Properly thrown exception.
    }

    login();

    try {
      componentRiskService.getPolicyViolationsByApplicationIds(Sets.newHashSet(app.getId()), null, null, null);
      fail("Should throw an UnauthorizedException as the application does not have read permissions.");
    }
    catch (UnauthorizedException e) {
      // Properly thrown exception.
    }

    grantReadPermission(app.getId());

    PolicyViolation violation = createPolicyViolation(app.getId());
    List<PolicyViolationDTO> result = componentRiskService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app.getId()), null, null, null);
    assertThat(result, hasSize(1));
    PolicyViolationDTO dto = result.get(0);
    assertThat(dto.id, is(violation.getId()));

    Application application = tempEntity.newApplication("nonReadableApplicationId", org.getId());

    try {
      componentRiskService.getPolicyViolationsByApplicationIds(Sets.newHashSet(app.getId(), application.getId()), null,
          null, null);
      fail("Should throw an UnauthorizedException as one of the applications does not have read permissions.");
    }
    catch (UnauthorizedException e) {
      // Properly thrown exception.
    }
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentRisks_ExplicitApplicationFilter_Unauthenticated() {
    componentRiskService.getComponentRisks(Collections.singleton(app.getId()), null, null, null, null, 1);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentRisks_ExplicitApplicationFilter_Unauthorized() {
    login();
    componentRiskService.getComponentRisks(Collections.singleton(app.getId()), null, null, null, null, 1);
  }

  @Test
  public void testGetComponentRisks_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    componentRiskService.getComponentRisks(Collections.singleton(app.getId()), null, null, null, null, 1);
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId());
    assertThat(componentRiskService.getComponentRisks(null, null, null, null, null, 1), hasSize(0));
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId());
    login();
    assertThat(componentRiskService.getComponentRisks(null, null, null, null, null, 1), hasSize(0));
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Authorized() {
    createPolicyViolation(app.getId());
    grantReadPermission(app.getId());
    assertThat(componentRiskService.getComponentRisks(null, null, null, null, null, 1), hasSize(1));
  }

  private PolicyViolation createPolicyViolation(String appId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "test scan id");
    return tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(app.getId(), "test policy name"));
  }
}
