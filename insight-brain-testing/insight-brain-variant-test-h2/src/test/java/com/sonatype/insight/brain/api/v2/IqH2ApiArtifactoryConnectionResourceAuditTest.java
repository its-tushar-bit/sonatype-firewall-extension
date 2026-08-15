/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiArtifactoryConnectionService;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Package-scoped: touches {@link ArtifactoryConnectionResource}'s package-private {@code BY_OWNER}/
 * {@code BY_ARTIFACTORY} constants, so the class stays in the original resource's package (see
 * convert-resource-test-to-variant skill, Step 3). Reproduces the {@code AbstractAuditTest} audit-log
 * capture/assertion scaffolding that the legacy {@code ApiArtifactoryConnectionResourceAuditTest} inherited.
 */
@IqH2Test
class IqH2ApiArtifactoryConnectionResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Application app;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setup() {
    logOutput.before();
    logOutput.clear();
    app = ctx.tempEntity().newApplicationWithParent();
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
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
    return ctx.restRequest().path(PublicApiPaths.ARTIFACTORY_CONNECTION_CONFIG_PATH_V2).auth();
  }

  @Test
  void testAudit_AddArtifactoryConnection() throws Exception {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://localrepo.com/";

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    ctx.assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.ARTIFACTORY_URL_AUDIT_KEY, dto.baseUrl);
  }

  @Test
  void testAudit_UpdateArtifactoryConnection() throws Exception {
    ArtifactoryConnection existingConnection =
        ctx.tempEntity().newArtifactoryConnection(app.getId(), "http://baseurl.com", null, null);
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://updatedrepo.com/";

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .body(dto)
        .put();
    ctx.assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.ARTIFACTORY_URL_AUDIT_KEY, dto.baseUrl);
  }

  @Test
  void testAudit_DeleteArtifactoryConnection() throws Exception {
    ArtifactoryConnection existingConnection =
        ctx.tempEntity().newArtifactoryConnection(app.getId(), "http://baseurl.com", null, null);

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .delete();
    ctx.assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ARTIFACTORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testAudit_UpdateArtifactoryConnectionStatus_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.enabled = true;
    dto.allowOverride = false;

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(dto)
        .put();
    ctx.assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.ENABLED_MODE_AUDIT_KEY, "enable");
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.OVERRIDE_BY_CHILD_AUDIT_KEY, "disallow");
  }

  @Test
  void testAudit_UpdateArtifactoryConnectionStatus_Application() throws Exception {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.enabled = true;

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .put();
    ctx.assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.ENABLED_MODE_AUDIT_KEY, "enable");
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
