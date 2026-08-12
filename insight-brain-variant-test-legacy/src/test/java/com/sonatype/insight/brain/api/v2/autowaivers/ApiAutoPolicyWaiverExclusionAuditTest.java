/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.OWNERS_PATH;
import static com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionResponseDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@SuppressWarnings("deprecation")
@Category(SlowTest.class)
public class ApiAutoPolicyWaiverExclusionAuditTest
    extends AbstractAuditTest
{
  private static final String REPORT_RESOURCE = "/ApiAutoPolicyWaiverExclusionResourceTest/report";

  @Before
  public void setup() {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
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

    createPolicyThreatReport(app.getId(), eval.getScanId(), violation);

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

  private void createPolicyThreatReport(
      final String applicationId,
      final String scanId,
      final PolicyViolation violation) throws Exception
  {
    final InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    ReportTestUtils.createReportFile(
        applicationId,
        scanId,
        ReportTestUtils.zipReportDir(REPORT_RESOURCE, tempDir),
        insightWork);
    ReportHelper.createPolicyThreats(insightWork, applicationId, scanId, List.of(violation));
  }
}
