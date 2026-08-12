/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryConnectionService;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Package-scoped: touches {@link ApiRepositoryConnectionResource}'s package-private {@code BY_OWNER}/
 * {@code BY_REPOSITORY} constants, so the class stays in the original resource's package (see
 * convert-resource-test-to-variant skill, Step 3).
 */
@IqH2Test
class IqH2ApiRepositoryConnectionResourceAuditTest
    implements AuditTestSupport
{
  @RegisterExtension
  static WireMockExtension nxrm3MockSever = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  private IqTestContext ctx;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private Application app;

  @BeforeEach
  void setup() {
    logOutput.before();
    logOutput.clear();
    app = ctx.tempEntity().newApplicationWithParent();
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
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.REPOSITORY_CONNECTION_CONFIG_PATH_V2).auth();
  }

  @Test
  void testAudit_AddRepositoryConnection() throws Exception {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://localrepo.com/";
    dto.format = RepositoryFormat.MAVEN;

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    ctx.assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REPOSITORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, ApiRepositoryConnectionService.REPOSITORY_URL_AUDIT_KEY, dto.baseUrl);
    assertCustomData(auditDTO, ApiRepositoryConnectionService.REPOSITORY_FORMAT_AUDIT_KEY, dto.format.toString());
  }

  @Test
  void testAudit_UpdateRepositoryConnection() throws Exception {
    RepositoryConnection existingConnection =
        ctx.tempEntity().newRepositoryConnection(app.getId(), "http://baseurl.com", null, null);
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://updatedrepo.com/";
    dto.format = RepositoryFormat.MAVEN;

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .body(dto)
        .put();
    ctx.assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REPOSITORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, ApiRepositoryConnectionService.REPOSITORY_URL_AUDIT_KEY, dto.baseUrl);
    assertCustomData(auditDTO, ApiRepositoryConnectionService.REPOSITORY_FORMAT_AUDIT_KEY, dto.format.toString());
  }

  @Test
  void testAudit_DeleteRepositoryConnection() throws Exception {
    RepositoryConnection existingConnection =
        ctx.tempEntity().newRepositoryConnection(app.getId(), "http://baseurl.com", null, null);

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .delete();
    ctx.assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REPOSITORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testAudit_UpdateRepositoryConnectionStatus_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.enabled = true;
    dto.allowOverride = false;

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(dto)
        .put();
    ctx.assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REPOSITORY_CONNECTION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, ApiRepositoryConnectionService.ENABLED_MODE_AUDIT_KEY, "enable");
    assertCustomData(auditDTO, ApiRepositoryConnectionService.OVERRIDE_BY_CHILD_AUDIT_KEY, "disallow");
  }

  @Test
  void testAudit_UpdateRepositoryConnectionStatus_Application() throws Exception {
    ApiRepositoryConnectionStatusRequestDTO dto = new ApiRepositoryConnectionStatusRequestDTO();
    dto.enabled = true;

    HttpResponse response = restRequest().path(ApiRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .put();
    ctx.assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REPOSITORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, ApiRepositoryConnectionService.ENABLED_MODE_AUDIT_KEY, "enable");
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
