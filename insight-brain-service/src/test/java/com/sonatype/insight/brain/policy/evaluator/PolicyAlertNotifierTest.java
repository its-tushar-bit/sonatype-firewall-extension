/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier.MailPolicyAlertCounts;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.test.LogOutput;

import org.sonatype.micromailer.Address;

import com.google.inject.Binder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

  @Captor
  private ArgumentCaptor<List<Address>> toAddressesArgumentCaptor;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    mailer = mock(InsightMail.class);
    when(mailer.getServer()).thenReturn("localhost:587");
    when(mailer.getCdnUrl()).thenReturn("http://localhost");
    binder.bind(InsightMail.class).toInstance(mailer);
  }

  @Before
  public void before() {
    config.setBaseUrl("http://localhost");
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

    notifier.sendNotifications(app, eval, null);
    log.assertDebug("Not sending notification emails for application " + app.getPublicId() + " and scan "
        + eval.getScanId() + " in stage " + eval.getStageTypeId() + ", no new policy violations since last evaluation");
  }

  @Test
  public void testLogging_NewViolations_NoNotification() {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    tempEntity.newPolicyViolation(eval, policy);

    notifier.sendNotifications(app, eval, null);
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

    notifier.sendNotifications(app, eval, null);
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

    notifier.sendNotifications(app, eval, null);
    log.assertError(
        "Unable to send notification email to " + action.getTarget() + " for application " + app.getPublicId()
            + " and scan " + eval.getScanId() + " in stage " + eval.getStageTypeId(), ex);
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
    verify(mailer, times(2)).sendHtml(anyString(), toAddressesArgumentCaptor.capture(), anyString(), anyString());
    // emailAddress3 should not get a message
    assertEmailAddresses(toAddressesArgumentCaptor, emailAddress1, emailAddress2);
  }

  @Test
  public void test_Monitoring_Notification_Email() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id",
        false /* reevaluation */, true /* forMonitoring */, new Date());
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    policy.addMonitorNotifyAction(new NotifyAction(emailAddress1, null));
    policy.addMonitorNotifyAction(new NotifyAction(emailAddress2, null));
    new PolicyDAO().update(policy);
    tempEntity.newPolicyViolation(eval, policy);

    notifier.sendNotifications(app, eval, null);
    verify(mailer, times(2)).sendHtml(anyString(), toAddressesArgumentCaptor.capture(), anyString(), anyString());
    assertEmailAddresses(toAddressesArgumentCaptor, emailAddress1, emailAddress2);
  }

  @Test
  public void test_Notification_Role() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");

    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    User user1 = tempEntity.newUser("test1", "FirstName1", "LastName1", emailAddress1);
    User user2 = tempEntity.newUser("test2", "FirstName2", "LastName2", emailAddress2);
    tempEntity.newUser("test3", "FirstName3", "LastName3", emailAddress3);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), user1.getUsername());
    tempEntity.newMembershipMapping(app.getOrganizationId(), role.getId(), user2.getUsername());

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id");
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    policy.addAction(eval.getStageTypeId(), new NotifyAction(role.getId(), NotifyActionType.TARGET_TYPE_ROLE));
    new PolicyDAO().update(policy);
    tempEntity.newPolicyViolation(eval, policy);

    notifier.sendNotifications(app, eval, null);
    verify(mailer, times(2)).sendHtml(anyString(), toAddressesArgumentCaptor.capture(), anyString(), anyString());
    // emailAddress3 should not get a message
    assertEmailAddresses(toAddressesArgumentCaptor, emailAddress1, emailAddress2);
  }

  @Test
  public void test_Monitoring_Notification_Role() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");

    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    User user1 = tempEntity.newUser("test1", "FirstName1", "LastName1", emailAddress1);
    User user2 = tempEntity.newUser("test2", "FirstName2", "LastName2", emailAddress2);
    tempEntity.newUser("test3", "FirstName3", "LastName3", emailAddress3);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), user1.getUsername());
    tempEntity.newMembershipMapping(app.getOrganizationId(), role.getId(), user2.getUsername());

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan-id",
        false /* reevaluation */, true /* forMonitoring */, new Date());
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    policy.addMonitorNotifyAction(new NotifyAction(role.getId(), NotifyActionType.TARGET_TYPE_ROLE));
    new PolicyDAO().update(policy);
    tempEntity.newPolicyViolation(eval, policy);

    notifier.sendNotifications(app, eval, null);
    verify(mailer, times(2)).sendHtml(anyString(), toAddressesArgumentCaptor.capture(), anyString(), anyString());
    // emailAddress3 should not get a message
    assertEmailAddresses(toAddressesArgumentCaptor, emailAddress1, emailAddress2);
  }

  private void assertEmailAddresses(ArgumentCaptor<List<Address>> toAddressesArgumentCaptor,
      String... expectedEmailAddresses)
  {
    assertThat(toAddressesArgumentCaptor.getAllValues(), hasSize(expectedEmailAddresses.length));
    Set<String> actualEmailAddresses = new HashSet<>();
    for (List<Address> addresses : toAddressesArgumentCaptor.getAllValues()) {
      for (Address address : addresses) {
        actualEmailAddresses.add(address.getMailAddress());
      }
    }
    assertThat(actualEmailAddresses, containsInAnyOrder(expectedEmailAddresses));
  }
}
