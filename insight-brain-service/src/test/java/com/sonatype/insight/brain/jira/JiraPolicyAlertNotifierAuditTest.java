/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.Arrays;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest.JiraIssueCreateResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.service.AbstractComponentAuditTest;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JiraPolicyAlertNotifierAuditTest
    extends AbstractComponentAuditTest
{
  @Mock
  private JiraService mockJiraService;

  @Mock
  private JiraClient mockJiraClient;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private JiraPolicyAlertNotifier jiraPolicyAlertNotifier;

  private Application app;

  private static final String SCAN_ID = "jira-audit-scan-id";

  private static final String PROJECT_KEY = "project-key";

  private static final String STAGE_ID = Stage.ID_BUILD;

  @Override
  public void configure(Binder binder) {
    binder.bind(JiraService.class).toInstance(mockJiraService);
    super.configure(binder);
  }

  @Before
  public void before() {
    setBaseUrl("http://localhost");

    when(mockJiraService.getConfiguration()).thenReturn(new JiraConfiguration());
    when(mockJiraService.client(any())).thenReturn(mockJiraClient);

    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testSendNotifications() throws Exception {
    List<PolicyNotification> policyNotifications = createPolicyNotifications();

    JiraIssueCreateResponse jiraIssueCreateResponse = mock(JiraIssueCreateResponse.class);
    when(mockJiraClient.createIssue(any(JiraIssueCreateRequest.class), anyBoolean()))
        .thenReturn(jiraIssueCreateResponse);
    when(jiraIssueCreateResponse.getKey()).thenReturn("audit-jira-key");

    jiraPolicyAlertNotifier.sendNotifications(app, SCAN_ID, new Stage(STAGE_ID, "BUILD"), policyNotifications);

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.CREATE_JIRA_ISSUE, 2);
    assertJiraNotificationAuditLog(auditDTOs.get(0), app, SCAN_ID, STAGE_ID, PROJECT_KEY, policyNotifications.size(),
        null);
    assertJiraNotificationAuditLog(auditDTOs.get(1), app, SCAN_ID, STAGE_ID, PROJECT_KEY, policyNotifications.size(),
        null);

    assertThat(auditDTOs).extracting(auditDTO -> auditDTO.data.get("jiraIssueTypeId")).contains(1, 2);
  }

  @Test
  public void testSendNotifications_JiraCommunicationError() throws Exception {
    List<PolicyNotification> policyNotifications = createPolicyNotifications();

    JiraIssueCreateResponse jiraIssueCreateResponse = mock(JiraIssueCreateResponse.class);
    when(mockJiraClient.createIssue(any(), anyBoolean())).thenThrow(new BadGatewayException("broken"))
        .thenReturn(jiraIssueCreateResponse);
    when(jiraIssueCreateResponse.getKey()).thenReturn("audit-jira-key");

    jiraPolicyAlertNotifier.sendNotifications(app, SCAN_ID, new Stage(STAGE_ID, "BUILD"), policyNotifications);

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.CREATE_JIRA_ISSUE, 2);
    assertJiraNotificationAuditLog(auditDTOs.get(0), app, SCAN_ID, STAGE_ID, PROJECT_KEY, policyNotifications.size(),
        "bad-gateway");
    assertJiraNotificationAuditLog(auditDTOs.get(1), app, SCAN_ID, STAGE_ID, PROJECT_KEY, policyNotifications.size(),
        null);

    assertThat(auditDTOs).extracting(auditDTO -> auditDTO.data.get("jiraIssueTypeId")).contains(1, 2);
  }

  private List<PolicyNotification> createPolicyNotifications() {
    Policy policy = tempEntity.newPolicy(app);
    policy.getNotifications().add(new JiraNotification(PROJECT_KEY, 1, STAGE_ID));
    policy.getNotifications().add(new JiraNotification(PROJECT_KEY, 2, STAGE_ID));
    policyDAO.update(policy);

    return Arrays.asList(createPolicyNotification(policy, "hash1"), createPolicyNotification(policy, "hash2"));
  }

  private PolicyNotification createPolicyNotification(Policy policy, String hash) {
    PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    ApplicationComponent component = tempEntity
        .newApplicationComponent(app.getId(), STAGE_ID, hash, MatchState.EXACT, false);
    policyFact.addComponentFact(new ComponentFact(component.getComponentIdentifier(), component.getHash()));
    return new PolicyNotification(policyFact, policy.getNotifications());
  }

  private void assertJiraNotificationAuditLog(AuditDTO auditDTO,
                                              Application app,
                                              String scanId,
                                              String stageId,
                                              String jiraProjectKey,
                                              int totalPolicyViolationCount,
                                              String error)
  {
    assertStandardData(auditDTO, AuditEvent.CREATE_JIRA_ISSUE, error, SYSTEM_USER);
    assertApplicationData(auditDTO, app);
    if (error == null) {
      assertCustomData(auditDTO, "scanId", scanId);
      assertCustomData(auditDTO, "stageId", stageId);
      assertCustomData(auditDTO, "jiraProjectKey", jiraProjectKey);
      assertCustomData(auditDTO, "totalPolicyViolationCount", totalPolicyViolationCount);
    }
  }
}
