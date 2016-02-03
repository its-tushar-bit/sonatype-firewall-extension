/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PolicyAlertNotifierTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput log = new LogOutput(PolicyAlertNotifier.class);

  @Inject
  private PolicyAlertNotifier notifier;

  private PolicyAlertEmailer policyAlertEmailer;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    policyAlertEmailer = mock(PolicyAlertEmailer.class);
    binder.bind(PolicyAlertEmailer.class).toInstance(policyAlertEmailer);
  }

  @Test
  public void testLogging_NoNewViolations() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");

    notifier.sendNotifications(app, eval, null);
    log.assertDebug("Not sending notification emails for application " + app.getPublicId() + " and scan "
        + eval.getScanId() + " in stage " + eval.getStageTypeId() + ", no new policy violations since last evaluation");
  }

  @Test
  public void test_Notification_Email() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    newPolicyViolationWantingAlerts(app, eval);

    notifier.sendNotifications(app, eval, null);
    verify(policyAlertEmailer, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class),
        anyListOf(PolicyAlert.class));
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
    doThrow(new RuntimeException("postal strike")).when(policyAlertEmailer).sendNotifications(any(Application.class),
        anyString(), any(Stage.class), anyListOf(PolicyAlert.class));
    try {
      notifier.sendNotifications(app, eval, null);
      fail("Expected exception");
    }
    catch (Exception expected) {
      // expected exception, check the content to help in debugging test failures
      assertThat(expected.getMessage(), equalTo("postal strike"));
    }

    // Then...
    PolicyViolation violationAfterAlerting = new PolicyViolationDAO().getById(violationBeforeAlerting.getId());
    assertThat(violationAfterAlerting.getNotifications(), not(empty()));
  }

  private PolicyViolation newPolicyViolationWantingAlerts(final Application app, final PolicyEvaluation eval) {
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    policy.addAction(eval.getStageTypeId(), new Action(Action.ID_NOTIFY, emailAddress1));
    policy.addAction(eval.getStageTypeId(), new Action(Action.ID_NOTIFY, emailAddress2));
    policy.addAction(Stage.ID_RELEASE, new Action(Action.ID_NOTIFY, emailAddress3));
    new PolicyDAO().update(policy);
    return tempEntity.newPolicyViolation(eval, policy);
  }
}
