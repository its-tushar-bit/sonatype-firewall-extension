/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.JiraConfigurationListener;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static com.sonatype.insight.brain.jira.JiraField.DESCRIPTION;
import static com.sonatype.insight.brain.jira.JiraField.ISSUETYPE;
import static com.sonatype.insight.brain.jira.JiraField.PROJECT;
import static com.sonatype.insight.brain.jira.JiraField.SUMMARY;

/**
 * @since 1.21.0
 */
@Named
@Singleton
public class JiraService
    implements JiraConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(JiraService.class);

  private final JiraConfigurationDAO jiraConfigurationDAO;

  private final JiraClientFactory clientFactory;

  private final AtomicReference<JiraConfiguration> jiraConfigurationAtomicReference = new AtomicReference<>();

  @Inject
  public JiraService(
      final JiraConfigurationDAO jiraConfigurationDAO,
      final JiraClientFactory clientFactory)
  {
    this.jiraConfigurationDAO = jiraConfigurationDAO;
    this.clientFactory = clientFactory;
    jiraConfigurationChanged();
  }

  public JiraConfiguration getConfiguration() {
    return jiraConfigurationAtomicReference.get();
  }

  public boolean isEnabled() {
    return getConfiguration() != null;
  }

  public JiraClient client(JiraConfiguration jiraConfiguration) {
    checkState(jiraConfiguration != null, "JIRA client was accessed but not enabled by configuration");
    return clientFactory.create(jiraConfiguration);
  }

  /**
   * Returns a list of projects which have acceptable issue-types.
   * <p>
   * Projects which have some acceptable issue-types have unacceptable omitted.
   */
  public List<JiraProject> getProjectsWithAcceptableIssueTypes() throws IOException {
    List<JiraProject> projects = new ArrayList<>();

    JiraConfiguration jiraConfiguration = getConfiguration();
    JiraClient client = client(jiraConfiguration);
    JiraIssueCreateMeta createMeta = client.getIssueCreateMeta();

    for (JiraProject project : createMeta.getProjects()) {
      List<JiraIssueType> acceptable = new ArrayList<>();
      for (JiraIssueType issueType : project.getIssueTypes()) {
        if (isAcceptableIssueType(jiraConfiguration.getCustomFields(), issueType)) {
          acceptable.add(issueType);
        }
        else {
          log.trace("Omitting unacceptable issue-type: {} for project: {}", issueType.getName(), project.getKey());
        }
      }

      // if any issue-types are acceptable, then replace and include in result
      if (!acceptable.isEmpty()) {
        project.setIssueTypes(acceptable);
        projects.add(project);
      }
      else {
        log.debug("Ignoring project {}; which has no acceptable issue-types", project.getKey());
      }
    }

    return projects;
  }

  /**
   * Returns {@code true} if the issue type is acceptable for notification creation.
   * <p>
   * Must not be a sub-task and have any required fields (w/o default values) other than: project, summary, issuetype or
   * description
   */
  @VisibleForTesting
  boolean isAcceptableIssueType(final Map<String, Object> customFields, final JiraIssueType issueType) {
    // all sub-tasks are not acceptable
    if (issueType.isSubtask()) {
      return false;
    }

    for (Entry<String, JiraField> entry : issueType.getFields().entrySet()) {
      String key = entry.getKey();
      JiraField field = entry.getValue();
      if (field.isRequired()) {
        if (PROJECT.equals(key) ||
            SUMMARY.equals(key) ||
            ISSUETYPE.equals(key) ||
            DESCRIPTION.equals(key) ||
            field.isHasDefaultValue() ||
            (customFields != null && customFields.containsKey(key))) {
          // accept the minimum set of required fields, required fields with default value,
          // or required fields with custom field defined for it
          continue;
        }
        else {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public void jiraConfigurationChanged() {
    jiraConfigurationAtomicReference.set(jiraConfigurationDAO.get());
  }
}
