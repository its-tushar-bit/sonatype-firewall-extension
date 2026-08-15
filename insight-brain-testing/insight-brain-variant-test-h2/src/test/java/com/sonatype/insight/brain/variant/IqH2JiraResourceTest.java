/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Collections;
import java.util.HashMap;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.jira.JiraClient;
import com.sonatype.insight.brain.jira.JiraClientFactory;
import com.sonatype.insight.brain.jira.JiraIssueCreateMeta;
import com.sonatype.insight.brain.jira.JiraIssueType;
import com.sonatype.insight.brain.jira.JiraProject;
import com.sonatype.insight.brain.jira.JiraResource;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@IqH2Test
class IqH2JiraResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(JiraResource.RESOURCE_PATH);
  }

  @BeforeEach
  void setup() throws Exception {
    createJiraConfiguration();

    JiraIssueCreateMeta jiraIssueCreateMeta = new JiraIssueCreateMeta();
    JiraProject jiraProject = new JiraProject();
    jiraProject.setKey("key");
    jiraProject.setName("projectName");
    JiraIssueType issueType = new JiraIssueType();
    issueType.setId(1);
    issueType.setName("issueName");
    issueType.setFields(new HashMap<>());
    jiraProject.setIssueTypes(Collections.singletonList(issueType));
    jiraIssueCreateMeta.setProjects(Collections.singletonList(jiraProject));

    JiraClient mockJiraClient = mock(JiraClient.class);
    when(mockJiraClient.getIssueCreateMeta()).thenReturn(jiraIssueCreateMeta);
    when(ctx.lookup(JiraClientFactory.class).create(any())).thenReturn(mockJiraClient);
  }

  private void createJiraConfiguration() {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    ApiJiraConfigurationService jiraConfigurationService = ctx.lookup(ApiJiraConfigurationService.class);
    jiraConfigurationService.setConfigurationInDatabaseNoAuthz(JsonUtils.asTree(dto));
    jiraConfigurationService.applyJiraConfigurationToClients();
  }

  @Test
  void testGetProjectsWithAcceptableIssueTypes() throws Exception {
    HttpResponse response = restRequest().path(JiraResource.PROJECT_PATH).get();
    ctx.assertResponseStatus(200, response);
    JiraProject[] jiraProjects = response.getBody(JiraProject[].class);

    assertThat(jiraProjects).hasSize(1);

    JiraProject jiraProject = jiraProjects[0];
    assertThat(jiraProject.getKey()).isEqualTo("key");
    assertThat(jiraProject.getName()).isEqualTo("projectName");
    assertThat(jiraProject.getIssueTypes()).hasSize(1);

    JiraIssueType issueType = jiraProject.getIssueTypes().get(0);
    assertThat(issueType.getId()).isEqualTo(1);
    assertThat(issueType.getName()).isEqualTo("issueName");
  }
}
