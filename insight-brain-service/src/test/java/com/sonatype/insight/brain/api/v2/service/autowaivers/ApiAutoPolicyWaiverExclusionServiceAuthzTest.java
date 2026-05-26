/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import static com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionRequestDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.List;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

public class ApiAutoPolicyWaiverExclusionServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Mock
  private ReportService reportService;

  @Inject
  private ApiAutoPolicyWaiverExclusionService apiAutoPolicyWaiverExclusionService;

  @Test(expected = UnauthenticatedException.class)
  public void testAddAutoPolicyWaiverExclusion_Unauthenticated() {
    Application app = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverExclusionRequestDTO dto = new ApiAutoPolicyWaiverExclusionRequestDTO();
    apiAutoPolicyWaiverExclusionService.addAutoPolicyWaiverExclusion(OwnerType.APPLICATION, app.getId(), dto);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddAutoPolicyWaiverExclusion_Unauthorized() {
    login();
    Application app = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverExclusionRequestDTO dto = new ApiAutoPolicyWaiverExclusionRequestDTO();
    apiAutoPolicyWaiverExclusionService.addAutoPolicyWaiverExclusion(OwnerType.APPLICATION, app.getId(), dto);
  }

  @Test
  public void testAddAutoPolicyWaiverExclusion_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation);

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverExclusionRequestDTO dto = new ApiAutoPolicyWaiverExclusionRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = violation.getId();
    dto.autoPolicyWaiverId = waiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    apiAutoPolicyWaiverExclusionService.addAutoPolicyWaiverExclusion(OwnerType.APPLICATION, app.getId(), dto);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteAutoPolicyWaiverExclusion_Unauthenticated() {
    Application app = tempEntity.newApplicationWithParent();
    apiAutoPolicyWaiverExclusionService.deleteAutoPolicyWaiverExclusion(
        OwnerType.APPLICATION,
        app.getId(),
        "fakeExclusionId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteAutoPolicyWaiverExclusion_Unauthorized() {
    login();
    Application app = tempEntity.newApplicationWithParent();
    apiAutoPolicyWaiverExclusionService.deleteAutoPolicyWaiverExclusion(
        OwnerType.APPLICATION,
        app.getId(),
        "fakeExclusionId");
  }

  @Test
  public void testDeleteAutoPolicyWaiverExclusion_Authorized() {
    Application app = tempEntity.newApplicationWithParent();
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(),
        waiver.getId());
    apiAutoPolicyWaiverExclusionService.deleteAutoPolicyWaiverExclusion(
        OwnerType.APPLICATION,
        app.getId(),
        exclusion.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAutoPolicyWaiverExclusionLogData_Unauthenticated() {
    apiAutoPolicyWaiverExclusionService.getAutoPolicyWaiverExclusions(
        OwnerType.APPLICATION, "ownerId", "autoPolicyWaiverId", 1, 10);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAutoPolicyWaiverExclusionLogData_Unauthorized() {
    login();
    apiAutoPolicyWaiverExclusionService.getAutoPolicyWaiverExclusions(
        OwnerType.APPLICATION, app.getId(), "autoPolicyWaiverId", 1, 10);
  }

  @Test
  public void testGetAutoPolicyWaiverExclusionLogData_Authorized() {
    grantReadPermission(app.getId());
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    apiAutoPolicyWaiverExclusionService.getAutoPolicyWaiverExclusions(
        OwnerType.APPLICATION, app.getId(), waiver.getId(), 1, 10);
  }

  private PolicyThreats createPolicyThreats(final List<Component> components) {
    final PolicyThreats policyThreats = new PolicyThreats();
    policyThreats.aaData.addAll(components);

    return policyThreats;
  }

  private PolicyThreats.Component createPolicyThreatsComponents(
      ComponentIdentifier componentIdentifier,
      PolicyViolation violation)
  {
    PolicyThreats.PolicyViolation policyViolation = new PolicyThreats.PolicyViolation();
    policyViolation.policyThreatLevel = violation.getThreatLevel();
    policyViolation.policyViolationId = violation.getId();
    policyViolation.policyName = violation.getPolicyName();
    policyViolation.policyId = violation.getPolicyId();
    policyViolation.actions = null;
    policyViolation.constraints = null;
    policyViolation.policyThreatCategory = null;
    policyViolation.reachabilityStatus = null;
    policyViolation.constraintFactsJson = violation.getConstraintFactsJson();

    final PolicyThreats.Component component = new PolicyThreats.Component();
    component.hash = violation.getHash();
    component.componentIdentifier = componentIdentifier;
    component.activeViolations.add(policyViolation);
    component.allViolations.add(policyViolation);
    return component;
  }
}
