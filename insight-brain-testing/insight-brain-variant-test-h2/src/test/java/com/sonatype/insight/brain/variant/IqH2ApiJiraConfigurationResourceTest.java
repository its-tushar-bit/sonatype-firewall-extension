/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiJiraConfigurationResourceTest
{
  private IqTestContext ctx;

  private JiraConfigurationDAO dao;

  @BeforeEach
  void setUp() {
    dao = ctx.lookup(JiraConfigurationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.JIRA_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testGetConfiguration() throws Exception {
    JiraConfiguration config = ctx.tempEntity().newJiraConfiguration();

    HttpResponse response = restRequest().get();

    ctx.assertResponseStatus(200, response);
    ApiJiraConfigurationDTO dto = response.getBody(ApiJiraConfigurationDTO.class);
    assertThat(dto).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("password")
        .isEqualTo(config);
    assertThat(dto.password).isNull();
  }

  @Test
  void testGetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().get();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(JiraConfigurationDAO.NOT_FOUND_ERROR_MSG);
  }

  @Test
  void testSetConfiguration() throws Exception {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    dto.username = "username";
    dto.password = "password".toCharArray();
    dto.customFields = Maps.newHashMap("field", "value");

    HttpResponse response = restRequest().body(dto).put();

    ctx.assertResponseStatus(204, response);
    JiraConfiguration jiraConfiguration = dao.get();
    assertThat(jiraConfiguration).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("password")
        .isEqualTo(dto);
    assertThat(ctx.lookup(PasswordHandler.class).decryptPassword(jiraConfiguration.getPassword()))
        .isEqualTo(dto.password);
  }

  @Test
  void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiJiraConfigurationService.NO_CONFIG_ERROR_MSG);
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    ctx.tempEntity().newJiraConfiguration();

    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(204, response);
    assertThat(dao.get()).isNull();
  }

  @Test
  void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(JiraConfigurationDAO.NOT_FOUND_ERROR_MSG);
  }
}
