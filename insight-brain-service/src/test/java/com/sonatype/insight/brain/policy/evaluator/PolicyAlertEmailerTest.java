/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import javax.naming.NamingException;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.organization.ApplicationContactLoader;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.security.CrowdClient;
import com.sonatype.insight.brain.security.CrowdClientFactory;
import com.sonatype.insight.brain.security.CrowdRealm;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import com.atlassian.crowd.exception.OperationFailedException;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyAlertEmailerTest
    extends AbstractComponentTest
{
  private static final Duration NOTIFICATION_WAIT_TIMEOUT = Duration.ofMillis(5000);

  @Rule
  public LogOutput logOutput = new LogOutput(1, PolicyAlertEmailer.class);

  @Inject
  private PolicyAlertEmailer policyAlertEmailer;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private UserDirectory userDirectory;

  @Inject
  private SsoUserService ssoUserService;

  @Mock
  private InsightMail mailer;

  @Captor
  private ArgumentCaptor<String> toAddressesArgumentCaptor;

  @Captor
  private ArgumentCaptor<String> emailSubjectArgumentCaptor;

  @Rule
  public TestLdapServer testLdapServer1 = new TestLdapServer();

  @Rule
  public TestLdapServer testLdapServer2 = new TestLdapServer();

  @Inject
  private LdapService ldapService;

  @Mock
  private CrowdClientFactory mockCrowdClientFactory;

  @Inject
  private PolicyNotificationUtil policyNotificationUtil;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private LdapServerDAO ldapServerDAO;

  @Inject
  private UserDAO userDAO;

  @Inject
  private MembershipMappingDAO membershipMappingDAO;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private LdapUserMappingDAO ldapUserMappingDAO;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Captor
  private ArgumentCaptor<Thread> threadArgumentCaptor;

  @Inject
  ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  OrganizationDAO organizationDAO;

  @Override
  public void configure(Binder binder) {
    lenient().when(mailer.getServer()).thenReturn("localhost:587");
    lenient().when(mailer.getCdnUrl()).thenReturn("https://cdn.sonatype.com/");
    binder.bind(InsightMail.class).toInstance(mailer);
    binder.bind(CrowdClientFactory.class).toInstance(mockCrowdClientFactory);
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
    super.configure(binder);
  }

  @Before
  public void before() {
    setBaseUrl("http://localhost");
  }

  @Test
  public void testNotificationEmailSubject() {
    String ownerName = "ownerName";
    assertThat(
        policyAlertEmailer.createPolicyMailSubject(new PolicyAlertCounts(1, 2, 3, 4, 5), ownerName, StageTypes.BUILD))
            .isEqualTo("Policy Alert for ownerName at stage Build: 1 critical violation out of 15");
    assertThat(
        policyAlertEmailer.createPolicyMailSubject(new PolicyAlertCounts(0, 2, 3, 4, 5), ownerName, StageTypes.BUILD))
            .isEqualTo("Policy Alert for ownerName at stage Build: 2 severe violations out of 14");
    assertThat(
        policyAlertEmailer.createPolicyMailSubject(new PolicyAlertCounts(0, 0, 3, 4, 5), ownerName, StageTypes.BUILD))
            .isEqualTo("Policy Alert for ownerName at stage Build: 3 moderate violations out of 12");
    assertThat(
        policyAlertEmailer.createPolicyMailSubject(new PolicyAlertCounts(0, 0, 0, 4, 5), ownerName, StageTypes.BUILD))
            .isEqualTo("Policy Alert for ownerName at stage Build: 9 neutral violations out of 9");
    assertThat(
        policyAlertEmailer.createPolicyMailSubject(new PolicyAlertCounts(0, 0, 0, 0, 5), ownerName, StageTypes.RELEASE))
            .isEqualTo("Policy Alert for ownerName at stage Release: 5 neutral violations out of 5");
  }

  @Test
  public void testLogging_NewViolations_NoNotification() {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    Policy policy = tempEntity.newPolicy(app);
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());

    await().atMost(NOTIFICATION_WAIT_TIMEOUT)
        .untilAsserted(() -> assertThat(logOutput).atDebugLevel()
            .contains(
                "Not sending notification emails for application " + app.getPublicId() + " and scan " + eval.getScanId()
                    + " in stage " + eval.getStageTypeId()
                    + ". There are either no recipients configured, or no new policy violations "
                    + "for policies configured to send notifications"));
    verify(mockShutdownHandler).add(threadArgumentCaptor.capture(), eq(ShutdownPriority.NOTIFICATIONS));
    assertThat(threadArgumentCaptor.getValue().getName()).startsWith("PolicyAlertEmailNotifierForScan");
  }

  @Test
  public void testLogging_NewViolations_Notification() {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    String email = "test@sonatype.com";
    policy.getNotifications().add(new UserNotification(email, eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));

    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());

    await().atMost(NOTIFICATION_WAIT_TIMEOUT)
        .untilAsserted(() -> assertThat(logOutput).atDebugLevel()
            .contains("Sending notification email via " + mailer.getServer() + " to " + email + " for application "
                + app.getPublicId() + " and scan " + eval.getScanId() + " in stage " + eval.getStageTypeId()));
  }

  @Test
  public void testLogging_NewViolations_Notification_Error() {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    String email = "test@sonatype.com";
    policy.getNotifications().add(new UserNotification(email, eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    Exception ex = new RuntimeException();
    doThrow(ex).when(mailer).sendHtml(anyString(), anyString(), anyString());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());

    await().atMost(NOTIFICATION_WAIT_TIMEOUT)
        .untilAsserted(() -> assertThat(logOutput).atErrorLevel()
            .contains("Unable to send notification email to " + email + " for application " + app.getPublicId()
                + " and scan " + eval.getScanId() + " in stage " + eval.getStageTypeId(), ex));
  }

  @Test
  public void testSendNotifications_Notification_Email() {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    policy.getNotifications().add(new UserNotification(emailAddress1, eval.getStageTypeId()));
    policy.getNotifications().add(new UserNotification(emailAddress2, eval.getStageTypeId()));
    policy.getNotifications().add(new UserNotification(emailAddress3, Stage.ID_RELEASE));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    // emailAddress3 should not get a message
    assertEmailAddresses(emailAddress1, emailAddress2);
    assertEmailSubject("Policy Alert for " + app.getName() + " at stage Build: 1 severe violation out of 1");
  }

  @Test
  public void testSendNotifications_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);

    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    String emailAddress1 = "test1@sonatype.com";
    policy.getNotifications().add(new UserNotification(emailAddress1, eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    verify(mailer, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis()).times(0)).sendHtml(any(), anyString(), anyString());
  }

  @Test
  public void testSendNotifications_Monitoring_Notification_Email() {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId,
        false /* reevaluation */, true /* forMonitoring */, new Date());
    Policy policy = tempEntity.newPolicy(app);
    String emailAddress1 = "test1@sonatype.com";
    String emailAddress2 = "test2@sonatype.com";
    String emailAddress3 = "test3@sonatype.com";
    String emailAddress4 = "test4@sonatype.com";
    policy.getNotifications().add(new UserNotification(emailAddress1, Notification.CONTINUOUS_MONITORING));
    policy.getNotifications().add(new UserNotification(emailAddress2, Notification.CONTINUOUS_MONITORING));
    policy.getNotifications().add(new UserNotification(emailAddress3, Notification.SBOM_CONTINUOUS_MONITORING));
    policy.getNotifications().add(new UserNotification(emailAddress4, Notification.SBOM_CONTINUOUS_MONITORING));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    assertEmailAddresses(emailAddress1, emailAddress2, emailAddress3, emailAddress4);
    assertEmailSubject(
        "Continuous Monitoring: Policy Alert for " + app.getName() + " at stage Build: 1 severe violation out of 1");
  }

  @Test
  public void test_Notification_Role() {
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
    Policy policy = tempEntity.newPolicy(app);
    policy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    // emailAddress4 should not get a message
    assertEmailAddresses(emailAddress1, emailAddress2, emailAddress3);
  }

  /**
   * See CLM-8161.
   */
  @Test
  public void test_Notification_Role_ObservesEmailChange() {
    Application app = tempEntity.newApplicationWithParent();
    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    String oldAddress = "oldaddress@sonatype.com";

    User user = tempEntity.newUser("test", "FirstName", "LastName", oldAddress);

    tempEntity.newMembershipMapping(app.getId(), role.getId(), user.getUsername());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    policy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = Collections.singletonList(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    verify(mailer, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis())).sendHtml(anyString(), anyString(), anyString());

    String newAddress = "newaddress@sonatype.com";
    user.setEmail(newAddress);
    userDAO.update(user);

    policyAlertEmailer.sendNotifications(app, "scan-id2", stage, policyNotifications, 0, eval.isForMonitoring());
    assertEmailAddresses(oldAddress, newAddress);
  }

  /**
   * See CLM-8161.
   */
  @Test
  public void test_Notification_Role_ObservesLdapUserEmailChange() throws Exception {
    startLdapServer1();

    Application app = tempEntity.newApplicationWithParent("test");

    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), "xb", MemberType.GROUP);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    policy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = Collections.singletonList(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    verify(mailer, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis())).sendHtml(anyString(), anyString(), anyString());

    testLdapServer1.loadData("/PolicyAlertEmailerTest/alter_testuser1_1_email.ldif");

    policyAlertEmailer.sendNotifications(app, "scan-id2", stage, policyNotifications, 0, eval.isForMonitoring());
    assertEmailAddresses("test.user1_1@company.com", "test.user1_1modified@company.com");
  }

  @Test
  public void test_Notification_Role_WithGroups() throws Exception {
    startLdapServer1();
    startLdapServer2();

    Application app = tempEntity.newApplicationWithParent("test");

    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    String groupName = "ab";
    tempEntity.newMembershipMapping(app.getId(), role.getId(), groupName, MemberType.GROUP);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    policy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    assertEmailAddresses("test.user1_1@company.com", "test.user2_1@company.com", "test.user3_1@company.com",
        "test.user1_2@company.com", "test.user2_2@company.com", "test.user3_2@company.com");
  }

  @Test
  public void test_Notification_Role_WithGroups_Static() throws Exception {
    startLdapServer1(true);
    startLdapServer2(true);

    Application app = tempEntity.newApplicationWithParent("test");

    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    String groupName = "Epsilon";
    tempEntity.newMembershipMapping(app.getId(), role.getId(), groupName, MemberType.GROUP);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    policy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    assertEmailAddresses("test.user1_1@company.com", "test.user2_1@company.com", "test.user1_2@company.com",
        "test.user2_2@company.com");
  }

  @Test
  public void test_Notification_Role_WithGroups_WithOneLdapServerFailure() throws Exception {
    startLdapServer1();
    startLdapServer2();

    Application app = tempEntity.newApplicationWithParent("test");

    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    String groupName = "ab";
    tempEntity.newMembershipMapping(app.getId(), role.getId(), groupName, MemberType.GROUP);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    policy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());
    List<LdapServer> ldapServers = ldapServerDAO.getAll();
    assertThat(ldapServers).hasSize(2);

    Throwable expectedException = new NamingException("Naming exception!");
    LdapService ldapServiceSpy = Mockito.spy(ldapService);
    doThrow(expectedException).when(ldapServiceSpy)
        .getUsersByGroup(argThat(new SameId(ldapServers.get(0))), any(String.class));

    UserDirectory userDirectory =
        new UserDirectory(userDAO, ldapServerDAO, ssoUserService, ldapServiceSpy, mockCrowdClientFactory);
    PolicyAlertEmailResolver policyAlertEmailResolver = new PolicyAlertEmailResolver(
        userDirectory,
        ldapServiceSpy,
        ownerDAO,
        ssoUserService,
        membershipMappingDAO,
        ldapServerDAO,
        mockCrowdClientFactory);
    PolicyAlertEmailer undertest = new PolicyAlertEmailer(
        mailer,
        lookup(BaseUrl.class),
        userDirectory,
        policyAlertEmailResolver,
        new AuditRecorder(null),
        testProductLicense,
        mockShutdownHandler,
        thirdPartySbomMetadataDAO,
        organizationDAO);

    undertest.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    // make sure emails from server 2 still go out
    assertEmailAddresses("test.user1_2@company.com", "test.user2_2@company.com", "test.user3_2@company.com");
    assertThat(logOutput).atErrorLevel()
        .contains("Cannot send notifications to members of group " + groupName
            + " using ldap server " + ldapServers.get(0).getName(), expectedException);
  }

  @Test
  public void test_Monitoring_Notification_Role() {
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
    Policy policy = tempEntity.newPolicy(app);
    policy.getNotifications()
        .add(new RoleNotification(role.getId(), role.getName(), Notification.CONTINUOUS_MONITORING));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
    // emailAddress4 should not get a message
    assertEmailAddresses(emailAddress1, emailAddress2, emailAddress3);
  }

  @Test
  public void test_Notification_Email_JiraNotificationsOnly() {
    Application app = tempEntity.newApplicationWithParent("test");
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);

    policy.getNotifications().add(new JiraNotification("projectKey", 10, eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());

    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());

    // No email should be sent out with only Jira Notification
    await().atMost(NOTIFICATION_WAIT_TIMEOUT)
        .untilAsserted(() -> assertThat(logOutput).atDebugLevel()
            .contains(
                "Not sending notification emails for application " + app.getPublicId() + " and scan " + eval.getScanId()
                    + " in stage " + eval.getStageTypeId()
                    + ". There are either no recipients configured, or no new policy violations "
                    + "for policies configured to send notifications"));
  }

  @Test
  public void testSendNotifications_Role_Crowd_NullCrowdClient() {
    sendRoleNotifications("group1");

    assertEmailAddresses();
  }

  @Test
  public void testSendNotifications_Role_Crowd_NoResults() throws Exception {
    String groupName = "group1";
    CrowdClient mockCrowdClient = Mockito.mock(CrowdClient.class);
    when(mockCrowdClient.searchGroupsByGroupNames(any())).thenReturn(
        Collections.singleton(new Member(MemberType.GROUP, "group1", "group1", null, CrowdRealm.ID)));
    when(mockCrowdClient.getUsersByGroupName(any())).thenReturn(Collections.emptySet());
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    sendRoleNotifications(groupName);

    verify(mockCrowdClient, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis()).times(1)).getUsersByGroupName(groupName);
    assertEmailAddresses();
  }

  @Test
  public void testSendNotifications_Role_Crowd() throws Exception {
    CrowdClient mockCrowdClient = Mockito.mock(CrowdClient.class);
    List<Member> members1 = Arrays.asList(
        new Member(MemberType.USER, "username1", "displayName1", "email1", CrowdRealm.ID),
        new Member(MemberType.USER, "username2", "displayName2", null, CrowdRealm.ID),
        new Member(MemberType.USER, "username3", "displayName3", "", CrowdRealm.ID),
        new Member(MemberType.USER, "username4", "displayName4", " ", CrowdRealm.ID));
    when(mockCrowdClient.getUsersByGroupName("group1")).thenReturn(new LinkedHashSet<>(members1));
    List<Member> members2 =
        Collections.singletonList(new Member(MemberType.USER, "username5", "displayName5", "email5", CrowdRealm.ID));
    when(mockCrowdClient.searchGroupsByGroupNames(any())).thenReturn(new LinkedHashSet<>(Arrays.asList(
        new Member(MemberType.GROUP, "group1", "group1", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group2", "group2", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group3", "group3", null, CrowdRealm.ID))));
    when(mockCrowdClient.getUsersByGroupName("group2")).thenReturn(new LinkedHashSet<>(members2));
    when(mockCrowdClient.getUsersByGroupName("group3")).thenThrow(new OperationFailedException());
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    sendRoleNotifications("group1", "group2", "group3");

    assertEmailAddresses("email1", "email5");
    await().atMost(NOTIFICATION_WAIT_TIMEOUT)
        .untilAsserted(() -> assertThat(logOutput).atErrorLevel()
            .contains("Cannot send notifications to members of group group3 using Crowd server."));
  }

  @Test
  public void testSendNotifications_Role_Saml() {
    enableSsoWithSaml();

    String uuid = TemporaryEntity.uuid();
    SamlUser samlUser1 = tempEntity.newSamlUser("username1" + uuid, null, null, "email1", null);
    SamlUser samlUser2 = tempEntity.newSamlUser("username2" + uuid, null, null, null, null);
    SamlUser samlUser3 = tempEntity.newSamlUser("username3" + uuid, null, null, "", null);
    SamlUser samlUser4 = tempEntity.newSamlUser("username4" + uuid, null, null, " ", null);
    SamlUser samlUser5 = tempEntity.newSamlUser("username5" + uuid, null, null, "email5", null);
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser3.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser4.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser5.getId(), samlGroup2.getId());

    sendRoleNotifications(samlGroup1.getName(), samlGroup2.getName(), "group3");

    assertEmailAddresses("email1", "email5");
  }

  @Test
  public void testSendNotifications_Role_OAuth2() {
    enableSsoWithOAuth2();

    String uuid = TemporaryEntity.uuid();
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("username1" + uuid, null, null, "email1", null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("username2" + uuid, null, null, null, null);
    OAuth2User oAuth2User3 = tempEntity.newOAuth2User("username3" + uuid, null, null, "", null);
    OAuth2User oAuth2User4 = tempEntity.newOAuth2User("username4" + uuid, null, null, " ", null);
    OAuth2User oAuth2User5 = tempEntity.newOAuth2User("username5" + uuid, null, null, "email5", null);
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User3.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User4.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User5.getId(), oAuth2Group2.getId());

    sendRoleNotifications(oAuth2Group1.getName(), oAuth2Group2.getName(), "group3");

    assertEmailAddresses("email1", "email5");
  }

  private void sendRoleNotifications(String... groupNames) {
    Application app = tempEntity.newApplicationWithParent("test");
    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    for (String groupName : groupNames) {
      tempEntity.newMembershipMapping(app.getId(), role.getId(), groupName, MemberType.GROUP);
    }
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "scan-id";
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(app);
    policy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), eval.getStageTypeId()));
    policyDAO.update(policy);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(tempEntity.newPolicyViolation(eval, policy));
    List<PolicyNotification> policyNotifications = policyNotificationUtil
        .createPolicyNotifications(app, policyViolations, eval.getStageTypeId(), eval.isForMonitoring());
    policyAlertEmailer.sendNotifications(app, scanId, stage, policyNotifications, 0, eval.isForMonitoring());
  }

  private void assertEmailAddresses(String... expectedEmailAddresses) {
    verify(mailer, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis()).times(expectedEmailAddresses.length))
        .sendHtml(toAddressesArgumentCaptor.capture(), anyString(), anyString());

    assertThat(toAddressesArgumentCaptor.getAllValues()).containsExactlyInAnyOrder(expectedEmailAddresses);
  }

  private void assertEmailSubject(String expectedEmailSubject) {
    verify(mailer, timeout(NOTIFICATION_WAIT_TIMEOUT.toMillis()).atLeastOnce())
        .sendHtml(any(), emailSubjectArgumentCaptor.capture(), anyString());

    assertThat(emailSubjectArgumentCaptor.getValue()).isEqualTo(expectedEmailSubject);
  }

  private void startLdapServer1() throws Exception {
    startLdapServer1(false);
  }

  private void startLdapServer1(boolean useStaticGroups) throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server 1");

    testLdapServer1.start();
    testLdapServer1.loadData("/PolicyAlertEmailerTest/ldap_users1.ldif");

    ldapService.upsertLdapConnection(createLdapConnection(ldapServer1, testLdapServer1));

    ldapUserMappingDAO.insert(createUserMapping(ldapServer1, useStaticGroups));
  }

  private void startLdapServer2() throws Exception {
    startLdapServer2(false);
  }

  private void startLdapServer2(boolean useStaticGroups) throws Exception {
    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server 2");

    testLdapServer2.start();
    testLdapServer2.loadData("/PolicyAlertEmailerTest/ldap_users2.ldif");

    ldapService.upsertLdapConnection(createLdapConnection(ldapServer2, testLdapServer2));

    ldapUserMappingDAO.insert(createUserMapping(ldapServer2, useStaticGroups));
  }

  private LdapConnection createLdapConnection(LdapServer ldapServer, TestLdapServer testLdapServer) {
    LdapConnection ldapConnection = ldapService.getLdapConnection(ldapServer.getId());
    ldapConnection.setServerId(ldapServer.getId());
    ldapConnection.setProtocol(LdapProtocol.LDAP);
    ldapConnection.setSearchBase("dc=company,dc=com");
    if (testLdapServer != null) {
      ldapConnection.setHostname(testLdapServer.getHostname());
      ldapConnection.setPort(testLdapServer.getPort());
    }
    return ldapConnection;
  }

  private LdapUserMapping createUserMapping(LdapServer ldapServer, boolean useStaticGroups) {
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    ldapUserMapping.setUserBaseDN("ou=users");
    ldapUserMapping.setUserObjectClass("person");
    ldapUserMapping.setUserIDAttribute("uid");
    ldapUserMapping.setUserRealNameAttribute("cn");
    ldapUserMapping.setUserEmailAttribute("mail");
    ldapUserMapping.setUserSubtree(true);
    ldapUserMapping.setGroupBaseDN("ou=groups");
    ldapUserMapping.setGroupIDAttribute("cn");
    ldapUserMapping.setGroupSubtree(true);
    if (useStaticGroups) {
      ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);
      ldapUserMapping.setGroupObjectClass("groupOfNames");
      ldapUserMapping.setGroupMemberAttribute("member");
      ldapUserMapping.setGroupMemberFormat("${dn}");
    }
    else {
      ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
      ldapUserMapping.setUserMemberOfGroupAttribute("departmentNumber");
    }
    return ldapUserMapping;
  }

  @Test
  public void testNotificationEmailBody() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-public-id");
    String scanId = "some scan id";
    Policy policy = tempEntity.newPolicy(app.getId(), "Notifying Policy");

    List<PolicyFact> policyFacts = new ArrayList<>();
    ComponentIdentifier componentIdentifierMaven = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1",
        "e1");
    String hashMaven = "hashmaven";
    policyFacts.add(newPolicyFact(policy, componentIdentifierMaven, hashMaven));
    ComponentIdentifier componentIdentifierAname = ComponentIdentifier.createAnameCoordinates("n1", "q&1", "v1");
    String hashAname = "hashAname";
    policyFacts.add(newPolicyFact(policy, componentIdentifierAname, hashAname));
    String hashUnknown = "hashUnknown12&3";
    policyFacts.add(newPolicyFact(policy, null, hashUnknown));

    ContactDTO appContact =
        ApplicationContactLoader.getInstance(userDirectory).getContact(app.getContactInternalName());

    Map<String, Object> baseModel =
        policyAlertEmailer.createPolicyMailModel(mailer.getCdnUrl(), app, StageTypes.STAGE_RELEASE, policyFacts);
    Map<String, Object> model =
        policyAlertEmailer.createPolicyMailModel(app, appContact, scanId, 7, baseModel);

    String emailBody = policyAlertEmailer.createPolicyMailBody(model);
    assertThat(emailBody)
        .contains(mailer.getCdnUrl(), getBaseUrl() + UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), scanId))
        .contains(app.getPublicId()) //
        .contains(StageTypes.STAGE_RELEASE.getName()) //
        .contains(ComponentDisplayNameUtil.fromIdentifier(componentIdentifierMaven).toString(),
            ComponentDisplayNameUtil.fromIdentifier(componentIdentifierAname).toString().replace("&", "&amp;"))
        .doesNotContain(hashMaven, hashAname)
        .contains(hashUnknown.replace("&", "&amp;")) //
        .contains(policy.getName()) //
        .contains("Failed &amp; Constraint Name 1", "Failed Constraint Name 2")
        .contains("Failed Condition &lt;Reason&gt; 1", "Failed Condition Reason 2") //
        .contains("7 Legacy Violations");
  }

  @Test
  public void testNotificationEmailBody_ContainerImageEvaluation() throws Exception {
    // Create an organization with a related repository ID to simulate container image evaluation
    Organization org = tempEntity.newOrganization();
    org.setRelatedRepositoryId("test-repository-id");
    organizationDAO.update(org);

    Application app = tempEntity.newApplication(org.getId());
    String scanId = "some-scan-id";
    Policy policy = tempEntity.newPolicy(app.getId(), "Container Policy");

    List<PolicyFact> policyFacts = new ArrayList<>();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String hash = "hash123";
    policyFacts.add(newPolicyFact(policy, componentIdentifier, hash));

    ContactDTO appContact =
        ApplicationContactLoader.getInstance(userDirectory).getContact(app.getContactInternalName());

    Map<String, Object> baseModel =
        policyAlertEmailer.createPolicyMailModel(mailer.getCdnUrl(), app, StageTypes.PROXY, policyFacts);
    Map<String, Object> model =
        policyAlertEmailer.createPolicyMailModel(app, appContact, scanId, 0, baseModel);

    String emailBody = policyAlertEmailer.createPolicyMailBody(model);

    // Verify that the email body contains the container image evaluation report URL
    assertThat(emailBody)
        .contains(getBaseUrl() + UserInterfaceLinksHelper.getFirewallContainerImageEvaluationReportUrl(
            app.getPublicId(), scanId))
        .doesNotContain(getBaseUrl() + UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), scanId));
  }

  @Test
  public void testNotificationEmailBodyForSM() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-public-id");
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(app.getId(), ThirdPartySbomMetadataStatus.ACTIVE, "bom.xml");
    Policy policy = tempEntity.newPolicy(app.getId(), "Notifying Policy");

    List<PolicyFact> policyFacts = new ArrayList<>();
    ComponentIdentifier componentIdentifierMaven = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1",
        "e1");
    String hashMaven = "hashmaven";
    policyFacts.add(newPolicyFact(policy, componentIdentifierMaven, hashMaven));
    ComponentIdentifier componentIdentifierAname = ComponentIdentifier.createAnameCoordinates("n1", "q&1", "v1");
    String hashAname = "hashAname";
    policyFacts.add(newPolicyFact(policy, componentIdentifierAname, hashAname));
    String hashUnknown = "hashUnknown12&3";
    policyFacts.add(newPolicyFact(policy, null, hashUnknown));

    ContactDTO appContact =
        ApplicationContactLoader.getInstance(userDirectory).getContact(app.getContactInternalName());

    Map<String, Object> baseModel =
        policyAlertEmailer.createPolicyMailModel(mailer.getCdnUrl(), app, StageTypes.COMPLIANCE, policyFacts);
    Map<String, Object> model =
        policyAlertEmailer.createPolicyMailModelForSbomManager(app, appContact, baseModel);

    String emailBody = policyAlertEmailer.createPolicyMailBodyForSbomManager(model);
    assertThat(emailBody)
        .contains(
            mailer.getCdnUrl() + "clm/sbom/logo-sonatype-sbom-manager.svg",
            getBaseUrl() +
                UserInterfaceLinksHelper.getSBOMBillOfMaterialPath(app.getPublicId(), sbomMetadata.getSbomVersion()))
        .contains(app.getPublicId()) //
        .contains(StageTypes.COMPLIANCE.getName()) //
        .contains(ComponentDisplayNameUtil.fromIdentifier(componentIdentifierMaven).toString(),
            ComponentDisplayNameUtil.fromIdentifier(componentIdentifierAname).toString().replace("&", "&amp;"))
        .doesNotContain(hashMaven, hashAname)
        .contains(hashUnknown.replace("&", "&amp;")) //
        .contains(policy.getName()) //
        .contains("Failed &amp; Constraint Name 1", "Failed Constraint Name 2")
        .contains("Failed Condition &lt;Reason&gt; 1", "Failed Condition Reason 2");
  }

  @Test
  public void testCreatePolicyMailModel_BaseUrlNotConfigured() {
    setBaseUrl(null);

    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    List<PolicyFact> policyFacts = new ArrayList<>();
    policyFacts
        .add(newPolicyFact(policy, ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), "hash"));

    assertThatThrownBy(
        () -> policyAlertEmailer.createPolicyMailModel(app, null /* appContact */, "scanId",
            0, Collections.emptyMap())).isInstanceOf(IllegalStateException.class)
                .hasMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  private PolicyFact newPolicyFact(Policy policy, ComponentIdentifier componentIdentifier, String hash) {
    ConditionFact conditionFact1 =
        new ConditionFact("condition-type-id", 0, "Failed Condition Summary 1", "Failed Condition <Reason> 1");
    ConditionFact conditionFact2 =
        new ConditionFact("condition-type-id", 0, "Failed Condition Summary 2", "Failed Condition Reason 2");
    ConstraintFact constraintFact1 = new ConstraintFact("constraint-id-1", "Failed & Constraint Name 1", "or");
    constraintFact1.addConditionFact(conditionFact1);
    constraintFact1.addConditionFact(conditionFact2);
    ConstraintFact constraintFact2 = new ConstraintFact("constraint-id-2", "Failed Constraint Name 2", "or");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, hash);
    if (componentIdentifier != null) {
      componentFact.setDisplayName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier));
    }
    componentFact.addConstraintFact(constraintFact1);
    componentFact.addConstraintFact(constraintFact2);
    PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    policyFact.addComponentFact(componentFact);

    return policyFact;
  }

  private static class SameId
      implements ArgumentMatcher<LdapServer>
  {
    private final String ldapServerId;

    SameId(LdapServer ldapServer) {
      ldapServerId = ldapServer.getId();
    }

    @Override
    public boolean matches(LdapServer other) {
      if (other == null) {
        return false;
      }
      return ldapServerId.equals(other.getId());
    }
  }
}
