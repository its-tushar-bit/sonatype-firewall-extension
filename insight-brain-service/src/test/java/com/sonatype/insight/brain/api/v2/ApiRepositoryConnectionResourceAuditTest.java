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
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ApiRepositoryConnectionResourceAuditTest
    extends AbstractAuditTest
{
  @Rule
  public WireMockRule nxrm3MockSever = new WireMockRule(wireMockConfig().dynamicPort());

  private Application app;

  private InsightConfig insightConfig;

  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
    insightConfig = getCLMServer().getInstance(InsightConfig.class);
    insightConfig.setExperimentalFeatures(
        ImmutableMap.of(ExperimentalFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getFlag(), true));
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.REPOSITORY_CONNECTION_CONFIG_PATH_V2).auth();
  }

  @Test
  public void testAudit_AddRepositoryConnection() throws Exception {
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://localrepo.com/";

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto)
        .post();
    assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REPOSITORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "repositoryBaseUrl", dto.baseUrl);
  }

  @Test
  public void testAudit_UpdateRepositoryConnection() throws Exception {
    RepositoryConnection existingConnection =
        tempEntity.newRepositoryConnection(app.getId(), "http://baseurl.com", null, null);
    ApiRepositoryConnectionDTO dto = new ApiRepositoryConnectionDTO();
    dto.ownerId = app.getId();
    dto.baseUrl = "http://updatedrepo.com/";

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .body(dto)
        .put();
    assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REPOSITORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "repositoryBaseUrl", dto.baseUrl);
  }

  @Test
  public void testAudit_DeleteRepositoryConnection() throws Exception {
    RepositoryConnection existingConnection =
        tempEntity.newRepositoryConnection(app.getId(), "http://baseurl.com", null, null);

    HttpResponse response = restRequest().path(DefaultRepositoryConnectionResource.BY_REPOSITORY)
        .parameter(OwnerType.APPLICATION, app.getId(), existingConnection.getId())
        .delete();
    assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REPOSITORY_CONNECTION, null);
    assertApplicationData(auditDTO, app);
  }
}
