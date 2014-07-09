/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.H2DatabaseMigrator;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PolicyDroolsCodeMigratorTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testMigrate() throws Exception {
    // Create test data
    Application app = tempEntity.newApplicationWithParent("PolicyDroolsCodeMigratorTest App",
        "PolicyDroolsCodeMigratorTestAppId");
    Policy policyApp = tempEntity.newPolicy(app.getId(), "policyApp");
    Policy policyOrg = tempEntity.newPolicy(app.getOrganizationId(), "policyOrg");

    // Change the db to have policy.drools_code nullable and all values null
    new H2DatabaseMigrator().runScript(OperationalDataStoreProvider.getDataSource(),
        "/PolicyDroolsCodeMigratorTest/set_policy_drools_code_to_null_success.sql");
    PolicyDAO policyDAO = new PolicyDAO();
    policyApp = policyDAO.getById(policyApp.getId());
    assertThat(policyApp.getDroolsCode(), is(nullValue()));
    policyOrg = policyDAO.getById(policyOrg.getId());
    assertThat(policyOrg.getDroolsCode(), is(nullValue()));

    // Run the migrator
    InsightWork insightWork = createInsightWork();
    PolicyDroolsCodeMigrator migrator = new PolicyDroolsCodeMigrator(insightWork);
    migrator.migrate();

    // Assert the code was generated for all policies
    policyApp = policyDAO.getById(policyApp.getId());
    assertThat(policyApp.getDroolsCode(), is(notNullValue()));
    policyOrg = policyDAO.getById(policyOrg.getId());
    assertThat(policyOrg.getDroolsCode(), is(notNullValue()));

    // Assert that the policy.drools_code column is not nullable
    try {
      new H2DatabaseMigrator().runScript(OperationalDataStoreProvider.getDataSource(),
          "/PolicyDroolsCodeMigratorTest/set_policy_drools_code_to_null_fail.sql");
      fail("Expected exception");
    }
    catch (Exception e) {
      assertExceptionContains(e, "NULL not allowed for column \"drools_code\"");
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

  private InsightWork createInsightWork() throws IOException {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    InsightWork work = new InsightWork(insightConfig);
    return work;
  }
}
