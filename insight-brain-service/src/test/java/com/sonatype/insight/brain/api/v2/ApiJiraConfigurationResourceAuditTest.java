/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.assertj.core.util.Maps;
import org.junit.Test;

public class ApiJiraConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.JIRA_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void testSetConfiguration() throws Exception {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    dto.username = "username";
    dto.password = "password".toCharArray();
    dto.customFields = Maps.newHashMap("field", "value");

    HttpResponse response = restRequest().body(dto).put();

    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_JIRA, null);
    assertAuditData(auditDTO, dto.url, dto.username);
  }

  @Test
  public void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_JIRA, "bad-request");
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    JiraConfiguration config = tempEntity.newJiraConfiguration();

    HttpResponse response = restRequest().delete();

    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_JIRA, null);
    assertAuditData(auditDTO, config.getUrl(), config.getUsername());
  }

  @Test
  public void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_JIRA, "not-found");
  }

  private void assertAuditData(
      AuditDTO auditDTO,
      String url,
      String username)
  {
    assertCustomData(auditDTO, "url", url);
    assertCustomData(auditDTO, "username", username);
  }
}
