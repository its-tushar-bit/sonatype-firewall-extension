/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PolicyAlertNotifierTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(PolicyAlertNotifier.class);

  @Inject
  private PolicyAlertNotifier notifier;

  @Mock
  private PolicyAlertEmailer policyAlertEmailer;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(PolicyAlertEmailer.class).toInstance(policyAlertEmailer);
  }

  @Test
  public void testLogging_NoNewViolations() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");

    notifier.sendNotifications(app, eval, null);
    logOutput.assertDebug("Not sending notifications for application " + app.getPublicId() + " and scan "
        + eval.getScanId() + " in stage " + eval.getStageTypeId() + ", no new policy violations since last evaluation");
  }

  @Test
  public void test_Notification_Email() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    newPolicyViolationWantingAlerts(app, eval);

    notifier.sendNotifications(app, eval, null);
    verify(policyAlertEmailer, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class),
        anyListOf(PolicyNotification.class));
  }

  /**
   * Ensure alerts are recorded with their violations regardless of the state of sending notifications.
   */
  @Test
  public void testAlertsRecordedWithoutNotificationSent() {
    // Given...
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");

    PolicyViolation violationBeforeAlerting = newPolicyViolationWantingAlerts(app, eval);
    assertThat(violationBeforeAlerting.getNotifications(), empty());

    // When notifier causes an error emailing notifications...
    RuntimeException ex = new RuntimeException("postal strike");
    doThrow(ex).when(policyAlertEmailer).sendNotifications(any(Application.class),
        anyString(), any(Stage.class), anyListOf(PolicyNotification.class));

    notifier.sendNotifications(app, eval, null);
    logOutput.assertError("Email notification failed", ex);

    // Then...
    PolicyViolation violationAfterAlerting = new PolicyViolationDAO().getById(violationBeforeAlerting.getId());
    assertThat(violationAfterAlerting.getNotifications(), not(empty()));
  }

  private PolicyViolation newPolicyViolationWantingAlerts(final Application app, final PolicyEvaluation eval) {
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    policy.getNotifications().add(new UserNotification(emailAddress1, eval.getStageTypeId()));
    policy.getNotifications().add(new UserNotification(emailAddress2, eval.getStageTypeId()));
    policy.getNotifications().add(new UserNotification(emailAddress3, Stage.ID_RELEASE));
    new PolicyDAO().update(policy);
    return tempEntity.newPolicyViolation(eval, policy);
  }
}
