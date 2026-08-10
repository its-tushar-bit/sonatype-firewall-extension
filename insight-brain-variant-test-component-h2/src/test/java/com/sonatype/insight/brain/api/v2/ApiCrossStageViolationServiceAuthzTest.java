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
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiCrossStageViolationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiCrossStageViolationService service;

  private static final ComponentIdentifier COMPONENT_IDENTIFIER =
      ComponentIdentifier.createNpmCoordinates("foo", "1.0.0");

  @Test
  public void testGetCrossStageViolationById_Authorized() {
    grantReadPermission(app.getId());
    Policy policy = tempEntity.newPolicy(org.getId(), "p1", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationById(violation.getId());
    assertThat(result.policyViolationId).isEqualTo(violation.getId());
  }

  @Test
  public void testGetCrossStageViolationById_Unauthorized() {
    grantWritePermission(app.getId());
    Policy policy = tempEntity.newPolicy(org.getId(), "p1", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    assertThrows(UnauthorizedException.class, () -> service.getCrossStageViolationById(violation.getId()));
  }

  @Test
  public void testGetCrossStageViolationById_Unauthenticated() {
    Policy policy = tempEntity.newPolicy(org.getId(), "p1", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    assertThrows(UnauthenticatedException.class, () -> service.getCrossStageViolationById(violation.getId()));
  }
}
