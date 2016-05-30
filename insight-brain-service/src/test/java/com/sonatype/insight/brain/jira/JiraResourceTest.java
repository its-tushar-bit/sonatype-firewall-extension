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
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class JiraResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(JiraResource.RESOURCE_PATH);
  }

  @Before
  public void setup() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
        config.setJiraConfig(new JiraConfig());
      }
    });

    JiraIssueCreateMeta jiraIssueCreateMeta = new JiraIssueCreateMeta();
    JiraProject jiraProject = new JiraProject();
    jiraProject.setKey("key");
    jiraProject.setName("projectName");
    JiraIssueType issueType = new JiraIssueType();
    issueType.setId(1);
    issueType.setName("issueName");
    issueType.setFields(new HashMap<String, JiraField>());
    jiraProject.setIssueTypes(Collections.singletonList(issueType));
    jiraIssueCreateMeta.setProjects(Collections.singletonList(jiraProject));

    when(mockJiraClient.getIssueCreateMeta()).thenReturn(jiraIssueCreateMeta);
  }

  @Test
  @ManualServerInit
  public void testGetProjectsWithAcceptableIssueTypes() throws Exception {
    HttpResponse response = restRequest().path(JiraResource.PROJECT_PATH).get();
    assertResponseStatus(200, response);
    JiraProject[] jiraProjects = response.getBody(JiraProject[].class);

    assertNotNull(jiraProjects);
    assertEquals(jiraProjects.length, 1);

    JiraProject jiraProject = jiraProjects[0];
    assertEquals(jiraProject.getKey(), "key");
    assertEquals(jiraProject.getName(), "projectName");
    assertThat(jiraProject.getIssueTypes(), hasSize(1));

    JiraIssueType issueType = jiraProject.getIssueTypes().get(0);
    assertEquals(issueType.getId(), 1);
    assertEquals(issueType.getName(), "issueName");
  }
}
