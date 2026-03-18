/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentAuditTest;
import com.sonatype.insight.brain.service.InsightMail;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class RepositoryPolicyAlertEmailerAuditTest
    extends AbstractComponentAuditTest
{
  private static final List<String> EMAILS = Arrays.asList("test1@sonatype.com", "test2@sonatype.com");

  private static final Comparator<AuditDTO> EMAIL_COMPARATOR = Comparator
      .comparing(auditDTO -> (String) auditDTO.data.getOrDefault("emailAddress", ""));

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer;

  @Mock
  private InsightMail mockInsightMail;

  private Repository repository;

  @Override
  public void configure(Binder binder) {
    binder.bind(InsightMail.class).toInstance(mockInsightMail);
    super.configure(binder);
  }

  @Before
  public void before() {
    setBaseUrl("http://localhost");
    when(mockInsightMail.getCdnUrl()).thenReturn("https://cdn.sonatype.com/");
    repository = tempEntity.newRepository();
  }

  @Test
  public void testSendNotifications() {
    List<PolicyNotification> policyNotifications = createPolicyNotifications();

    repositoryPolicyAlertEmailer.sendNotifications(repository, policyNotifications);

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.SEND_MAIL, 2);
    auditDTOs.sort(EMAIL_COMPARATOR);
    assertRepositoryPolicyNotificationAuditData(auditDTOs.get(0), policyNotifications.size(), EMAILS.get(0), null);
    assertRepositoryPolicyNotificationAuditData(auditDTOs.get(1), policyNotifications.size(), EMAILS.get(1), null);
  }

  @Test
  public void testSendNotifications_MailException() {
    List<PolicyNotification> policyNotifications = createPolicyNotifications();
    doThrow(new RuntimeException()).doNothing().when(mockInsightMail).sendHtml(any(), anyString(), anyString());

    repositoryPolicyAlertEmailer.sendNotifications(repository, policyNotifications);

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.SEND_MAIL, 2);
    auditDTOs.sort(EMAIL_COMPARATOR);
    assertRepositoryPolicyNotificationAuditData(auditDTOs.get(0), policyNotifications.size(), EMAILS.get(0),
        "server-error");
    assertRepositoryPolicyNotificationAuditData(auditDTOs.get(1), policyNotifications.size(), EMAILS.get(1), null);
  }

  private List<PolicyNotification> createPolicyNotifications() {
    Policy policy = tempEntity.newPolicy();
    policy.getNotifications().add(new UserNotification(EMAILS.get(0), ProxyStageType.ID));
    policy.getNotifications().add(new UserNotification(EMAILS.get(1), ProxyStageType.ID));
    policyDAO.update(policy);
    List<PolicyNotification> policyNotifications = new ArrayList<>();
    policyNotifications.add(createPolicyNotification(policy, "hash1"));
    policyNotifications.add(createPolicyNotification(policy, "hash2"));
    policyNotifications.add(createPolicyNotification(policy, "hash3"));
    return policyNotifications;
  }

  private PolicyNotification createPolicyNotification(Policy policy, String hash) {
    PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository, hash);
    policyFact.addComponentFact(new ComponentFact(component.getComponentIdentifier(), component.getHash()));
    return new PolicyNotification(policyFact, policy.getNotifications());
  }

  private void assertRepositoryPolicyNotificationAuditData(
      AuditDTO auditDTO,
      int totalPolicyViolationCount,
      String email,
      String error)
  {
    assertStandardData(auditDTO, AuditEvent.SEND_MAIL, error, SYSTEM_USER);
    assertRepositoryData(auditDTO, repository);
    if (error == null) {
      assertCustomData(auditDTO, "totalPolicyViolationCount", totalPolicyViolationCount);
      assertCustomData(auditDTO, "emailAddress", email);
    }
  }
}
