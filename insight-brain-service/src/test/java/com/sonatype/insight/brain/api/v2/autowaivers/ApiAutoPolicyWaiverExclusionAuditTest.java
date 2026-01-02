/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionResponseDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.OWNERS_PATH;
import static com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
public class ApiAutoPolicyWaiverExclusionAuditTest
    extends AbstractAuditTest
{
  protected static ReportService reportService = mock(ReportService.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(ReportService.class).toInstance(reportService);
    super.configure(binder);
  }

  @Before
  public void setup() {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Mockito.reset(reportService);
  }

  @After
  public void cleanup() {
    licenseManager.reset();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(org.getId());
    Policy policy = tempEntity.newPolicy(org.getId());

    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverExclusionRequestDTO dto = new ApiAutoPolicyWaiverExclusionRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getOrganizationId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = violation.getId();
    dto.autoPolicyWaiverId = autoPolicyWaiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(dto)
        .post();

    assertResponseStatus(200, response);
    ApiAutoPolicyWaiverExclusionResponseDTO responseDTO =
        response.getBody(ApiAutoPolicyWaiverExclusionResponseDTO.class);
    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER_REVOCATION, null);
    assertOrganizationData(auditDTO, org);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId",
        responseDTO.autoPolicyWaiverExclusionId);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverExclusionRequestDTO dto = new ApiAutoPolicyWaiverExclusionRequestDTO();
    dto.applicationPublicId = application.getPublicId();
    dto.ownerId = application.getId();
    dto.scanId = "scanId";
    dto.policyViolationId = "violationId";
    dto.autoPolicyWaiverId = autoPolicyWaiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    restRequest().with(unauthorizedUser())
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(dto)
        .post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER_REVOCATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion =
        tempEntity.newAutoPolicyWaiverExclusion(app.getId(), autoPolicyWaiver.getId());

    restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER_REVOCATION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId", exclusion.getId());
  }

  @Test
  public void testDeleteAutoPolicyWaiverExclusion_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    AutoPolicyWaiverExclusion exclusion =
        tempEntity.newAutoPolicyWaiverExclusion(organization.getId(), autoPolicyWaiver.getId());

    restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER_REVOCATION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId", exclusion.getId());
  }

  @Test
  public void testDeleteAutoPolicyWaiverExclusion_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    AutoPolicyWaiverExclusion exclusion =
        tempEntity.newAutoPolicyWaiverExclusion(organization.getId(), autoPolicyWaiver.getId());

    restRequest().with(unauthorizedUser())
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER_REVOCATION, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  private PolicyThreats createPolicyThreats(final List<Component> components) {
    final PolicyThreats policyThreats = new PolicyThreats();
    policyThreats.aaData.addAll(components);

    return policyThreats;
  }

  private PolicyThreats.Component createPolicyThreatsComponents(
      ComponentIdentifier componentIdentifier,
      PolicyViolation violation
  )
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
