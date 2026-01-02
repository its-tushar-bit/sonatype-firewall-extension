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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class JiraResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(JiraResource.RESOURCE_PATH);
  }

  @Before
  public void setup() throws Exception {
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

    when(mockJiraClient.getIssueCreateMeta()).thenReturn(jiraIssueCreateMeta);
  }

  @Test
  public void testGetProjectsWithAcceptableIssueTypes() throws Exception {
    HttpResponse response = restRequest().path(JiraResource.PROJECT_PATH).get();
    assertResponseStatus(200, response);
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
