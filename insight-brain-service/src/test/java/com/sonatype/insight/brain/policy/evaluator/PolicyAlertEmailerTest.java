/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.test.LogOutput;

import org.sonatype.micromailer.Address;

import com.google.inject.Binder;
import org.junit.After;
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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PolicyAlertEmailerTest
    extends AbstractComponentTest
{
  private static final int NOTIFICATION_WAIT_TIMEOUT = 5000; // millisecs

  @Rule
  public LogOutput log = new LogOutput(PolicyAlertEmailer.class);

  @Inject
  private InsightConfig config;

  @Inject
  private PolicyAlertEmailer policyAlertEmailer;

  private InsightMail mailer;

  @Captor
  private ArgumentCaptor<List<Address>> toAddressesArgumentCaptor;

  private LdapServer serverDetails;

  @Rule
  public TestLdapServer ldapServer = new TestLdapServer();

  @Inject
  private LdapManager manager;

  private PolicyDAO policyDAO = new PolicyDAO();

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

  @After
  public void cleanup() throws Exception {
    if (serverDetails != null) {
      new LdapServerDAO().delete(serverDetails);
    }
  }

  @Test
  public void testNotificationEmailSubject() throws Exception {
    Assert.assertEquals("Policy Alert: 1 critical violation out of 15",
        PolicyAlertEmailer.createPolicyMailSubject(new PolicyAlertEmailer.MailPolicyAlertCounts(1, 2, 3, 4, 5)));
    Assert.assertEquals("Policy Alert: 2 severe violations out of 14",
        PolicyAlertEmailer.createPolicyMailSubject(new PolicyAlertEmailer.MailPolicyAlertCounts(0, 2, 3, 4, 5)));
    Assert.assertEquals("Policy Alert: 3 moderate violations out of 12",
        PolicyAlertEmailer.createPolicyMailSubject(new PolicyAlertEmailer.MailPolicyAlertCounts(0, 0, 3, 4, 5)));
    Assert.assertEquals("Policy Alert: 9 neutral violations out of 9",
        PolicyAlertEmailer.createPolicyMailSubject(new PolicyAlertEmailer.MailPolicyAlertCounts(0, 0, 0, 4, 5)));
    Assert.assertEquals("Policy Alert: 5 neutral violations out of 5",
        PolicyAlertEmailer.createPolicyMailSubject(new PolicyAlertEmailer.MailPolicyAlertCounts(0, 0, 0, 0, 5)));
  }

  @Test
  public void testLogging_NewViolations_NoNotification() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyViolations, eval.getStageTypeId(),
        eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyAlerts);

    log.assertDebug(
        "Not sending notification emails for application " + app.getPublicId() + " and scan " + eval.getScanId()
            + " in stage " + eval.getStageTypeId() + ", no recipients configured for any violated policy",
        NOTIFICATION_WAIT_TIMEOUT);
  }

  @Test
  public void testLogging_NewViolations_Notification() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    Action action = new Action(Action.ID_NOTIFY, "test@sonatype.com");
    policy.addAction(eval.getStageTypeId(), action);
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));

    List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyViolations, eval.getStageTypeId(),
        eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyAlerts);

    log.assertDebug(
        "Sending notification email via " + mailer.getServer() + " to " + action.getTarget() + " for application "
            + app.getPublicId() + " and scan " + eval.getScanId() + " in stage " + eval.getStageTypeId(),
        NOTIFICATION_WAIT_TIMEOUT);
  }

  @Test
  public void testLogging_NewViolations_Notification_Error() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    Action action = new Action(Action.ID_NOTIFY, "test@sonatype.com");
    policy.addAction(eval.getStageTypeId(), action);
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyViolations, eval.getStageTypeId(),
        eval.isForMonitoring());

    config.setBaseUrl("http://localhost");

    Exception ex = new RuntimeException();
    doThrow(ex).when(mailer).sendHtml(anyString(), anyListOf(Address.class), anyString(), anyString());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyAlerts);

    log.assertError(
        "Unable to send notification email to " + action.getTarget() + " for application " + app.getPublicId()
            + " and scan " + eval.getScanId() + " in stage " + eval.getStageTypeId(), ex, NOTIFICATION_WAIT_TIMEOUT);
  }

  @Test
  public void test_Notification_Email() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    policy.addAction(eval.getStageTypeId(), new Action(Action.ID_NOTIFY, emailAddress1));
    policy.addAction(eval.getStageTypeId(), new Action(Action.ID_NOTIFY, emailAddress2));
    policy.addAction(Stage.ID_RELEASE, new Action(Action.ID_NOTIFY, emailAddress3));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyViolations, eval.getStageTypeId(),
        eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyAlerts);
    // emailAddress3 should not get a message
    assertEmailAddresses(emailAddress1, emailAddress2);
  }

  @Test
  public void test_Monitoring_Notification_Email() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId,
        false /* reevaluation */, true /* forMonitoring */, new Date());
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    policy.addMonitorNotifyAction(new NotifyAction(emailAddress1, null));
    policy.addMonitorNotifyAction(new NotifyAction(emailAddress2, null));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyViolations, eval.getStageTypeId(),
        eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyAlerts);
    assertEmailAddresses(emailAddress1, emailAddress2);
  }

  @Test
  public void test_Notification_Role() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    String emailAddress4 = "test4@sonatype.com";
    User user1 = tempEntity.newUser("test1", "FirstName1", "LastName1", emailAddress1);
    User user2 = tempEntity.newUser("test2", "FirstName2", "LastName2", emailAddress2);
    User user3 = tempEntity.newUser("test3", "FirstName3", "LastName3", emailAddress3);
    tempEntity.newUser("test4", "FirstName4", "LastName4", emailAddress4);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), user1.getUsername());
    tempEntity.newMembershipMapping(app.getOrganizationId(), role.getId(), user2.getUsername());
    tempEntity.newMembershipMapping(org.getParentOwnerId(), role.getId(), user3.getUsername());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    policy.addAction(eval.getStageTypeId(), new NotifyAction(role.getId(), NotifyActionType.TARGET_TYPE_ROLE));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyViolations, eval.getStageTypeId(),
        eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyAlerts);
    // emailAddress4 should not get a message
    assertEmailAddresses(emailAddress1, emailAddress2, emailAddress3);
  }

  @Test
  public void test_Notification_Role_WithGroups() throws Exception {
    startLdapServer();
    setSearchBase();

    LdapUserMapping ldapUserMapping = createUserMapping();
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("departmentNumber");
    new LdapUserMappingDAO().insert(ldapUserMapping);

    Application app = tempEntity.newApplicationWithParent("test");

    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    String groupName = "ab";
    tempEntity.newMembershipMapping(app.getId(), role.getId(), groupName, MemberType.GROUP);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    policy.addAction(eval.getStageTypeId(), new NotifyAction(role.getId(), NotifyActionType.TARGET_TYPE_ROLE));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyViolations, eval.getStageTypeId(),
        eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyAlerts);
    assertEmailAddresses("test.user@company.com", "test.user2@company.com", "test.user3@company.com");
  }

  @Test
  public void test_Monitoring_Notification_Role() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    String emailAddress4 = "test4@sonatype.com";
    User user1 = tempEntity.newUser("test1", "FirstName1", "LastName1", emailAddress1);
    User user2 = tempEntity.newUser("test2", "FirstName2", "LastName2", emailAddress2);
    User user3 = tempEntity.newUser("test3", "FirstName3", "LastName3", emailAddress3);
    tempEntity.newUser("test4", "FirstName4", "LastName4", emailAddress4);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), user1.getUsername());
    tempEntity.newMembershipMapping(app.getOrganizationId(), role.getId(), user2.getUsername());
    tempEntity.newMembershipMapping(org.getParentOwnerId(), role.getId(), user3.getUsername());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId,
        false /* reevaluation */, true /* forMonitoring */, new Date());
    Policy policy = tempEntity.newPolicy(app.getId(), "test");
    policy.addMonitorNotifyAction(new NotifyAction(role.getId(), NotifyActionType.TARGET_TYPE_ROLE));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyViolations, eval.getStageTypeId(),
        eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyAlerts);
    // emailAddress4 should not get a message
    assertEmailAddresses(emailAddress1, emailAddress2, emailAddress3);
  }

  private void assertEmailAddresses(String... expectedEmailAddresses) {
    verify(mailer, timeout(NOTIFICATION_WAIT_TIMEOUT).times(expectedEmailAddresses.length)).sendHtml(anyString(),
        toAddressesArgumentCaptor.capture(), anyString(), anyString());

    assertThat(toAddressesArgumentCaptor.getAllValues(), hasSize(expectedEmailAddresses.length));
    Set<String> actualEmailAddresses = new HashSet<>();
    for (List<Address> addresses : toAddressesArgumentCaptor.getAllValues()) {
      for (Address address : addresses) {
        actualEmailAddresses.add(address.getMailAddress());
      }
    }
    assertThat(actualEmailAddresses, containsInAnyOrder(expectedEmailAddresses));
  }

  private void startLdapServer() throws Exception {
    serverDetails = tempEntity.newLdapServer("Test Server");

    ldapServer.start();
    ldapServer.loadData("/ldap_users.ldif");
  }

  protected LdapConnection createLdapConnection() {
    LdapConnection conn = manager.loadConnection(serverDetails.getId());
    conn.setServerId(serverDetails.getId());
    conn.setProtocol(LdapProtocol.LDAP);
    if (ldapServer != null) {
      conn.setHostname(ldapServer.getHostname());
      conn.setPort(ldapServer.getPort());
    }
    return conn;
  }

  private void setSearchBase() {
    LdapConnection conn = createLdapConnection();
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);
  }

  private LdapUserMapping createUserMapping() {
    LdapUserMapping umap = new LdapUserMapping();
    umap.setServerId(serverDetails.getId());
    umap.setUserBaseDN("ou=users");
    umap.setUserObjectClass("person");
    umap.setUserIDAttribute("uid");
    umap.setUserRealNameAttribute("cn");
    umap.setUserEmailAttribute("mail");
    umap.setUserSubtree(true);
    umap.setGroupBaseDN("ou=groups");
    umap.setGroupIDAttribute("cn");
    umap.setGroupSubtree(true);
    return umap;
  }
}
