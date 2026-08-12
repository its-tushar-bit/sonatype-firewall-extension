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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ApiArtifactoryConnectionResourceAuditTest
    extends AbstractAuditTest
{
  @Rule
  public WireMockRule artifactoryMockSever = new WireMockRule(wireMockConfig().dynamicPort());

  private Application app;

  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ARTIFACTORY_CONNECTION_CONFIG_PATH_V2).auth();
  }

  @Test
  public void testAudit_AddArtifactoryConnection() throws Exception {
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://localrepo.com/";

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.ARTIFACTORY_URL_AUDIT_KEY, dto.baseUrl);
  }

  @Test
  public void testAudit_UpdateArtifactoryConnection() throws Exception {
    ArtifactoryConnection existingConnection =
        tempEntity.newArtifactoryConnection(app.getId(), "http://baseurl.com", null, null);
    ApiArtifactoryConnectionDTO dto = new ApiArtifactoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://updatedrepo.com/";

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .body(dto)
        .put();
    assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.ARTIFACTORY_URL_AUDIT_KEY, dto.baseUrl);
  }

  @Test
  public void testAudit_DeleteArtifactoryConnection() throws Exception {
    ArtifactoryConnection existingConnection =
        tempEntity.newArtifactoryConnection(app.getId(), "http://baseurl.com", null, null);

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_ARTIFACTORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .delete();
    assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ARTIFACTORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testAudit_UpdateArtifactoryConnectionStatus_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.enabled = true;
    dto.allowOverride = false;

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(dto)
        .put();
    assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.ENABLED_MODE_AUDIT_KEY, "enable");
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.OVERRIDE_BY_CHILD_AUDIT_KEY, "disallow");
  }

  @Test
  public void testAudit_UpdateArtifactoryConnectionStatus_Application() throws Exception {
    ApiArtifactoryConnectionStatusRequestDTO dto = new ApiArtifactoryConnectionStatusRequestDTO();
    dto.enabled = true;

    HttpResponse response = restRequest().path(ArtifactoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .put();
    assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ARTIFACTORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, ApiArtifactoryConnectionService.ENABLED_MODE_AUDIT_KEY, "enable");
  }
}
