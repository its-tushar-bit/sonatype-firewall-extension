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
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
<<<<<<< .mine
import com.sonatype.insight.brain.model.label.Label;
=======
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
>>>>>>> .theirs
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
<<<<<<< .mine
import com.sonatype.insight.brain.model.security.Role;
=======
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
>>>>>>> .theirs
import com.sonatype.insight.brain.model.security.UserToken;
<<<<<<< .mine
import com.sonatype.insight.brain.model.tag.Tag;
=======
import com.sonatype.nexus.scm.SourceControlProvider;
>>>>>>> .theirs

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
    tempEntity.newDashboardFilter("TestUser", "testRealmId", "testFilterName", "testFilter");
    tempEntity.newDashboardFilter("TestUser", "testRealmId", "", true, "testFilterName", "testFilter1");

    scrubDb();

    assertThat(getSqlDumpContent()).contains("TestUser", "testuser", "testFilterName");
    assertThat(getScrubbedSqlContent()).doesNotContain("TestUser", "testuser", "testFilterName");
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
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(tempEntity.newRepository().getId(), 5, "testPathname", true,
        "testPolicyId", "testPolicyName", ComponentIdentifier.createNpmCoordinates("packageId", "version"));
    repositoryPolicyViolation.setPolicyWaiverComment("testPolicyWaiverComment");
    new RepositoryPolicyViolationDAO().update(repositoryPolicyViolation);

    scrubDb();

    assertThat(getSqlDumpContent()).contains("testPathname", "testPolicyName", "testPolicyWaiverComment");
    assertThat(getScrubbedSqlContent()).doesNotContain("testPathname", "testPolicyName", "testPolicyWaiverComment");
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
  public void testScrubDB_Table_user_token() throws Exception {
    UserToken userToken = tempEntity.newUserToken("testUsername", "testRealmId");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(userToken.getId(), "testUsername", "testRealmId");
    assertThat(getScrubbedSqlContent()).doesNotContain(userToken.getId(), "testUsername", "testRealmId");
  }

  @Test
  public void testScrubDB_Table_user_viewed_product_notification() throws Exception {
    tempEntity.newUserViewedProductNotification("TestUser", "testRealmId", "testNotificationId");

    scrubDb();

    assertThat(getSqlDumpContent()).contains("TestUser", "testuser");
    assertThat(getScrubbedSqlContent()).doesNotContain("TestUser", "testuser");
  }

  @Test
  public void testScrubDB_Table_webhook() throws Exception {
    Webhook webhook = tempEntity.newWebhook("http://example.com", Collections.emptySet());

    scrubDb();

    assertThat(getSqlDumpContent()).contains(webhook.getId(), "http://example.com");
    assertThat(getScrubbedSqlContent()).doesNotContain(webhook.getId(), "http://example.com");
  }

  @Test
  public void testScrubDB_Table_scm() throws Exception {
    // source_control table
    String repoUrl = "http://bitbucket.org/scm/org/repo";
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newOrganization();
    tempEntity.newSourceControl("ROOT_ORGANIZATION_ID", null, "testUser",
        "testToken", SourceControlProvider.BITBUCKET, true, false, "master", null);
    tempEntity
            .newSourceControl(app.getId(), repoUrl, null, "TOKEN", null, null, true, null,
                null);

    // sc pull request comment
    PolicyEvaluation sourcePolicyEvaluation = tempEntity.newPolicyEvaluation(
        app.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation = tempEntity.newPolicyEvaluation(
        app.getId(), BuildStageType.ID, "targetScan", "targetCommit");
    tempEntity.newSourceControlPullRequestComment(app.getId(), 1, 2, 3, "contentHash", sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId());

    // commit history
    tempEntity.newSourceControlDefaultBranchCommitHistory(app.getId(), "commitHash", new Date(), null);

    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());

    assertThat(getSqlDumpContent()).contains(repoUrl, "testUser", "testToken", "contentHash", "commitHash");
    assertThat(getScrubbedSqlContent()).doesNotContain(repoUrl, "testUser", "testToken", "contentHash", "commitHash");
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
    Application app = tempEntity.newApplicationWithParent("TestPublicID", "Test app");

    scrubDb();

    assertThat(getSqlDumpContent()).contains(app.getId(), "TestPublicID", "testpublicid", "Test app", "testapp");
    String scrubbedSqlContent = getScrubbedSqlContent();
    assertThat(scrubbedSqlContent).contains(app.getId());
    assertThat(scrubbedSqlContent).doesNotContain("TestPublicID", "testpublicid", "Test app", "testapp");
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

  private void scrubDb() {
    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());
  }
}
