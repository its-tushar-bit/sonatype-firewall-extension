/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import static com.sonatype.insight.brain.jira.JiraField.DESCRIPTION;
import static com.sonatype.insight.brain.jira.JiraField.ISSUETYPE;
import static com.sonatype.insight.brain.jira.JiraField.PROJECT;
import static com.sonatype.insight.brain.jira.JiraField.SUMMARY;

/**
 * https://docs.atlassian.com/jira/REST/latest/#api/2/issue-createIssue
 *
 * @since 1.21.0
 */
public class JiraIssueCreateRequest
{
  private final Map<String, Object> fields = new LinkedHashMap<>();

  public Map<String, Object> getFields() {
    return fields;
  }

  @SuppressWarnings("unchecked")
  public <T> T getField(String name) {
    return (T) fields.get(name);
  }

  private JiraIssueCreateRequest field(final String name, final Object value) {
    fields.put(name, value);
    return this;
  }

  /**
   * Helper to set {@code project} field from a project key.
   */
  JiraIssueCreateRequest project(final String key) {
    return field(PROJECT, ImmutableMap.of("key", key));
  }

  /**
   * Helper to set {@code issuetype} field from a issue-type id.
   */
  JiraIssueCreateRequest issueType(final long id) {
    return field(ISSUETYPE, ImmutableMap.of("id", id));
  }

  /**
   * Helper to set {@code summary} field.
   */
  JiraIssueCreateRequest summary(final String text) {
    return field(SUMMARY, text);
  }

  /**
   * Helper to set {@code description} field.
   */
  JiraIssueCreateRequest description(final Object description) {
    return field(DESCRIPTION, description);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "fields=" + fields +
        '}';
  }

  /**
   * Successful issue creation response.
   */
  public static class JiraIssueCreateResponse
  {
    private long id;

    private String key;

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

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "id=" + id +
          ", key='" + key + '\'' +
          '}';
    }
  }
}
