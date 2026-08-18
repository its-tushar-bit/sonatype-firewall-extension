/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionResponseDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.OWNERS_PATH;
import static com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;
import static org.mockito.Mockito.when;

/**
 * Package-scoped: touches {@link ApiAutoPolicyWaiverExclusionResource}'s package-private {@code OWNERS_PATH}/
 * {@code BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH} constants, so the class stays in the original resource's package
 * (see convert-resource-test-to-variant skill, Step 3).
 */
@SuppressWarnings("deprecation")
@IqH2Test
public class IqH2ApiAutoPolicyWaiverExclusionAuditTest
    implements AuditTestSupport
{
  private static final String REPORT_RESOURCE = "/ApiAutoPolicyWaiverExclusionResourceTest/report";

  private IqTestContext ctx;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private com.sonatype.insight.brain.model.security.User unauthorizedUser;

  @BeforeEach
  public void setup() throws Exception {
    logOutput.before();
    logOutput.clear();
    // beforeTest() resets the shared DeveloperEnablementService mock; the auto-policy-waiver resource is gated
    // by @ProductLicenseEnforcementPoint(DEVELOPER_DASHBOARD), which FeaturesService derives from this mock, so
    // re-stub it before setFeatures grants DEVELOPER_DASHBOARD (mirrors the AbstractResourceTest baseline).
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(true);
    unauthorizedUser = ctx.tempEntity().newUser();
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
  }

  @AfterEach
  public void cleanup() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  private java.util.function.Consumer<com.sonatype.insight.brain.HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(org.getId());
    Policy policy = ctx.tempEntity().newPolicy(org.getId());

    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = ctx.tempEntity().newPolicyViolation(eval, policy, identifier, "fake", "fake");

    createPolicyThreatReport(app.getId(), eval.getScanId(), violation);

    ApiAutoPolicyWaiverExclusionRequestDTO dto = new ApiAutoPolicyWaiverExclusionRequestDTO();
    dto.applicationPublicId = app.getPublicId();
    dto.ownerId = app.getOrganizationId();
    dto.scanId = eval.getScanId();
    dto.policyViolationId = violation.getId();
    dto.autoPolicyWaiverId = autoPolicyWaiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(dto)
        .post();

    ctx.assertResponseStatus(200, response);
    ApiAutoPolicyWaiverExclusionResponseDTO responseDTO =
        response.getBody(ApiAutoPolicyWaiverExclusionResponseDTO.class);
    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER_REVOCATION, null);
    assertOrganizationData(auditDTO, org);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId",
        responseDTO.autoPolicyWaiverExclusionId);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverExclusionRequestDTO dto = new ApiAutoPolicyWaiverExclusionRequestDTO();
    dto.applicationPublicId = application.getPublicId();
    dto.ownerId = application.getId();
    dto.scanId = "scanId";
    dto.policyViolationId = "violationId";
    dto.autoPolicyWaiverId = autoPolicyWaiver.getId();
    dto.matchStrategy = EXACT_COMPONENT;

    ctx.restRequest()
        .with(unauthorizedUser())
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(dto)
        .post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER_REVOCATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion =
        ctx.tempEntity().newAutoPolicyWaiverExclusion(app.getId(), autoPolicyWaiver.getId());

    ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER_REVOCATION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId", exclusion.getId());
  }

  @Test
  public void testDeleteAutoPolicyWaiverExclusion_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(organization.getId());
    AutoPolicyWaiverExclusion exclusion =
        ctx.tempEntity().newAutoPolicyWaiverExclusion(organization.getId(), autoPolicyWaiver.getId());

    ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER_REVOCATION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId", exclusion.getId());
  }

  @Test
  public void testDeleteAutoPolicyWaiverExclusion_Unauthorized() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(organization.getId());
    AutoPolicyWaiverExclusion exclusion =
        ctx.tempEntity().newAutoPolicyWaiverExclusion(organization.getId(), autoPolicyWaiver.getId());

    ctx.restRequest()
        .with(unauthorizedUser())
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
    final InsightWork insightWork = ctx.lookup(InsightWork.class);
    ReportTestUtils.createReportFile(
        applicationId,
        scanId,
        ReportTestUtils.zipReportDir(REPORT_RESOURCE, ctx.tempFolder()),
        insightWork);
    ReportHelper.createPolicyThreats(insightWork, applicationId, scanId, List.of(violation));
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
