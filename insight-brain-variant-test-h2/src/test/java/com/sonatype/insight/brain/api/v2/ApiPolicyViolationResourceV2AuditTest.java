/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Locale;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.PolicyAuditDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Converted from the legacy {@code ApiPolicyViolationResourceV2AuditTest}. Kept in the original
 * package/simple name because {@code testGetTransitivePolicyViolationsByOwnerStageComponent_*} and
 * {@code testGetTransitivePolicyViolationsByAppScanComponent} resolve the report fixture via
 * {@code getClass().getSimpleName()}.
 */
@IqH2Test
public class ApiPolicyViolationResourceV2AuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUserAccount;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  public void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUserAccount = ctx.tempEntity().newUser();
  }

  @AfterEach
  public void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUserAccount.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<com.sonatype.insight.brain.HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUserAccount);
  }

  @Test
  public void testGetPolicyViolations() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newApplication(org.getId());
    Policy policy1 = ctx.tempEntity().newPolicy(org);
    Policy policy2 = ctx.tempEntity().newPolicy(org);
    ctx.tempEntity().newApplicationWithParent();
    String unknownPolicyId = "unknownPolicyId";

    ctx.restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .query("p", policy1.getId(), policy2.getId(), unknownPolicyId)
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_POLICY_VIOLATIONS, null);
    PolicyAuditDTO[] actuals = JSON.convertValue(auditDTO.data.get("selectedPolicies"), PolicyAuditDTO[].class);

    assertThat(actuals).containsExactlyInAnyOrder(
        new PolicyAuditDTO(policy1.getId(), policy1),
        new PolicyAuditDTO(policy2.getId(), policy2),
        new PolicyAuditDTO(unknownPolicyId, null));
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent_Application() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    String scanId = "scanId";
    ctx.tempEntity().newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ctx.createReportFile(application.getId(), scanId, "/" + getClass().getSimpleName() + "/report");

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), BuildStageType.ID)
        .query("componentIdentifier", direct)
        .get();

    ctx.assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "stageId", BuildStageType.ID);
    assertCustomObject(auditDTO, "componentIdentifier", direct);
    assertCustomData(auditDTO, "componentHash", "hash1");
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    Application application = ctx.tempEntity().newApplication(organization.getId());
    String scanId = "scanId";
    ctx.tempEntity().newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ctx.createReportFile(application.getId(), scanId, "/" + getClass().getSimpleName() + "/report");

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH)
        .parameter(organization.getType().name().toLowerCase(Locale.ROOT), organization.getPublicId(),
            BuildStageType.ID)
        .query("componentIdentifier", direct)
        .get();

    ctx.assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "stageId", BuildStageType.ID);
    assertCustomObject(auditDTO, "componentIdentifier", direct);
    assertCustomData(auditDTO, "componentHash", "hash1");
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", null, "e");

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), BuildStageType.ID)
        .query("componentIdentifier", direct)
        .with(unauthorizedUser())
        .get();

    ctx.assertResponseStatus(403, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    String scanId = "scanId";
    ctx.tempEntity().newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ctx.createReportFile(application.getId(), scanId, "/" + getClass().getSimpleName() + "/report");

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_APP_AND_SCAN_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), scanId)
        .query("componentIdentifier", direct)
        .get();

    ctx.assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "scanId", scanId);
    assertCustomData(auditDTO, "stageId", BuildStageType.ID);
    assertCustomObject(auditDTO, "componentIdentifier", direct);
    assertCustomData(auditDTO, "componentHash", "hash1");
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", null, "e");

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_APP_AND_SCAN_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), "scanId")
        .query("componentIdentifier", direct)
        .with(unauthorizedUser())
        .get();

    ctx.assertResponseStatus(403, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, "unauthorized");
    assertApplicationData(auditDTO, application);
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
