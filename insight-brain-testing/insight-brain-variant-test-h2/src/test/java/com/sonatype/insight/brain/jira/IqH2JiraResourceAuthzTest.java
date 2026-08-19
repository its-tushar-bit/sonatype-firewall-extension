/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.Collections;
import java.util.HashMap;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Kept in the {@code com.sonatype.insight.brain.jira} package; reproduces the
 * {@code AbstractResourceAuthzTest} fixture (org/app/repo + authorized/unauthorized users) and its
 * {@code testAuthcGet}/{@code assertStatus} helpers that the legacy {@code JiraResourceAuthzTest}
 * inherited from its base class, plus the Jira configuration + mock {@code JiraClient} setup.
 */
@IqH2Test
class IqH2JiraResourceAuthzTest
{
  private IqTestContext ctx;

  private Organization org;

  private Application app;

  private Repository repo;

  private RepositoryManager repositoryManager;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void setup() throws Exception {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    repositoryManager = ctx.tempEntity().newRepositoryManager();
    repo = ctx.tempEntity().newRepository(repositoryManager, "test");
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();

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

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(JiraResource.RESOURCE_PATH);
  }

  private void assertStatus(HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  // Sometimes, simply being able to log in, is all the authorization you need...
  private HttpResponse testAuthcGet(HttpRequest request) throws Exception {
    HttpResponse response = request.anon().get();
    assertStatus(response, 401);

    response = request.auth(authorized).get();
    assertStatus(response, null);
    return response;
  }

  @Test
  void testGetProjectsWithAcceptableIssueTypes() throws Exception {
    testAuthcGet(restRequest().path(JiraResource.PROJECT_PATH));
  }
}
