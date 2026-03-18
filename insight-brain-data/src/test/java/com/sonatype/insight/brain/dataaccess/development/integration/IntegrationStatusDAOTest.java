/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.development.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.prioritization.IntegrationStatusSummary;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationStatusDAOTest
    extends AbstractDbDAOTest
{
  private IntegrationStatusDAO integrationStatusDAO;

  private static final Date CUTOFF_DATE = new Date(TimeUnit.DAYS.toMillis(84));

  @Before
  @Override
  public void setup() {
    super.setup();
    // Create the DAO directly since there's no factory method yet
    integrationStatusDAO = new IntegrationStatusDAO(databaseRule.getOperationalDataStore());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetIntegrationStatusBulk_EmptyCollection() {
    List<IntegrationStatusSummary> result = integrationStatusDAO.getIntegrationStatusBulk(
        Collections.emptyList(), CUTOFF_DATE);

    assertThat(result).isEmpty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetIntegrationStatusBulk_SingleApplication() {
    List<IntegrationStatusSummary> result = integrationStatusDAO.getIntegrationStatusBulk(
        Collections.singletonList(application.getId()), CUTOFF_DATE);

    assertThat(result).hasSize(1);
    IntegrationStatusSummary summary = result.get(0);
    assertThat(summary.applicationId()).isEqualTo(application.getId());
    assertThat(summary.applicationName()).isEqualTo(application.getName());
    assertThat(summary.applicationPublicId()).isEqualTo(application.getPublicId());
    assertThat(summary.organizationId()).isEqualTo(organization.getId());
    // Note: For empty database, these should be defaults
    assertThat(summary.lastEvaluationTimestamp()).isEqualTo(0L);
    assertThat(summary.lastScanId()).isNull();
    assertThat(summary.lastCommitTimestamp()).isEqualTo(0L);
    assertThat(summary.isCiIntegrationEnabled()).isEqualTo(false);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetIntegrationStatusBulk_PostgreSQL_WithPolicyEvaluations() {
    Application app2 = tempEntity.newApplication("TestApp2", "TestApp2-PublicId", organization.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "testScanId");
    evaluation.setForMonitoring(false);
    evaluation.setReevaluation(false);

    List<IntegrationStatusSummary> result = integrationStatusDAO.getIntegrationStatusBulk(
        Arrays.asList(application.getId(), app2.getId()), CUTOFF_DATE);

    assertThat(result).hasSize(2);

    // Find the summary for app2 which should have policy evaluation data
    IntegrationStatusSummary app2Summary = result.stream()
        .filter(s -> s.applicationId().equals(app2.getId()))
        .findFirst()
        .orElse(null);

    assertThat(app2Summary).isNotNull();
    assertThat(app2Summary.applicationId()).isEqualTo(app2.getId());
    assertThat(app2Summary.applicationName()).isEqualTo(app2.getName());
    assertThat(app2Summary.lastEvaluationTimestamp()).isEqualTo(evaluation.getTime().getTime());
    assertThat(app2Summary.lastScanId()).isEqualTo(evaluation.getScanId());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetIntegrationStatusBulk_PostgreSQL_WithSourceControlData() {
    Date commitTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L); // 1 day ago
    SourceControlDefaultBranchCommitHistory commitHistory =
        tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commitHash", commitTime, null);

    List<IntegrationStatusSummary> result = integrationStatusDAO.getIntegrationStatusBulk(
        Collections.singletonList(application.getId()), CUTOFF_DATE);

    assertThat(result).hasSize(1);
    IntegrationStatusSummary summary = result.get(0);
    assertThat(summary.lastCommitTimestamp()).isEqualTo(commitHistory.getCommitTime().getTime());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetIntegrationStatusBulk_PostgreSQL_WithCIIntegration() {
    Application app3 = tempEntity.newApplication("TestApp3", "TestApp3-PublicId", organization.getId());

    // Create a recent policy evaluation (within 84 days) to indicate CI integration
    PolicyEvaluation recentEvaluation = tempEntity.newPolicyEvaluation(app3.getId(), Stage.ID_BUILD, "recentScanId");
    recentEvaluation.setForMonitoring(false);
    recentEvaluation.setReevaluation(false);
    recentEvaluation.setTime(new Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)); // 30 days ago

    List<IntegrationStatusSummary> result = integrationStatusDAO.getIntegrationStatusBulk(
        Collections.singletonList(app3.getId()), CUTOFF_DATE);

    assertThat(result).hasSize(1);
    IntegrationStatusSummary summary = result.get(0);
    assertThat(summary.isCiIntegrationEnabled()).isEqualTo(true);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetIntegrationStatusBulk_PostgreSQL_CompleteIntegrationScenario() {
    Application fullyIntegratedApp = tempEntity.newApplication("FullyIntegratedApp",
        "FullyIntegratedApp-PublicId", organization.getId());

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(fullyIntegratedApp.getId(),
        Stage.ID_BUILD, "fullScanId");
    evaluation.setForMonitoring(false);
    evaluation.setReevaluation(false);

    tempEntity.newSourceControlDefaultBranchCommitHistory(fullyIntegratedApp.getId(),
        "fullCommitHash", new Date(), null);

    List<IntegrationStatusSummary> result = integrationStatusDAO.getIntegrationStatusBulk(
        Collections.singletonList(fullyIntegratedApp.getId()), CUTOFF_DATE);

    assertThat(result).hasSize(1);
    IntegrationStatusSummary summary = result.get(0);

    // Verify all integration aspects
    assertThat(summary.applicationId()).isEqualTo(fullyIntegratedApp.getId());
    assertThat(summary.applicationName()).isEqualTo(fullyIntegratedApp.getName());
    assertThat(summary.applicationPublicId()).isEqualTo(fullyIntegratedApp.getPublicId());
    assertThat(summary.organizationId()).isEqualTo(organization.getId());
    assertThat(summary.lastEvaluationTimestamp()).isNotEqualTo(0L);
    assertThat(summary.lastScanId()).isEqualTo(evaluation.getScanId());
    assertThat(summary.lastCommitTimestamp()).isNotEqualTo(0L);
    assertThat(summary.isCiIntegrationEnabled()).isEqualTo(true);
    assertThat(summary.hasPrioritiesReport()).isEqualTo(true);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetIntegrationStatusBulk_PostgreSQL_LargeDataSet() {
    // Creates 100 applications to test the optimization with realistic scale

    final int appCount = 100;
    final List<Application> apps = new ArrayList<>();
    final List<String> appIds = new ArrayList<>();
    final List<PolicyEvaluation> evaluations = new ArrayList<>();

    // Create 100 applications with varying integration data
    for (int i = 0; i < appCount; i++) {
      Application app = tempEntity.newApplication("BulkApp" + i, "BulkApp" + i + "-PublicId",
          organization.getId());
      apps.add(app);
      appIds.add(app.getId());

      // Add policy evaluations to every 3rd app (simulating real-world sparse data)
      if (i % 3 == 0) {
        PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD,
            "bulkScanId" + i);
        eval.setForMonitoring(false);
        eval.setReevaluation(false);
        evaluations.add(eval);
      }

      // Add source control history to every 4th app
      if (i % 4 == 0) {
        Date commitTime = new Date(System.currentTimeMillis() - (i * 1000 * 60)); // Varying commit times
        tempEntity.newSourceControlDefaultBranchCommitHistory(app.getId(), "commit" + i,
            commitTime, null);
      }
    }

    // Execute bulk query
    long startTime = System.currentTimeMillis();
    List<IntegrationStatusSummary> result = integrationStatusDAO.getIntegrationStatusBulk(appIds, CUTOFF_DATE);
    long executionTime = System.currentTimeMillis() - startTime;

    // Verify results
    assertThat(result).hasSize(appCount);
    assertThat(executionTime)
        .as("Query should complete reasonably fast for bulk data")
        .isLessThan(5000L);

    // Count applications with different integration states
    int appsWithEvaluations = 0;
    int appsWithSourceControl = 0;
    int appsWithCiIntegration = 0;

    for (IntegrationStatusSummary summary : result) {
      assertThat(summary.organizationId()).isEqualTo(organization.getId());
      assertThat(summary.applicationName()).startsWith("BulkApp");

      if (summary.lastEvaluationTimestamp() > 0) {
        appsWithEvaluations++;
      }
      if (summary.lastCommitTimestamp() > 0) {
        appsWithSourceControl++;
      }
      if (summary.isCiIntegrationEnabled()) {
        appsWithCiIntegration++;
      }
    }

    // Verify expected distribution based on our test data setup
    assertThat(appsWithEvaluations)
        .as("Should have evaluations for every 3rd app")
        .isEqualTo((int) Math.ceil(appCount / 3.0));
    assertThat(appsWithSourceControl)
        .as("Should have source control for every 4th app")
        .isEqualTo(appCount / 4);
    assertThat(appsWithCiIntegration)
        .as("CI integration should match apps with recent evaluations")
        .isEqualTo(appsWithEvaluations);

    // Verify results are consistently ordered by application_id
    String previousAppId = null;
    for (IntegrationStatusSummary summary : result) {
      if (previousAppId != null) {
        assertThat(summary.applicationId().compareTo(previousAppId))
            .as("Results should be ordered by application_id")
            .isGreaterThan(0);
      }
      previousAppId = summary.applicationId();
    }
  }
}
