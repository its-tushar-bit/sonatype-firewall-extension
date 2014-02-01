/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.H2DatabaseMigrator;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @since 1.9
 */
public class PolicyMigratorTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testMigrate() throws Exception {
    Organization org = tempEntity.newOrganization("PolicyMigratorTest Org");
    Application app = tempEntity.newApplication("PolicyMigratorTest App", "PolicyMigratorTestAppId", org.getId());

    // Create the policy data to be migrated
    InsightWork insightWork = createInsightWork();
    File policyDir = new File(insightWork.getWorkDir(), "policy");
    File orgPolicyDir = new File(policyDir, org.getId());
    orgPolicyDir.mkdirs();
    assertTrue(orgPolicyDir.isDirectory());
    File appPolicyDir = new File(policyDir, app.getId());
    appPolicyDir.mkdirs();
    assertTrue(appPolicyDir.isDirectory());

    URL testPolicyFileUrl = getClass().getResource("/PolicyMigratorTest/policy1.json");
    FileUtils.copyFile(new File(testPolicyFileUrl.getFile()), new File(orgPolicyDir, "policy.json"));
    testPolicyFileUrl = getClass().getResource("/PolicyMigratorTest/policy2.json");
    FileUtils.copyFile(new File(testPolicyFileUrl.getFile()), new File(appPolicyDir, "policy.json"));

    // Remove the db foreign keys that must be created only after the policy data is migrated
    new H2DatabaseMigrator().runScript(OperationalDataStoreProvider.getDataSource(),
        "/PolicyMigratorTest/remove_foreign_keys.sql");

    PolicyMigrator migrator = new PolicyMigrator(insightWork);
    migrator.migrate();

    // Assert the migrated policies
    PolicyDAO policyDAO = new PolicyDAO();
    List<Policy> orgPolicies = policyDAO.getByOwnerId(org.getId());
    assertThat(orgPolicies, hasSize(1));
    assertThat(orgPolicies.get(0).getName(), is("Test Policy 1"));
    assertThat(orgPolicies.get(0).getId(), is("7e7a659ba7cd44e281824f43b38ada0b"));
    List<Policy> appPolicies = policyDAO.getByOwnerId(app.getId());
    assertThat(appPolicies, hasSize(2));
    assertThat(appPolicies.get(0).getName(), is("Test Policy 2"));
    assertThat(appPolicies.get(0).getId(), is("da31c4440914400399e4dccfc13c1897"));
    assertThat(appPolicies.get(1).getName(), is("Test Policy 3"));
    assertThat(appPolicies.get(1).getId(), is("73fecb2f8bec4b38868f5e62c98141ef"));
    
    // Assert that the new db foreign keys were created
    Tag tag = tempEntity.newTag(org.getId(), "PolicyMigratorTest Tag");
    try {
      tempEntity.newPolicyTag("FakePolicyId", tag.getId());
      fail("Foreign keys were not created");
    }
    catch (Exception e) {
      assertExceptionContains(e, "Referential integrity constraint violation: \"policy_tag_policy_fk:");
    }
    try {
      tempEntity.newWaiver("FakePolicyId", org.getId());
      fail("Foreign keys were not created");
    }
    catch (Exception e) {
      assertExceptionContains(e, "Referential integrity constraint violation: \"policy_waiver_policy_fk:");
    }
  }

  private void assertExceptionContains(Exception e, String message) throws Exception {
    Throwable cause = e;
    while (cause != null) {
      if (cause.getMessage() != null && cause.getMessage().contains(message)) {
        return;
      }
      cause = cause.getCause();
    }
    throw e;
  }

  @Test
  public void testMigrate_PolicyDirDoesNotExist() throws Exception {
    InsightWork insightWork = createInsightWork();
    File policyDir = new File(insightWork.getWorkDir(), "policy");
    assertFalse(policyDir.exists());
    File markerFile = new File(insightWork.getWorkDir(), PolicyMigrator.MARKER_FILE_NAME);

    PolicyMigrator migrator = new PolicyMigrator(insightWork);
    migrator.migrate();

    assertTrue(markerFile.exists());
  }

  @Test
  public void testMigrate_PolicyDirEmpty() throws Exception {
    InsightWork insightWork = createInsightWork();
    File policyDir = new File(insightWork.getWorkDir(), "policy");
    policyDir.mkdirs();
    assertTrue(policyDir.exists());
    assertThat(policyDir.listFiles().length, is(0));
    File markerFile = new File(insightWork.getWorkDir(), PolicyMigrator.MARKER_FILE_NAME);
    assertFalse(markerFile.exists());

    PolicyMigrator migrator = new PolicyMigrator(insightWork);
    migrator.migrate();

    assertTrue(markerFile.exists());
  }

  @Test
  public void testMigrate_OwnerDoesNotExist() throws Exception {
    InsightWork insightWork = createInsightWork();
    File policyDir = new File(insightWork.getWorkDir(), "policy");
    File ownerPolicyDir = new File(policyDir, "YetiId");
    ownerPolicyDir.mkdirs();
    assertTrue(ownerPolicyDir.isDirectory());
    File markerFile = new File(insightWork.getWorkDir(), PolicyMigrator.MARKER_FILE_NAME);
    assertFalse(markerFile.exists());

    PolicyMigrator migrator = new PolicyMigrator(insightWork);
    migrator.migrate();

    assertTrue(markerFile.exists());
  }

  private InsightWork createInsightWork() throws IOException {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    InsightWork work = new InsightWork(insightConfig);
    return work;
  }
}
