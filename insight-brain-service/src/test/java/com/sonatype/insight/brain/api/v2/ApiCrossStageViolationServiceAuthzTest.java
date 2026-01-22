/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiCrossStageViolationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiCrossStageViolationService service;

  private final ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foo", "1.0.0");

  @Test
  public void testGetCrossStageViolationById_Authorized() {
    grantReadPermission(app.getId());
    Policy policy = tempEntity.newPolicy(org.getId(), "p1", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, componentIdentifier, "1234", "vuln1");

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation.getId());
    assertThat(result.policyViolationId).isEqualTo(violation.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetCrossStageViolationById_Unauthorized() {
    grantWritePermission(app.getId());
    Policy policy = tempEntity.newPolicy(org.getId(), "p1", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, componentIdentifier, "1234", "vuln1");

    service.getCrossStageViolationById(violation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetCrossStageViolationById_Unauthenticated() {
    Policy policy = tempEntity.newPolicy(org.getId(), "p1", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, componentIdentifier, "1234", "vuln1");

    service.getCrossStageViolationById(violation.getId());
  }
}
