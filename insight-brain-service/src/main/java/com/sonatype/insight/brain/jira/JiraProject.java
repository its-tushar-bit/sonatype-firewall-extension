/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @since 1.21.0
 */
public class JiraProject
{
  private long id;

  private String key;

  private String name;

  private List<JiraIssueType> issueTypes;

  public long getId() {
    return id;
  }

  public void setId(final long id) {
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  public void setKey(final String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public List<JiraIssueType> getIssueTypes() {
    return issueTypes;
  }

  public void setIssueTypes(final List<JiraIssueType> issueTypes) {
    this.issueTypes = issueTypes;
  }

  // NOTE: Jackson 2.5+ supports configuring mapper to be case-insensitive, but Jackson version used here is too old

  /**
   * {@link #issueTypes} is used as {@code issuetypes} from some endpoints.
   * Different method here to avoid confusion with the default setter for the property.
   */
  @JsonSetter("issuetypes")
  public void setIssueTypesLowercase(final List<JiraIssueType> issueTypes) {
    this.issueTypes = issueTypes;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id=" + id +
        ", key='" + key + '\'' +
        ", name='" + name + '\'' +
        ", issueTypes=" + issueTypes +
        '}';
  }
}
