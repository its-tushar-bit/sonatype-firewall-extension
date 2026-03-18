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
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiJiraConfigurationResourceTest
    extends AbstractResourceTest
{
  private JiraConfigurationDAO dao;

  @Before
  public void setUp() {
    dao = lookup(JiraConfigurationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.JIRA_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void testGetConfiguration() throws Exception {
    JiraConfiguration config = tempEntity.newJiraConfiguration();

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    ApiJiraConfigurationDTO dto = response.getBody(ApiJiraConfigurationDTO.class);
    assertThat(dto).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("password")
        .isEqualTo(config);
    assertThat(dto.password).isNull();
  }

  @Test
  public void testGetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(JiraConfigurationDAO.NOT_FOUND_ERROR_MSG);
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
    JiraConfiguration jiraConfiguration = dao.get();
    assertThat(jiraConfiguration).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("password")
        .isEqualTo(dto);
    assertThat(getCLMServer().getInstance(PasswordHandler.class).decryptPassword(jiraConfiguration.getPassword()))
        .isEqualTo(dto.password);
  }

  @Test
  public void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(ApiJiraConfigurationService.NO_CONFIG_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    tempEntity.newJiraConfiguration();

    HttpResponse response = restRequest().delete();

    assertResponseStatus(204, response);
    assertThat(dao.get()).isNull();
  }

  @Test
  public void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(JiraConfigurationDAO.NOT_FOUND_ERROR_MSG);
  }
}
