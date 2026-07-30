/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentAuditTest;
import com.sonatype.insight.brain.service.InsightMail;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@Category(SlowTest.class)
public class PolicyAlertEmailerAuditTest
    extends AbstractComponentAuditTest
{
  private static final List<String> EMAILS = Arrays.asList("test1@sonatype.com", "test2@sonatype.com");

  private static final Comparator<AuditDTO> EMAIL_COMPARATOR = Comparator
      .comparing(auditDTO -> (String) auditDTO.data.getOrDefault("emailAddress", ""));

  private static final String SCAN_ID = "scanId";

  private static final String STAGE_ID = BuildStageType.ID;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyAlertEmailer policyAlertEmailer;

  @Mock
  private InsightMail mockInsightMail;

  private Application application;

  @Before
  public void before() {
    setBaseUrl("http://localhost");
    when(mockInsightMail.getCdnUrl()).thenReturn("https://cdn.sonatype.com/");
    application = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testSendNotifications() {
    List<PolicyNotification> policyNotifications = createPolicyNotifications();

    policyAlertEmailer.sendNotifications(application, SCAN_ID, new Stage(STAGE_ID), policyNotifications, 0, false);

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.SEND_MAIL, 2);
    auditDTOs.sort(EMAIL_COMPARATOR);
    assertApplicationPolicyNotificationAuditData(auditDTOs.get(0), policyNotifications.size(), EMAILS.get(0), null);
    assertApplicationPolicyNotificationAuditData(auditDTOs.get(1), policyNotifications.size(), EMAILS.get(1), null);
  }

  @Test
  public void testSendNotifications_MailException() {
    List<PolicyNotification> policyNotifications = createPolicyNotifications();
    doThrow(new RuntimeException()).doNothing().when(mockInsightMail).sendHtml(any(), anyString(), anyString());

    policyAlertEmailer.sendNotifications(application, SCAN_ID, new Stage(STAGE_ID), policyNotifications, 0, false);

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.SEND_MAIL, 2);
    auditDTOs.sort(EMAIL_COMPARATOR);
    assertApplicationPolicyNotificationAuditData(auditDTOs.get(0), policyNotifications.size(), EMAILS.get(0),
        "server-error");
    assertApplicationPolicyNotificationAuditData(auditDTOs.get(1), policyNotifications.size(), EMAILS.get(1), null);
  }

  private List<PolicyNotification> createPolicyNotifications() {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, SCAN_ID);
    Policy policy = tempEntity.newPolicy(application);
    policy.getNotifications().add(new UserNotification(EMAILS.get(0), policyEvaluation.getStageTypeId()));
    policy.getNotifications().add(new UserNotification(EMAILS.get(1), policyEvaluation.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyNotification> policyNotifications = new ArrayList<>();
    policyNotifications.add(createPolicyNotification(policy, "hash1"));
    policyNotifications.add(createPolicyNotification(policy, "hash2"));
    policyNotifications.add(createPolicyNotification(policy, "hash3"));
    return policyNotifications;
  }

  private PolicyNotification createPolicyNotification(Policy policy, String hash) {
    PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    OwnerComponent component = tempEntity
        .newApplicationComponent(application.getId(), STAGE_ID, hash, MatchState.EXACT, false);
    policyFact.addComponentFact(new ComponentFact(component.getComponentIdentifier(), component.getHash()));
    return new PolicyNotification(policyFact, policy.getNotifications());
  }

  private void assertApplicationPolicyNotificationAuditData(
      AuditDTO auditDTO,
      int totalPolicyViolationCount,
      String email,
      String error)
  {
    assertStandardData(auditDTO, AuditEvent.SEND_MAIL, error, SYSTEM_USER);
    assertApplicationData(auditDTO, application);
    if (error == null) {
      assertCustomData(auditDTO, "scanId", SCAN_ID);
      assertCustomData(auditDTO, "stageId", STAGE_ID);
      assertCustomData(auditDTO, "totalPolicyViolationCount", totalPolicyViolationCount);
      assertCustomData(auditDTO, "emailAddress", email);
    }
  }
}
