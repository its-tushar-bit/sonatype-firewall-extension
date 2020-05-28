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

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.security.UserToken;

import org.apache.commons.io.FileUtils;
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

    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());

    assertThat(getSqlDumpContent()).contains("TestUser", "testuser", "testFilterName");
    assertThat(getScrubbedSqlContent()).doesNotContain("TestUser", "testuser", "testFilterName");
  }

  @Test
  public void testScrubDB_Table_mail_configuration() throws Exception {
    MailConfiguration mailConfiguration = tempEntity.newMailConfiguration("testUsername", "testPassword".toCharArray());

    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());

    assertThat(getSqlDumpContent()).contains(mailConfiguration.getId(), "testUsername");
    assertThat(getScrubbedSqlContent()).doesNotContain(mailConfiguration.getId(), "testUsername");
  }

  @Test
  public void testScrubDB_Table_proxy_server_configuration() throws Exception {
    tempEntity.setProxyServerConfiguration("testHostname", 1234);

    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());

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

    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());

    assertThat(getSqlDumpContent()).contains("testPathname", "testPolicyName", "testPolicyWaiverComment");
    assertThat(getScrubbedSqlContent()).doesNotContain("testPathname", "testPolicyName", "testPolicyWaiverComment");
  }

  @Test
  public void testScrubDB_Table_saml_configuration() throws Exception {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("testIdentityProviderMetadataXml", "testEntityId");

    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());

    assertThat(getSqlDumpContent()).contains(samlConfiguration.getId(), "testIdentityProviderMetadataXml",
        "testEntityId");
    assertThat(getScrubbedSqlContent()).doesNotContain(samlConfiguration.getId(), "testIdentityProviderMetadataXml",
        "testEntityId");
  }

  @Test
  public void testScrubDB_Table_user_token() throws Exception {
    UserToken userToken = tempEntity.newUserToken("testUsername", "testRealmId");

    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());

    assertThat(getSqlDumpContent()).contains(userToken.getId(), "testUsername", "testRealmId");
    assertThat(getScrubbedSqlContent()).doesNotContain(userToken.getId(), "testUsername", "testRealmId");
  }

  @Test
  public void testScrubDB_Table_user_viewed_product_notification() throws Exception {
    tempEntity.newUserViewedProductNotification("TestUser", "testRealmId", "testNotificationId");

    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());

    assertThat(getSqlDumpContent()).contains("TestUser", "testuser");
    assertThat(getScrubbedSqlContent()).doesNotContain("TestUser", "testuser");
  }

  @Test
  public void testScrubDB_Table_webhook() throws Exception {
    Webhook webhook = tempEntity.newWebhook("http://example.com", Collections.emptySet());

    DbScrubber.scrubDb(IN_MEMORY_DB_CONNECTION_STRING, //
        "sa" /* username */, //
        "" /* password */, //
        false /* rebuild */, true /* keepFiles */, tempDir.getRoot());

    assertThat(getSqlDumpContent()).contains(webhook.getId(), "http://example.com");
    assertThat(getScrubbedSqlContent()).doesNotContain(webhook.getId(), "http://example.com");
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
}
