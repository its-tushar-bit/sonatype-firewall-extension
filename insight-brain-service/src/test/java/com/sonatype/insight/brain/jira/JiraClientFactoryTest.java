/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.utils.AbstractHttpClientTest;

public class JiraClientFactoryTest
    extends AbstractHttpClientTest
{
  @Inject
  private JiraClientFactory jiraClientFactory;

  @Inject
  private PasswordHandler passwordHandler;

  @Override
  protected void pingUrl(String url) throws Exception {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl(url);
    jiraConfiguration.setUsername("jira-user");
    jiraConfiguration.setPassword(passwordHandler.encryptPassword("jira-pass".toCharArray()));
    jiraClientFactory.create(jiraConfiguration).getIssueCreateMeta();
  }
}
