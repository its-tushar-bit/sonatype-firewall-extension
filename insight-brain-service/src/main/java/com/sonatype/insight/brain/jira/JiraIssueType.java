/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.Map;

/**
 * @see JiraProject
 * @since 1.21.0
 */
public class JiraIssueType
{
  private long id;

  private String name;

  private boolean subtask;

  private String iconUrl;

  private Map<String, JiraField> fields;

  public long getId() {
    return id;
  }

  public void setId(final long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public boolean isSubtask() {
    return subtask;
  }

  public void setSubtask(final boolean subtask) {
    this.subtask = subtask;
  }

  public String getIconUrl() {
    return iconUrl;
  }

  public void setIconUrl(final String iconUrl) {
    this.iconUrl = iconUrl;
  }

  public Map<String, JiraField> getFields() {
    return fields;
  }

  public void setFields(final Map<String, JiraField> fields) {
    this.fields = fields;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", subtask=" + subtask +
        ", iconUrl='" + iconUrl + '\'' +
        ", fields=" + fields +
        '}';
  }
}
