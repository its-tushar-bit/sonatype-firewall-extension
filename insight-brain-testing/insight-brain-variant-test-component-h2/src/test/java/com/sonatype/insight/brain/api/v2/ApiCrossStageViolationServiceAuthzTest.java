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
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
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

  /**
   * CLM-44279 — HRC-owned violations must return a well-formed response for a caller with
   * read permission on the underlying repository (which the HRC inherits from).
   */
  @Test
  public void testGetCrossStageViolationByConstituentId_hrcOwned_Authorized() {
    Repository repository = tempEntity.newRepository();
    grantReadPermission(repository.getId());
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    Policy policy = tempEntity.newPolicy(org.getId(), "p1", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-1", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    ApiCrossStageViolationDTOV2 result = service.getCrossStageViolationByConstituentId(violation.getId());
    assertThat(result.hrcId).isEqualTo(hrc.getId());
    assertThat(result.applicationPublicId).isNull();
  }

  /**
   * CLM-44279 — an authenticated caller without read permission on the underlying repository
   * must be rejected on the HRC-owned path (mirrors the app-owned {@code _Unauthorized} case).
   */
  @Test
  public void testGetCrossStageViolationByConstituentId_hrcOwned_Unauthorized() {
    Repository repository = tempEntity.newRepository();
    grantWritePermission(repository.getId());
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    Policy policy = tempEntity.newPolicy(org.getId(), "p1", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-1", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    assertThrows(UnauthorizedException.class,
        () -> service.getCrossStageViolationByConstituentId(violation.getId()));
  }

  /**
   * CLM-44279 — unauthenticated callers must be rejected on the HRC-owned path
   * (mirrors the app-owned {@code _Unauthenticated} case).
   */
  @Test
  public void testGetCrossStageViolationByConstituentId_hrcOwned_Unauthenticated() {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    Policy policy = tempEntity.newPolicy(org.getId(), "p1", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(hrc.getId(), Stage.ID_BUILD, "hrc-scan-1", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, COMPONENT_IDENTIFIER, "1234", "vuln1");

    assertThrows(UnauthenticatedException.class,
        () -> service.getCrossStageViolationByConstituentId(violation.getId()));
  }
}
