/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import static com.google.common.base.Preconditions.checkState;

/**
 * Creates {@link JiraClient} instances.
 *
 * @since 1.21.0
 */
@Named
@Singleton
public class JiraClientFactory
{
  private final InsightConfig insightConfig;

  private final InsightProxy insightProxy;

  @Inject
  public JiraClientFactory(final InsightConfig insightConfig,
                           final InsightProxy insightProxy)
  {
    this.insightConfig = insightConfig;
    this.insightProxy = insightProxy;
  }

  public JiraClient create() {
    JiraConfig jiraConfig = insightConfig.getJiraConfig();
    checkState(jiraConfig != null, "Missing JIRA configuration");

    Configuration configuration = new Configuration();
    insightProxy.contextualize(configuration, jiraConfig.getUrl());
    SimpleAuthentication authentication = new SimpleAuthentication();
    authentication.setUsername(jiraConfig.getUsername());
    authentication.setPassword(jiraConfig.getPassword().toCharArray());
    configuration.setServerAuth(authentication);

    return new JiraClient(configuration);
  }
}
