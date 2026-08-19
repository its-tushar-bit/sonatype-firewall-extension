/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.function.Consumer;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPolicyDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSuccessMetricsRetentionPolicyDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Package-scoped: {@link #restRequest(String)} touches {@link ApiDataRetentionPolicyResource}'s package-private
 * {@code ORGANIZATION_PATH} constant, so the class stays in the original resource's package (see
 * convert-resource-test-to-variant skill, Step 3). Reproduces the {@code AbstractAuditTest} audit-log
 * capture/assertion scaffolding that the legacy {@code ApiDataRetentionPolicyResourceAuditTest} inherited.
 */
@IqH2Test
class IqH2ApiDataRetentionPolicyResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private HttpRequest restRequest(String organizationId) {
    return ctx.restRequest()
        .path(PublicApiPaths.DATA_RETENTION_POLICY_RESOURCE_PATH, ApiDataRetentionPolicyResource.ORGANIZATION_PATH)
        .parameter(organizationId);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  void testSetDataRetentionPolicies_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_BUILD,
        new ApiReportRetentionPolicyDTO(false, true, 30, ApiAgeDTO.fromString("2 weeks")));
    dto.successMetrics = new ApiSuccessMetricsRetentionPolicyDTO(false, true, ApiAgeDTO.fromString("1 year"));
    restRequest(org.getId()).body(dto).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_DATA_RETENTION, null);
    assertOrganizationData(auditDTO, org);
    assertCustomObject(auditDTO, "dataRetentionPolicies", dto);
  }

  @Test
  void testSetDataRetentionPolicies_Unauthorized() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    restRequest(org.getId()).with(unauthorizedUser()).body(dto).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_DATA_RETENTION, "unauthorized");
    assertOrganizationData(auditDTO, org);
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
