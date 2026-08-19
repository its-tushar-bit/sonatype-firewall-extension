/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
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
  private final InsightProxy insightProxy;

  private final PasswordHandler passwordHandler;

  @Inject
  public JiraClientFactory(InsightProxy insightProxy, PasswordHandler passwordHandler) {
    this.insightProxy = insightProxy;
    this.passwordHandler = passwordHandler;
  }

  public JiraClient create(JiraConfiguration jiraConfiguration) {
    checkState(jiraConfiguration != null, "Missing JIRA configuration");

    Configuration configuration = new Configuration();
    insightProxy.contextualize(configuration, jiraConfiguration.getUrl());
    if (jiraConfiguration.getUsername() != null && jiraConfiguration.getPassword() != null) {
      SimpleAuthentication authentication = new SimpleAuthentication();
      authentication.setUsername(jiraConfiguration.getUsername());
      authentication.setPassword(passwordHandler.decryptPassword(jiraConfiguration.getPassword()));
      configuration.setServerAuth(authentication);
    }

    return new JiraClient(configuration);
  }
}
