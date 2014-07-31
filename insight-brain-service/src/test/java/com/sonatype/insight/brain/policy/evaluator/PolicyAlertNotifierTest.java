/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier.MailPolicyAlertCounts;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.test.LogOutput;

import org.sonatype.micromailer.Address;

import com.google.inject.Binder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyAlertNotifierTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput log = new LogOutput(PolicyAlertNotifier.class);

  @Inject
  private InsightConfig config;

  @Inject
  private PolicyAlertNotifier notifier;

  private InsightMail mailer;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    mailer = mock(InsightMail.class);
    when(mailer.getServer()).thenReturn("localhost:587");
    when(mailer.getCdnUrl()).thenReturn("http://localhost");
    binder.bind(InsightMail.class).toInstance(mailer);
  }

  @Test
  public void testNotificationEmailSubject() throws Exception {
    Assert.assertEquals("Policy Alert: 1 critical violation out of 15",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(1, 2, 3, 4, 5)));
    Assert.assertEquals("Policy Alert: 2 severe violations out of 14",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(0, 2, 3, 4, 5)));
    Assert.assertEquals("Policy Alert: 3 moderate violations out of 12",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(0, 0, 3, 4, 5)));
    Assert.assertEquals("Policy Alert: 9 neutral violations out of 9",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(0, 0, 0, 4, 5)));
    Assert.assertEquals("Policy Alert: 5 neutral violations out of 5",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(0, 0, 0, 0, 5)));
  }

  @Test
  public void testLogging_NoNewViolations() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");

    notifier.sendNotifications(app.getPublicId(), eval, null);
    log.assertDebug("Not sending notification emails for application " + app.getPublicId() + " and scan "
        + eval.getScanId() + " in stage " + eval.getStageTypeId() + ", no new policy violations since last evaluation");
  }

  @Test
  public void testLogging_NewViolations_NoNotification() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    tempEntity.newPolicyViolation(eval, policy);

    notifier.sendNotifications(app.getPublicId(), eval, null);
    log.assertDebug("Not sending notification emails for application " + app.getPublicId() + " and scan "
        + eval.getScanId() + " in stage " + eval.getStageTypeId()
        + ", no recipients configured for any violated policy");
  }

  @Test
  public void testLogging_NewViolations_Notification() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    Action action = new Action(Action.ID_NOTIFY, "test@sonatype.com");
    policy.addAction(eval.getStageTypeId(), action);
    new PolicyDAO().update(policy);
    tempEntity.newPolicyViolation(eval, policy);

    notifier.sendNotifications(app.getPublicId(), eval, null);
    log.assertDebug("Sending notification email via " + mailer.getServer() + " to " + action.getTarget()
        + " for application " + app.getPublicId() + " and scan " + eval.getScanId() + " in stage "
        + eval.getStageTypeId());
  }

  @Test
  public void testLogging_NewViolations_Notification_Error() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    Action action = new Action(Action.ID_NOTIFY, "test@sonatype.com");
    policy.addAction(eval.getStageTypeId(), action);
    new PolicyDAO().update(policy);
    tempEntity.newPolicyViolation(eval, policy);
    config.setBaseUrl("http://localhost");

    Exception ex = new RuntimeException();
    doThrow(ex).when(mailer).sendHtml(anyString(), anyListOf(Address.class), anyString(), anyString());

    notifier.sendNotifications(app.getPublicId(), eval, null);
    log.assertError(
        "Unable to send notification email to " + action.getTarget() + " for application " + app.getPublicId()
            + " and scan " + eval.getScanId() + " in stage " + eval.getStageTypeId(), ex);
  }
}
