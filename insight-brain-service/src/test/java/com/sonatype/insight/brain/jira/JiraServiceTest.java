/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.sonatype.insight.brain.jira.JiraField.DESCRIPTION;
import static com.sonatype.insight.brain.jira.JiraField.ISSUETYPE;
import static com.sonatype.insight.brain.jira.JiraField.PROJECT;
import static com.sonatype.insight.brain.jira.JiraField.REPORTER;
import static com.sonatype.insight.brain.jira.JiraField.SUMMARY;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JiraServiceTest
{
  @Mock
  private InsightConfig insightConfig;

  @Mock
  private JiraConfig jiraConfig;

  @Mock
  private JiraClientFactory jiraClientFactory;

  @Mock
  private JiraClient jiraClient;

  private JiraService underTest;

  @Before
  public void setUp() {
    Map<String, String> reporterMap = new HashMap<>();
    reporterMap.put("name", "reporter_name");

    Map<String, Object> customFields = new HashMap<>();
    customFields.put(REPORTER, reporterMap);

    when(insightConfig.getJiraConfig()).thenReturn(jiraConfig);
    when(jiraConfig.getCustomFields()).thenReturn(customFields);

    underTest = new JiraService(insightConfig, jiraClientFactory);
  }

  @Test
  public void testIsEnabled() {
    // default null configuration should NOT be enabled
    when(insightConfig.getJiraConfig()).thenReturn(null);

    assertThat(underTest.isEnabled(), is(false));

    // install configuration and it should be enabled
    when(insightConfig.getJiraConfig()).thenReturn(jiraConfig);

    assertThat(underTest.isEnabled(), is(true));
  }

  @Test
  public void testIsAcceptableIssueType_IsAcceptable() {
    JiraIssueType issueType = createIssueType();

    assertThat(underTest.isAcceptableIssueType(issueType), is(true));
  }

  @Test
  public void testIsAcceptableIssueType_IsNotAcceptable() {
    JiraIssueType issueType = createIssueType();
    issueType.setSubtask(true);

    assertThat(underTest.isAcceptableIssueType(issueType), is(false));
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

    assertThat(underTest.isAcceptableIssueType(issueType), is(false));
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

    assertThat(underTest.isAcceptableIssueType(issueType), is(true));
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

    when(jiraClient.getIssueCreateMeta()).thenReturn(jiraIssueCreateMeta);
    when(jiraClientFactory.create()).thenReturn(jiraClient);

    JiraService underTest = new JiraService(insightConfig, jiraClientFactory);
    jiraProjectList = underTest.getProjectsWithAcceptableIssueTypes();

    assertThat(jiraProjectList, hasSize(1));
    jiraProject = jiraProjectList.get(0);
    assertEquals(jiraProject.getKey(), "key1");
    assertThat(jiraProject.getIssueTypes(), hasSize(1));
    assertEquals(jiraProject.getIssueTypes().get(0).getId(), 1);
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
