/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.jira.JiraPolicyAlertNotifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class PolicyAlertNotifierTest
    extends AbstractComponentH2Test
{
  public LogOutput logOutput = new LogOutput(PolicyAlertNotifier.class);

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyAlertNotifier notifier;

  @Mock
  private PolicyAlertEmailer policyAlertEmailer;

  @Mock
  private JiraPolicyAlertNotifier jiraPolicyAlertNotifier;

  @Mock
  private PolicyAlertScmNotifier policyAlertScmNotifier;

  @Test
  public void testLogging_NoNewViolations() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = eval;
    results.notifiableViolations = Collections.emptyList();
    results.allViolations = Collections.emptyList();

    notifier.sendNotifications(app, results);
    assertThat(logOutput).atDebugLevel()
        .contains("Not sending notifications for application " + app.getPublicId() + " and scan " + eval.getScanId()
            + " in stage " + eval.getStageTypeId() + ", no new policy violations for policies " +
            "configured to send notifications since last evaluation.");
  }

  @Test
  public void test_Notification_SuccessfulAllNotifiers() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    PolicyViolation violation = newPolicyViolationWantingAlerts(app, eval);
    PolicyViolation legacyViolation = tempEntity.newLegacyPolicyViolation(eval, tempEntity.newPolicy());
    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = eval;
    results.notifiableViolations = Collections.singletonList(violation);
    results.allViolations = Arrays.asList(violation, legacyViolation);

    // when we send a notification
    notifier.sendNotifications(app, results);

    // then see the notifications go to email
    verify(policyAlertEmailer, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList(), eq(1),
        eq(eval.isForMonitoring()));

    // and see the notifications go to jira
    verify(jiraPolicyAlertNotifier, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList());

    // and see the notifications go to source control
    verify(policyAlertScmNotifier, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList(),
        any());
  }

  @Test
  public void test_Notification_ExceptionsFromNotifiers() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    PolicyViolation violation = newPolicyViolationWantingAlerts(app, eval);
    PolicyViolation legacyViolation = tempEntity.newLegacyPolicyViolation(eval, tempEntity.newPolicy());
    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = eval;
    results.notifiableViolations = Collections.singletonList(violation);
    results.allViolations = Arrays.asList(violation, legacyViolation);

    // given each notifier throws an exception
    doThrow(new RuntimeException("oh no in email!")).when(policyAlertEmailer)
        .sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList(), eq(1),
            eq(eval.isForMonitoring()));
    doThrow(new RuntimeException("oh no in jira!")).when(jiraPolicyAlertNotifier)
        .sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList());
    doThrow(new RuntimeException("oh no in scm!")).when(policyAlertScmNotifier)
        .sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList(), any());

    // when we send a notification
    notifier.sendNotifications(app, results);

    // then see the notifications go to email
    verify(policyAlertEmailer, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList(), eq(1),
        eq(eval.isForMonitoring()));

    // and see the notifications still go to jira
    verify(jiraPolicyAlertNotifier, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList());

    // and see the notifications still go to source control
    verify(policyAlertScmNotifier, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList(),
        any());

    // and we see the exceptions logged
    assertThat(logOutput).atErrorLevel().contains("Email notification failed");
    assertThat(logOutput).atErrorLevel().contains("JIRA notification failed");
    assertThat(logOutput).atErrorLevel().contains("Source Control notification failed");
  }

  private PolicyViolation newPolicyViolationWantingAlerts(final Application app, final PolicyEvaluation eval) {
    Policy policy = tempEntity.newPolicy(app);
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    policy.getNotifications().add(new UserNotification(emailAddress1, eval.getStageTypeId()));
    policy.getNotifications().add(new UserNotification(emailAddress2, eval.getStageTypeId()));
    policy.getNotifications().add(new UserNotification(emailAddress3, Stage.ID_RELEASE));
    policyDAO.update(policy);
    return tempEntity.newPolicyViolation(eval, policy);
  }
}
