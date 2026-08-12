/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.Collections;
import java.util.HashMap;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.when;

public class JiraResourceAuthzTest
    extends AbstractResourceAuthzTest
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
    testAuthcGet(restRequest().path(JiraResource.PROJECT_PATH));
  }
}
