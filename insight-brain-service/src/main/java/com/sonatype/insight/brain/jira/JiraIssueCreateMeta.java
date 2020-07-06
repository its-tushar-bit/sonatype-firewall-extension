/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.List;

/**
 * Details about issue creation.
 *
 * This includes the minimal data needed to determine which projects, issue-types and fields.
 *
 * https://docs.atlassian.com/jira/REST/latest/#api/2/issue-getCreateIssueMeta
 * https://developer.atlassian.com/cloud/jira/platform/rest/v3/#api-rest-api-3-issue-createmeta-get
 *
 * @since 1.21.0
 */
public class JiraIssueCreateMeta
{
  private List<JiraProject> projects;

  public List<JiraProject> getProjects() {
    return projects;
  }

  public void setProjects(final List<JiraProject> projects) {
    this.projects = projects;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "projects=" + projects +
        '}';
  }
}
