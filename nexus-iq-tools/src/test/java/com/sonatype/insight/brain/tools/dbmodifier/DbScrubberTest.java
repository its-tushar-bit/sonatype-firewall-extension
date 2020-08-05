/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemNoticeDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class DbScrubberTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private static final String IN_MEMORY_DB_CONNECTION_STRING =
      "jdbc:h2:mem:inMemoryDatabase;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE;SCHEMA="
          + OperationalDataStoreProvider.ID;

  @Test
  public void testScrubDB_Table_dashboard_filter() throws Exception {
    DashboardFilter dashboardFilter1 =
        tempEntity.newDashboardFilter("TestUser", "testRealmId", "testFilterName", "testFilter");
    DashboardFilter dashboardFilter2 =
        tempEntity.newDashboardFilter("TestUser", "testRealmId", "", true, "testFilterName", "testFilter1");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(dashboardFilter1.getId(), dashboardFilter2.getId(), "TestUser", "testuser",
        "testFilterName");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(dashboardFilter1.getId(), dashboardFilter2.getId());
    assertThat(scrubbedSqlContent).doesNotContain("TestUser", "testuser", "testFilterName");
  }

  @Test
  public void testScrubDB_Table_mail_configuration() throws Exception {
    MailConfiguration mailConfiguration = tempEntity.newMailConfiguration("testUsername", "testPassword".toCharArray());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(mailConfiguration.getId(), "testUsername");
    assertThat(getScrubbedSqlContent()).doesNotContain(mailConfiguration.getId(), "testUsername");
  }

  @Test
  public void testScrubDB_Table_proxy_server_configuration() throws Exception {
    tempEntity.setProxyServerConfiguration("testHostname", 1234);

    scrubDb();

    assertThat(getSqlDumpContent()).contains("testHostname");
    assertThat(getScrubbedSqlContent()).doesNotContain("testHostname");
  }

  @Test
  public void testScrubDB_Table_repository_policy_violation() throws Exception {
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(
        tempEntity.newRepository().getId(), 5, "testPath1/testPath2/testFilename", true, "testPolicyId",
        "testPolicyName", ComponentIdentifier.createNpmCoordinates("TestPackageId", "TestVersion"));
    repositoryPolicyViolation.setPolicyWaiverComment("testPolicyWaiverComment");
    new RepositoryPolicyViolationDAO().update(repositoryPolicyViolation);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(repositoryPolicyViolation.getId(), "testPath1/testPath2/testFilename",
        "testPolicyName", "testPolicyWaiverComment", "TestPackageId", "TestVersion", ComponentIdentifier.NPM_PACKAGE_ID,
        ComponentIdentifier.VERSION);
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(repositoryPolicyViolation.getId(), ComponentIdentifier.NPM_PACKAGE_ID,
        ComponentIdentifier.VERSION);
    assertThat(scrubbedSqlContent).doesNotContain("testPath1", "testPath2", "testFilename",
        "testPolicyWaiverComment", "TestPackageId", "TestVersion");
  }

  @Test
  public void testScrubDB_Table_saml_configuration() throws Exception {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("testIdentityProviderMetadataXml", "testEntityId");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(samlConfiguration.getId(), "testIdentityProviderMetadataXml",
        "testEntityId");
    assertThat(getScrubbedSqlContent()).doesNotContain(samlConfiguration.getId(), "testIdentityProviderMetadataXml",
        "testEntityId");
  }

  @Test
  public void testScrubDB_Table_membership_mapping() throws Exception {
    User user = tempEntity.newUser("testUsername");
    MembershipMapping membershipMapping = tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID,
        Role.OWNER_ROLE_ID, user.getUsername(), MemberType.USER);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(membershipMapping.getId(), "testUsername");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(membershipMapping.getId());
    assertThat(scrubbedSqlContent).doesNotContain("testUsername");
  }

  @Test
  public void testScrubDB_Table_user() throws Exception {
    User user = tempEntity.newUser("testUsername", "testFirstName", "testLastName", "testemail@example.com");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(user.getId(), "testUsername", "testusername", "testFirstName",
        "testLastName", "testemail@example.com");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(user.getId());
    assertThat(scrubbedSqlContent).doesNotContain("testUsername", "testusername", "testFirstName", "testLastName",
        "testemail@example.com");
  }

  @Test
  public void testScrubDB_Table_user_token() throws Exception {
    UserToken userToken = tempEntity.newUserToken("testUsername", "testRealmId");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(userToken.getId(), "testUsername", "testRealmId");
    assertThat(getScrubbedSqlContent()).doesNotContain(userToken.getId(), "testUsername", "testRealmId");
  }

  @Test
  public void testScrubDB_Table_system_notice() throws Exception {
    SystemNoticeDAO systemNoticeDAO = new SystemNoticeDAO();
    SystemNotice systemNotice = systemNoticeDAO.get();
    systemNotice.setMessage("testSystemNotice");
    systemNoticeDAO.update(systemNotice);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(systemNotice.getId(), "testSystemNotice");
    assertThat(getScrubbedSqlContent()).doesNotContain(systemNotice.getId(), "testSystemNotice");
  }

  @Test
  public void testScrubDB_Table_ldap_server() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("testLdapServer");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(ldapServer.getId(), "testLdapServer");
    assertThat(getScrubbedSqlContent()).doesNotContain(ldapServer.getId(), "testLdapServer");
  }

  @Test
  public void testScrubDB_Table_ldap_connection() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("testLdapServer");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(ldapConnection.getId(), ldapConnection.getHostname());
    assertThat(getScrubbedSqlContent()).doesNotContain(ldapConnection.getId(), ldapConnection.getHostname());
  }

  @Test
  public void testScrubDB_Table_ldap_usermapping() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("testLdapServer");
    LdapUserMapping ldapUserMapping = tempEntity.newLdapUserMapping(ldapServer.getId());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(ldapUserMapping.getId(), ldapUserMapping.getUserBaseDN());
    assertThat(getScrubbedSqlContent()).doesNotContain(ldapUserMapping.getId(), ldapUserMapping.getUserBaseDN());
  }

  @Test
  public void testScrubDB_Table_user_viewed_product_notification() throws Exception {
    UserViewedProductNotification userViewedProductNotification =
        tempEntity.newUserViewedProductNotification("TestUser", "testRealmId", "testNotificationId");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(userViewedProductNotification.getId(), "TestUser", "testuser");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(userViewedProductNotification.getId());
    assertThat(scrubbedSqlContent).doesNotContain("TestUser", "testuser");
  }

  @Test
  public void testScrubDB_Table_webhook() throws Exception {
    Webhook webhook = tempEntity.newWebhook("http://example.com", Collections.emptySet());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(webhook.getId(), "http://example.com");
    assertThat(getScrubbedSqlContent()).doesNotContain(webhook.getId(), "http://example.com");
  }

  @Test
  public void testScrubDB_Table_source_control() throws Exception {
    String repoUrl = "http://bitbucket.org/scm/org/repo";
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newOrganization();
    SourceControl rootSourceControl = tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, "testUser",
        "testToken", SourceControlProvider.BITBUCKET, true, false, "master", null);
    SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), repoUrl, null, "TOKEN", null, null, true, null, null);

    scrubDb();

    assertThat(getSqlDumpContent())
        .contains(rootSourceControl.getId(), appSourceControl.getId(), repoUrl, "testUser", "TOKEN");
    assertThat(getScrubbedSqlContent())
        .doesNotContain(rootSourceControl.getId(), appSourceControl.getId(), repoUrl, "testUser", "TOKEN");
  }

  @Test
  public void testScrubDB_Table_source_control_pull_request_comment() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation sourcePolicyEvaluation = tempEntity.newPolicyEvaluation(
        app.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation = tempEntity.newPolicyEvaluation(
        app.getId(), BuildStageType.ID, "targetScan", "targetCommit");
    SourceControlPullRequestComment sourceControlPullRequestComment = tempEntity
        .newSourceControlPullRequestComment(app.getId(), 1, 2, 3, "contentHash", sourcePolicyEvaluation.getId(),
            targetPolicyEvaluation.getId());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(sourceControlPullRequestComment.getId());
    assertThat(getScrubbedSqlContent()).doesNotContain(sourceControlPullRequestComment.getId());
  }

  @Test
  public void testScrubDB_Table_source_control_default_branch_commit_history() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    SourceControlDefaultBranchCommitHistory sourceControlDefaultBranchCommitHistory =
        tempEntity.newSourceControlDefaultBranchCommitHistory(app.getId(), "commitHash", new Date(), null);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(sourceControlDefaultBranchCommitHistory.getId());
    assertThat(getScrubbedSqlContent()).doesNotContain(sourceControlDefaultBranchCommitHistory.getId());
  }

  @Test
  public void testScrubDB_Table_source_control_event() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "targetScan", "targetCommit");
    SourceControlEvent sourceControlEvent =
        tempEntity.newSourceControlEvent(app, sourcePolicyEvaluation, targetPolicyEvaluation);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(sourceControlEvent.getId());
    assertThat(getScrubbedSqlContent()).doesNotContain(sourceControlEvent.getId());
  }

  private String getSqlDumpContent() throws IOException {
    for (File file : tempDir.getRoot().listFiles()) {
      if (file.getName().startsWith(DbScrubber.SQL_FILENAME_PREFIX)
          && !file.getName().endsWith(DbScrubber.SCRUBBED_SQL_FILENAME_SUFFIX)) {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
      }
    }
    fail("Did not find SQL dump file");
    return null;
  }

  private String getScrubbedSqlContent() throws IOException {
    for (File file : tempDir.getRoot().listFiles()) {
      if (file.getName().startsWith(DbScrubber.SQL_FILENAME_PREFIX)
          && file.getName().endsWith(DbScrubber.SCRUBBED_SQL_FILENAME_SUFFIX)) {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
      }
    }
    fail("Did not find scrubbed SQL file");
    return null;
  }

  @Test
  public void testScrubDB_Table_organization() throws Exception {
    Organization org = tempEntity.newOrganization("Test org");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(org.getId(), "Test org", "testorg");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(org.getId());
    assertThat(scrubbedSqlContent).doesNotContain("Test org", "testorg");
  }

  @Test
  public void testScrubDB_Table_application() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("Test app", "TestPublicID", org.getId(), "TestContactName");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(app.getId(), "TestPublicID", "testpublicid", "Test app", "testapp",
        "TestContactName");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(app.getId());
    assertThat(scrubbedSqlContent).doesNotContain("TestPublicID", "testpublicid", "Test app", "testapp",
        "TestContactName");
  }

  @Test
  public void testScrubDB_Table_label() throws Exception {
    Label label =
        tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "Test label", "Test description", Color.yellow);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(label.getId(), "Test label", "test label", "Test description");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(label.getId());
    assertThat(scrubbedSqlContent).doesNotContain("Test label", "test label", "Test description");
  }

  @Test
  public void testScrubDB_Table_license_threat_group() throws Exception {
    LicenseThreatGroup licenseThreatGroup =
        tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID, "Test LTG", 5);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(licenseThreatGroup.getId(), "Test LTG", "testltg");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(licenseThreatGroup.getId());
    assertThat(scrubbedSqlContent).doesNotContain("Test LTG", "testltg");
  }

  @Test
  @Ignore
  // Policies need extra processing to be scrubbed properly
  public void testScrubDB_Table_policy() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Test policy");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(policy.getId(), "Test policy", "testpolicy");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(policy.getId());
    assertThat(scrubbedSqlContent).doesNotContain("Test policy", "testpolicy");
  }

  @Test
  public void testScrubDB_Table_role() throws Exception {
    Role role = tempEntity.newRole("Test role", "Test description", false);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(role.getId(), "Test role", "testrole", "Test description");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(role.getId());
    assertThat(scrubbedSqlContent).doesNotContain("Test role", "testrole", "Test description");
  }

  @Test
  public void testScrubDB_Table_tag() throws Exception {
    Tag appCategory =
        tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Test app category", "Test description", Color.yellow);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(appCategory.getId(), "Test app category", "testappcategory",
        "Test description");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(appCategory.getId());
    assertThat(scrubbedSqlContent).doesNotContain("Test app category", "testappcategory", "Test description");
  }

  @Test
  public void testScrubDB_Table_repository_manager() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager("TestInstanceId"); 

    scrubDb();

    assertThat(getSqlDumpContent()).contains(repoManager.getId(), "TestInstanceId");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(repoManager.getId());
    assertThat(scrubbedSqlContent).doesNotContain("TestInstanceId");
  }

  @Test
  public void testScrubDB_Table_repository() throws Exception {
    Repository repo = tempEntity.newRepository("TestPublicId");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(repo.getId(), "TestPublicId");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(repo.getId());
    assertThat(scrubbedSqlContent).doesNotContain("TestPublicId");
  }

  @Test
  public void testScrubDB_Table_hash_component_identifier() throws Exception {
    HashComponentIdentifier claimedComponent = tempEntity.newClaimedComponent("TestHash",
        ComponentIdentifier.createNpmCoordinates("TestPackageId", "TestVersion"));
    claimedComponent.setComment("Test comment");
    new HashComponentIdentifierDAO().update(claimedComponent);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(claimedComponent.getId(), "TestPackageId", "TestVersion", "Test comment");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(claimedComponent.getId(), ComponentIdentifier.NPM_PACKAGE_ID,
        ComponentIdentifier.VERSION);
    assertThat(scrubbedSqlContent).doesNotContain("TestPackageId", "TestVersion", "Test comment");
  }

  @Test
  public void testScrubDB_Table_policy_waiver() throws Exception {
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("TestHash", policy.getId(), Organization.ROOT_ORGANIZATION_ID, "Test comment");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(policyWaiver.getId(), "Test comment");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(policyWaiver.getId());
    assertThat(scrubbedSqlContent).doesNotContain("Test comment");
  }

  @Test
  public void testScrubDB_Table_license_override() throws Exception {
    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID,
        ComponentIdentifier.createNpmCoordinates("TestPackageId", "TestVersion"), LicenseOverrideStatus.OVERRIDDEN,
        "Apache-2.0", "Test comment");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(licenseOverride.getId(), "TestPackageId", "TestVersion",
        ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION, "Test comment");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(licenseOverride.getId(), ComponentIdentifier.NPM_PACKAGE_ID,
        ComponentIdentifier.VERSION);
    assertThat(scrubbedSqlContent).doesNotContain("TestPackageId", "TestVersion", "Test comment");
  }

  @Test
  public void testScrubDB_Table_sv_override() throws Exception {
    SecurityVulnerabilityOverride svOverride =
        tempEntity.newSecurityVulnerabilityOverride(Organization.ROOT_ORGANIZATION_ID, "TestHash", "CVE",
            "CVE-1234-5678", SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE, "Test comment");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(svOverride.getId(), "Test comment");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(svOverride.getId());
    assertThat(scrubbedSqlContent).doesNotContain("Test comment");
  }

  @Test
  public void testScrubDB_Table_application_component_KnownComponent() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApplicationComponent knownComponent = tempEntity.newApplicationComponent(app.getId(), StageTypes.BUILD.getId(),
        "hashKnownComponent", ComponentIdentifier.createNpmCoordinates("KnownPackageId", "KnownVersion"),
        "knownPath/fooPath/barFile", MatchState.EXACT, false /* proprietary */, new Date());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(knownComponent.getId(), "hashKnownComponent", "KnownPackageId",
        "KnownVersion", ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION, "knownPath/fooPath/barFile");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(knownComponent.getId(), "hashKnownComponent", "KnownPackageId",
        "KnownVersion", ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION);
    assertThat(scrubbedSqlContent).doesNotContain("knownPath", "fooPath", "barFile");
  }

  @Test
  public void testScrubDB_Table_application_component_UnknownComponent() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApplicationComponent unknownComponent = tempEntity.newApplicationComponent(app.getId(), StageTypes.BUILD.getId(),
        "hashUnknownComponent", null /* componentIdentifier */, "unknownPath/fooPath/barFile", MatchState.UNKNOWN,
        false /* proprietary */, new Date());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(unknownComponent.getId(), "hashUnknownComponent",
        "unknownPath/fooPath/barFile");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(unknownComponent.getId(), "hashUnknownComponent");
    assertThat(scrubbedSqlContent).doesNotContain("unknownPath", "fooPath", "barFile");
  }

  @Test
  public void testScrubDB_Table_application_component_NotIdentifiedBySonatypeComponent() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApplicationComponent claimedComponent = tempEntity.newApplicationComponent(app.getId(), StageTypes.BUILD.getId(),
        "hashClaimedComponent", ComponentIdentifier.createNpmCoordinates("ClaimedPackageId", "ClaimedVersion"),
        "claimedPath/fooPath/barFile", MatchState.EXACT, IdentificationSource.MANUAL, false /* proprietary */,
        new Date());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(claimedComponent.getId(), "hashClaimedComponent", "ClaimedPackageId",
        "ClaimedVersion", ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION,
        IdentificationSource.MANUAL.getId(), "claimedPath/fooPath/barFile");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(claimedComponent.getId(), "hashClaimedComponent",
        ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION, IdentificationSource.MANUAL.getId());
    assertThat(scrubbedSqlContent).doesNotContain("ClaimedPackageId", "ClaimedVersion", "claimedPath", "fooPath",
        "barFile");
  }

  @Test
  public void testScrubDB_Table_application_component_ProprietaryComponent() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApplicationComponent proprietaryComponent =
        tempEntity.newApplicationComponent(app.getId(), StageTypes.BUILD.getId(), "hashPropComponent",
            ComponentIdentifier.createNpmCoordinates("ProprietaryPackageId", "ProprietaryVersion"),
            "proprietaryPath/fooPath/barFile", MatchState.EXACT, true /* proprietary */, new Date());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(proprietaryComponent.getId(), "hashPropComponent",
        "ProprietaryPackageId", "ProprietaryVersion", ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION,
        "proprietaryPath/fooPath/barFile");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(proprietaryComponent.getId(), "hashPropComponent",
        ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION);
    assertThat(scrubbedSqlContent).doesNotContain("ProprietaryPackageId", "ProprietaryVersion", "proprietaryPath",
        "fooPath", "barFile");
  }

  @Test
  public void testScrubDB_Table_repository_component_KnownComponent() throws Exception {
    Repository repo = tempEntity.newRepository();
    RepositoryComponent knownComponent =
        tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "knownPath/fooPath/barFile",
            "hashKnownComponent",
            ComponentIdentifier.createNpmCoordinates("KnownPackageId", "KnownVersion"), false);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(knownComponent.getId(), "hashKnownComponent", "KnownPackageId",
        "KnownVersion", ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION, "knownPath/fooPath/barFile");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(knownComponent.getId(), "hashKnownComponent", "KnownPackageId",
        "KnownVersion", ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION);
    assertThat(scrubbedSqlContent).doesNotContain("knownPath", "fooPath", "barFile");
  }

  @Test
  public void testScrubDB_Table_repository_component_UnknownComponent() throws Exception {
    Repository repo = tempEntity.newRepository();
    RepositoryComponent unknownComponent = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        "unknownPath/fooPath/barFile", "hashUnknownComponent", null, false);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(unknownComponent.getId(), "hashUnknownComponent",
        "unknownPath/fooPath/barFile");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(unknownComponent.getId(), "hashUnknownComponent");
    assertThat(scrubbedSqlContent).doesNotContain("unknownPath", "fooPath", "barFile");
  }

  @Test
  public void testScrubDB_Table_repository_component_NotIdentifiedBySonatypeComponent() throws Exception {
    Repository repo = tempEntity.newRepository();
    RepositoryComponent claimedComponent =
        tempEntity.newRepositoryComponent(repo.getId(), "claimedPath/fooPath/barFile", new Date(),
            "hashClaimedComponent",
            ComponentIdentifier.createNpmCoordinates("ClaimedPackageId", "ClaimedVersion"), MatchState.EXACT.getId(),
            IdentificationSource.MANUAL.getId(), new Date());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(claimedComponent.getId(), "hashClaimedComponent", "ClaimedPackageId",
        "ClaimedVersion", ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION,
        IdentificationSource.MANUAL.getId(), "claimedPath/fooPath/barFile");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(claimedComponent.getId(), "hashClaimedComponent",
        ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION, IdentificationSource.MANUAL.getId());
    assertThat(scrubbedSqlContent).doesNotContain("ClaimedPackageId", "ClaimedVersion", "claimedPath", "fooPath",
        "barFile");
  }

  @Test
  public void testScrubDB_Table_policy_evaluation() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId", new Date(), "testCommitHash");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(policyEvaluation.getId(), "testCommitHash");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(policyEvaluation.getId());
    assertThat(scrubbedSqlContent).doesNotContain("testCommitHash");
  }

  @Test
  public void testScrubDB_Table_policy_violation() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId");
    Policy policy = tempEntity.newPolicy();
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy,
        ComponentIdentifier.createNpmCoordinates("TestPackageId", "TestVersion"), "TestHash", "TestReason",
        "TestPath1/TestPath2/TestFileName");
    policyViolation.setPolicyWaiverComment("testPolicyWaiverComment");
    new PolicyViolationDAO().update(policyViolation);
    new PolicyDAO().delete(policy);

    scrubDb();

    assertThat(getSqlDumpContent()).contains(policyViolation.getId(), policy.getName(), "TestHash", "TestPackageId",
        "TestVersion", ComponentIdentifier.NPM_PACKAGE_ID, ComponentIdentifier.VERSION, policy.getName(),
        "TestPath1/TestPath2/TestFileName", "testPolicyWaiverComment");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(policyViolation.getId(), "TestHash", ComponentIdentifier.NPM_PACKAGE_ID,
        ComponentIdentifier.VERSION);
    assertThat(scrubbedSqlContent).doesNotContain("TestPackageId", "TestVersion", policy.getName(), "TestPath1",
        "TestPath2", "TestFilename", "testPolicyWaiverComment");
  }

  private void scrubDb() {
    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());
  }
}
