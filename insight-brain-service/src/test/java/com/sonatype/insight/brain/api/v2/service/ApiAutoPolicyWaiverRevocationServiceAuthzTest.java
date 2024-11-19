/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiAutoPolicyWaiverRevocationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiAutoPolicyWaiverRevocationService apiAutoPolicyWaiverRevocationService;

  @Test(expected = UnauthenticatedException.class)
  public void testAddAutoPolicyWaiverRevocation_Unauthenticated() {
    Application app = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverRevocationDTO dto = new ApiAutoPolicyWaiverRevocationDTO();
    apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddAutoPolicyWaiverRevocation_Unauthorized() {
    login();
    Application app = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverRevocationDTO dto = new ApiAutoPolicyWaiverRevocationDTO();
    apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeUserId",
        "fakeUserName",
        new Date(),
        waiver.getId(),
        "scanId",
        "fakeHash",
        "fakeAssociatedPackageUrl",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteAutoPolicyWaiverRevocation_Unauthenticated() {
    Application app = tempEntity.newApplicationWithParent();
    apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(
        OwnerType.APPLICATION,
        app.getId(),
        "fakeRevocationId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteAutoPolicyWaiverRevocation_Unauthorized() {
    login();
    Application app = tempEntity.newApplicationWithParent();
    apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(
        OwnerType.APPLICATION,
        app.getId(),
        "fakeRevocationId");
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(
        OwnerType.APPLICATION,
        app.getId(),
        revocation.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAutoPolicyWaiverExclusionLogData_Unauthenticated() {
    apiAutoPolicyWaiverRevocationService.getAutoPolicyWaiverRevocations(
        OwnerType.APPLICATION, "ownerId", "autoPolicyWaiverId", 1, 10);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAutoPolicyWaiverExclusionLogData_Unauthorized() {
    login();
    apiAutoPolicyWaiverRevocationService.getAutoPolicyWaiverRevocations(
        OwnerType.APPLICATION, app.getId(), "autoPolicyWaiverId", 1, 10);
  }

  @Test
  public void testGetAutoPolicyWaiverExclusionLogData_Authorized() {
    grantReadPermission(app.getId());
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    apiAutoPolicyWaiverRevocationService.getAutoPolicyWaiverRevocations(
        OwnerType.APPLICATION, app.getId(), waiver.getId(), 1, 10);
  }
}
