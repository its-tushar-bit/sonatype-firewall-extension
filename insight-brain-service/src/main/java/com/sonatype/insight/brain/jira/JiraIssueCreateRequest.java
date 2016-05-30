/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import static com.sonatype.insight.brain.jira.JiraField.DESCRIPTION;
import static com.sonatype.insight.brain.jira.JiraField.ISSUETYPE;
import static com.sonatype.insight.brain.jira.JiraField.LABELS;
import static com.sonatype.insight.brain.jira.JiraField.PROJECT;
import static com.sonatype.insight.brain.jira.JiraField.SUMMARY;

/**
 * https://docs.atlassian.com/jira/REST/latest/#api/2/issue-createIssue
 *
 * @since 1.21.0
 */
public class JiraIssueCreateRequest
{
  private Map<String, Object> fields;

  public Map<String, Object> getFields() {
    if (fields == null) {
      fields = new LinkedHashMap<>();
    }
    return fields;
  }

  public void setFields(final Map<String, Object> fields) {
    this.fields = fields;
  }

  public JiraIssueCreateRequest field(final String name, final Object value) {
    getFields().put(name, value);
    return this;
  }

  /**
   * Helper to set {@code project} field from a project key.
   */
  public JiraIssueCreateRequest project(final String key) {
    return field(PROJECT, ImmutableMap.of("key", key));
  }

  /**
   * Helper to set {@code project} field from a project id.
   */
  public JiraIssueCreateRequest project(final long id) {
    return field(PROJECT, ImmutableMap.of("id", id));
  }

  /**
   * Helper to set {@code issuetype} field from a issue-type name.
   */
  public JiraIssueCreateRequest issueType(final String name) {
    return field(ISSUETYPE, ImmutableMap.of("name", name));
  }

  /**
   * Helper to set {@code issuetype} field from a issue-type id.
   */
  public JiraIssueCreateRequest issueType(final long id) {
    return field(ISSUETYPE, ImmutableMap.of("id", id));
  }

  /**
   * Helper to set {@code summary} field.
   */
  public JiraIssueCreateRequest summary(final String text) {
    return field(SUMMARY, text);
  }

  /**
   * Helper to set {@code description} field.
   */
  public JiraIssueCreateRequest description(final String text) {
    return field(DESCRIPTION, text);
  }

  /**
   * Helper to set {@code labels} field.
   */
  public JiraIssueCreateRequest labels(final List<String> labels) {
    field(LABELS, labels);
    return this;
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
