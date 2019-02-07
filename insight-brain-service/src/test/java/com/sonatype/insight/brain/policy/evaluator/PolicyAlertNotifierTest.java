/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
    binder.bind(PolicyAlertEmailer.class).toInstance(policyAlertEmailer);
    super.configure(binder);
  }

  @Test
  public void testLogging_NoNewViolations() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = eval;
    results.notifiableViolations = Arrays.asList();
    results.allViolations = Arrays.asList();

    notifier.sendNotifications(app, results);
    assertThat(logOutput).atDebugLevel()
        .contains("Not sending notifications for application " + app.getPublicId() + " and scan " + eval.getScanId()
            + " in stage " + eval.getStageTypeId() + ", no new policy violations since last evaluation");
  }

  @Test
  public void test_Notification_Email() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    PolicyViolation violation = newPolicyViolationWantingAlerts(app, eval);
    PolicyViolation grandfatheredViolation = tempEntity.newGrandfatheredPolicyViolation(eval, tempEntity.newPolicy());
    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = eval;
    results.notifiableViolations = Arrays.asList(violation);
    results.allViolations = Arrays.asList(violation, grandfatheredViolation);

    notifier.sendNotifications(app, results);
    verify(policyAlertEmailer, times(1)).sendNotifications(eq(app), eq("scan-id"), any(Stage.class), anyList(), eq(1));
  }

  private PolicyViolation newPolicyViolationWantingAlerts(final Application app, final PolicyEvaluation eval) {
    Policy policy = tempEntity.newPolicy(app);
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
