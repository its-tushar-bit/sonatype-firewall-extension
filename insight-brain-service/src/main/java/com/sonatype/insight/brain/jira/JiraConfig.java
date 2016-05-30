/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * JIRA integration configuration.
 *
 * @since 1.21.0
 */
public class JiraConfig
{
  /**
   * The URL of the JIRA server.  For example: {@code https://issues.sonatype.org/}.
   */
  @NotNull
  private String url;

  @NotNull
  private String username;

  @NotNull
  private String password;

  /**
   * Optional fields to include in JIRA issue create requests; before more specific fields are added.
   *
   * See https://docs.atlassian.com/jira/REST/latest/#api/2/issue-createIssue
   */
  @Nullable
  private Map<String, Object> customFields;

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  @Nullable
  public Map<String, Object> getCustomFields() {
    return customFields;
  }

  public void setCustomFields(@Nullable final Map<String, Object> customFields) {
    this.customFields = customFields;
  }
}
