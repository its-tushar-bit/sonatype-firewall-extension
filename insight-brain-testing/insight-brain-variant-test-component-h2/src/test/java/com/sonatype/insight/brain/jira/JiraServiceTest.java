/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import static com.sonatype.insight.brain.jira.JiraField.DESCRIPTION;
import static com.sonatype.insight.brain.jira.JiraField.ISSUETYPE;
import static com.sonatype.insight.brain.jira.JiraField.PROJECT;
import static com.sonatype.insight.brain.jira.JiraField.REPORTER;
import static com.sonatype.insight.brain.jira.JiraField.SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class JiraServiceTest
    extends AbstractComponentH2Test
{
  @Mock
  private JiraClientFactory mockJiraClientFactory;

  @Mock
  private JiraClient mockJiraClient;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Inject
  private Configuration configuration;

  @Inject
  private JiraService jiraService;

  private final Map<String, Object> customFields = new HashMap<>();

  {
    Map<String, String> reporterMap = new HashMap<>();
    reporterMap.put("name", "reporter_name");
    customFields.put(REPORTER, reporterMap);
  }

  @Test
  public void testIsEnabled() {
    // default null configuration should NOT be enabled
    assertThat(jiraService.isEnabled()).isFalse();

    // install configuration and it should be enabled
    createJiraConfiguration(null);
    assertThat(jiraService.isEnabled()).isTrue();
  }

  @Test
  public void testIsAcceptableIssueType_IsAcceptable() {
    JiraIssueType issueType = createIssueType();

    assertThat(jiraService.isAcceptableIssueType(customFields, issueType)).isTrue();
  }

  @Test
  public void testIsAcceptableIssueType_IsNotAcceptable() {
    JiraIssueType issueType = createIssueType();
    issueType.setSubtask(true);

    assertThat(jiraService.isAcceptableIssueType(customFields, issueType)).isFalse();
  }

  /**
   * Custom required field should be unacceptable.
   */
  @Test
  public void testIsAcceptableIssueType_CustomRequiredFieldNotAcceptable() {
    JiraIssueType issueType = createIssueType();

    JiraField field = new JiraField();
    field.setRequired(true);
    issueType.getFields().put("custom", field);

    assertThat(jiraService.isAcceptableIssueType(customFields, issueType)).isFalse();
  }

  /**
   * Custom non-required field should be acceptable.
   */
  @Test
  public void testIsAcceptableIssueType_CustomFieldAcceptable() {
    JiraIssueType issueType = createIssueType();

    JiraField field = new JiraField();
    field.setRequired(false);
    issueType.getFields().put("custom", field);

    assertThat(jiraService.isAcceptableIssueType(customFields, issueType)).isTrue();
  }

  @Test
  public void testGetProjectsWithAcceptableIssues() throws IOException {
    JiraIssueCreateMeta jiraIssueCreateMeta = new JiraIssueCreateMeta();
    List<JiraProject> jiraProjectList = new ArrayList<>();

    JiraProject jiraProject = new JiraProject();
    jiraProject.setKey("key1");
    List<JiraIssueType> issueTypes = new ArrayList<>();
    JiraIssueType issueType = createIssueType();
    issueType.setId(1);
    JiraField field = new JiraField();
    field.setRequired(false);
    issueType.getFields().put("custom", field);
    issueTypes.add(issueType);
    issueType = createIssueType();
    issueType.setId(2);
    issueType.setSubtask(true);
    issueTypes.add(issueType);
    jiraProject.setIssueTypes(issueTypes);
    jiraProjectList.add(jiraProject);

    jiraProject = new JiraProject();
    jiraProject.setKey("key2");
    issueType = createIssueType();
    issueType.setId(3);
    field = new JiraField();
    field.setRequired(true);
    issueType.getFields().put("custom", field);
    jiraProject.setIssueTypes(Collections.singletonList(issueType));
    jiraProjectList.add(jiraProject);

    jiraIssueCreateMeta.setProjects(jiraProjectList);

    when(mockJiraClientFactory.create(any())).thenReturn(mockJiraClient);
    when(mockJiraClient.getIssueCreateMeta()).thenReturn(jiraIssueCreateMeta);

    createJiraConfiguration(customFields);
    JiraService underTest = new JiraService(configuration, mockJiraClientFactory);
    jiraProjectList = underTest.getProjectsWithAcceptableIssueTypes();

    assertThat(jiraProjectList).hasSize(1);
    jiraProject = jiraProjectList.get(0);
    assertThat(jiraProject.getKey()).isEqualTo("key1");
    assertThat(jiraProject.getIssueTypes()).extracting(JiraIssueType::getId).containsExactly(1L);
  }

  private JiraIssueType createIssueType() {
    JiraIssueType issueType = new JiraIssueType();
    Map<String, JiraField> fields = new HashMap<>();
    issueType.setFields(fields);
    defaultFields(fields);
    return issueType;
  }

  /**
   * Fill in the default fields.
   */
  private void defaultFields(final Map<String, JiraField> fields) {
    JiraField field = new JiraField();
    field.setRequired(true);
    fields.put(PROJECT, field);

    field = new JiraField();
    field.setRequired(true);
    fields.put(ISSUETYPE, field);

    field = new JiraField();
    field.setRequired(true);
    fields.put(SUMMARY, field);

    // default appears that description is optional, but we support it required as well
    field = new JiraField();
    field.setRequired(false);
    fields.put(DESCRIPTION, field);

    // JIRA 7 now marks reporter as required
    field = new JiraField();
    field.setRequired(true);
    fields.put(REPORTER, field);
  }
}
