/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

/**
 * Details about a JIRA field as configured for a project issue-type.
 *
 * @see JiraIssueType
 * @since 1.21.0
 */
public class JiraField
{
  public static final String PROJECT = "project";

  public static final String SUMMARY = "summary";

  public static final String ISSUETYPE = "issuetype";

  public static final String DESCRIPTION = "description";

  public static final String REPORTER = "reporter";

  private String name;

  private boolean required;

  private boolean hasDefaultValue;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(final boolean required) {
    this.required = required;
  }

  public boolean isHasDefaultValue() {
    return hasDefaultValue;
  }

  public void setHasDefaultValue(final boolean hasDefaultValue) {
    this.hasDefaultValue = hasDefaultValue;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        ", required=" + required +
        ", hasDefaultValue=" + hasDefaultValue +
        '}';
  }
}
