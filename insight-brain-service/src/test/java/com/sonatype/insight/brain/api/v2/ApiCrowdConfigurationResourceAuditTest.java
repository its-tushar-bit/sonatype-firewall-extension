/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiCrowdConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCrowdConfigurationService;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApiCrowdConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CROWD_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration() throws Exception {
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = "serverUrl";
    dto.applicationName = "applicationName";
    dto.applicationPassword = "applicationPassword".toCharArray();

    HttpResponse response = restRequest().body(dto).put();

    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CROWD, null);
    assertCustomData(auditDTO, ApiCrowdConfigurationService.CROWD_SERVER_URL_AUDIT_KEY, dto.serverUrl);
    assertCustomData(auditDTO, ApiCrowdConfigurationService.CROWD_APPLICATION_NAME_AUDIT_KEY, dto.applicationName);
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_BadRequest() throws Exception {
    HttpResponse response = restRequest().body(null).put();

    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_CROWD, "bad-request");
  }

  @Test
  public void testDeleteCrowdConfiguration() throws Exception {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();

    HttpResponse response = restRequest().delete();

    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_CROWD, null);
    assertCustomData(auditDTO, ApiCrowdConfigurationService.CROWD_SERVER_URL_AUDIT_KEY,
        crowdConfiguration.getServerUrl());
    assertCustomData(auditDTO, ApiCrowdConfigurationService.CROWD_APPLICATION_NAME_AUDIT_KEY,
        crowdConfiguration.getApplicationName());
  }

  @Test
  public void testDeleteCrowdConfiguration_Unauthorized() throws Exception {
    HttpResponse response = restRequest().with(unauthorizedUser()).delete();

    assertResponseStatus(403, response);
    assertAuditLog(AuditEvent.DELETE_CROWD, "unauthorized");
  }
}
