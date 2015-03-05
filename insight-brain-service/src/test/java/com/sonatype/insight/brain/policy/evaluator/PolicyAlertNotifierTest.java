/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
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
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    policy.addAction(eval.getStageTypeId(), new Action(Action.ID_NOTIFY, emailAddress1));
    policy.addAction(eval.getStageTypeId(), new Action(Action.ID_NOTIFY, emailAddress2));
    policy.addAction(Stage.ID_RELEASE, new Action(Action.ID_NOTIFY, emailAddress3));
    new PolicyDAO().update(policy);
    tempEntity.newPolicyViolation(eval, policy);

    notifier.sendNotifications(app, eval, null);
    verify(policyAlertEmailer, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class),
        anyListOf(PolicyAlert.class));
  }
}
