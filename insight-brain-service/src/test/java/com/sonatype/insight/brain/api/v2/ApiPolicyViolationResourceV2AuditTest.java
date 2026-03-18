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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.Test;

import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyViolationResourceV2AuditTest
    extends AbstractAuditTest
{
  @Test
  public void testGetPolicyViolations() throws Exception {
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    Policy policy1 = tempEntity.newPolicy(org);
    Policy policy2 = tempEntity.newPolicy(org);
    tempEntity.newApplicationWithParent();
    String unknownPolicyId = "unknownPolicyId";

    restRequest().path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
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
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationResourceV2AuditTest/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), BuildStageType.ID)
        .query("componentIdentifier", direct)
        .get();

    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "stageId", BuildStageType.ID);
    assertCustomObject(auditDTO, "componentIdentifier", direct);
    assertCustomData(auditDTO, "componentHash", "hash1");
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationResourceV2AuditTest/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH)
        .parameter(organization.getType().name().toLowerCase(Locale.ROOT), organization.getPublicId(),
            BuildStageType.ID)
        .query("componentIdentifier", direct)
        .get();

    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "stageId", BuildStageType.ID);
    assertCustomObject(auditDTO, "componentIdentifier", direct);
    assertCustomData(auditDTO, "componentHash", "hash1");
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", null, "e");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), BuildStageType.ID)
        .query("componentIdentifier", direct)
        .with(unauthorizedUser())
        .get();

    assertResponseStatus(403, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationResourceV2AuditTest/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_APP_AND_SCAN_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), scanId)
        .query("componentIdentifier", direct)
        .get();

    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "scanId", scanId);
    assertCustomData(auditDTO, "stageId", BuildStageType.ID);
    assertCustomObject(auditDTO, "componentIdentifier", direct);
    assertCustomData(auditDTO, "componentHash", "hash1");
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", null, "e");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_APP_AND_SCAN_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), "scanId")
        .query("componentIdentifier", direct)
        .with(unauthorizedUser())
        .get();

    assertResponseStatus(403, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS, "unauthorized");
    assertApplicationData(auditDTO, application);
  }
}
