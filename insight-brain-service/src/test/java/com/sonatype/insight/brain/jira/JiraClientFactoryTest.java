/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.utils.AbstractHttpClientTest;

public class JiraClientFactoryTest
    extends AbstractHttpClientTest
{
  @Inject
  private JiraClientFactory jiraClientFactory;

  @Inject
  private InsightConfig appConfig;

  @Override
  protected void pingUrl(String url) throws Exception {
    JiraConfig jiraConfig = new JiraConfig();
    jiraConfig.setUrl(url);
    jiraConfig.setUsername("jira-user");
    jiraConfig.setPassword("jira-pass");
    appConfig.setJiraConfig(jiraConfig);
    jiraClientFactory.create().getIssueCreateMeta();
  }
}
