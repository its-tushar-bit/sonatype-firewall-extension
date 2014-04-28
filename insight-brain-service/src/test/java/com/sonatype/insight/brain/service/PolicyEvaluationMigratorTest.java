/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.NewestPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.trending.TrendingReportCache;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class PolicyEvaluationMigratorTest
{

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private final PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

  private File sonatypeWork;

  private InsightConfig insightConfig;

  private InsightWork insightWork;

  private PolicyEvaluationMigrator policyEvaluationMigrator;

  private Application app1;
  private Application app2;
  private Application appNoEvals;

  @Before
  public void setup() throws IOException {
    sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
    policyEvaluationMigrator = new PolicyEvaluationMigrator(insightWork,
        new TrendingReportCache(insightWork, new FileCleaner()));

    //provide evaluation logs and dummied up reports(zip has no content and the report.cache files are loaded) for test purposes
    FileUtils.copyDirectory(new File("target/test-classes/PolicyEvaluationMigratorTest"), sonatypeWork);
    Organization organization = tempEntity.newOrganization();
    app1 = tempEntity.newApplicationWithSpecificId("app1",
        "PolicyEvaluationMigratorTest", "PolicyEvaluationMigratorTest", organization.getId());
    app2 = tempEntity.newApplicationWithSpecificId("app2",
        "PolicyEvaluationMigratorTest2", "PolicyEvaluationMigratorTest2", organization.getId());

    //this app has no evals at all, it should not contribute a new policy evaluation
    appNoEvals = tempEntity.newApplicationWithSpecificId("appNoEvals", "NoEvals", "NoEvals",
        organization.getId());
  }

  @Test
  public void testMigrateApps() throws Exception {
    policyEvaluationMigrator.migrate();

    //we should find 5 records for the first app in the build stage, one of them for monitoring, 2 originals and 2 for a re-evaluation(different scans)
    List<PolicyEvaluation> appBuildEvaluations = policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(), Stage.ID_BUILD);
    assertThat(appBuildEvaluations, hasSize(5));

    // build: monitoring is first because it is the newest evaluation
    PolicyEvaluation buildMonitoring = appBuildEvaluations.get(0);
    assertMonitoringPolicyEvaluation(buildMonitoring, Stage.ID_BUILD, true, true, app1.getId(), "buildScan", 1395857108660l);
    List<PolicyViolation> violations = policyViolationDAO.getByEvaluationId(buildMonitoring.getId());
    assertThat(violations, hasSize(1));
    assertCarrotSearchComponent(buildMonitoring.getId(), violations.get(0));

    // build: re-eval of second scan
    PolicyEvaluation secondScanReeval = appBuildEvaluations.get(1);
    assertPolicyEvaluation(secondScanReeval, Stage.ID_BUILD, true, false, app1.getId(), "buildScan2", 1395857108660l);
    violations = policyViolationDAO.getByEvaluationId(secondScanReeval.getId());
    assertThat(violations, hasSize(1));
    assertAntlrComponent(secondScanReeval.getId(), violations.get(0));

    // build: subset of results from other "original" scan, just newer
    PolicyEvaluation secondScan = appBuildEvaluations.get(2);
    assertPolicyEvaluation(secondScan, Stage.ID_BUILD, false, false, app1.getId(), "buildScan2", 1395857108659l);
    violations = policyViolationDAO.getByEvaluationId(secondScan.getId());
    assertThat(violations, hasSize(1));
    assertAntlrComponent(secondScan.getId(), violations.get(0));

    // build: next, only one of the two re-evaluations in the log is recorded
    PolicyEvaluation buildReEvaluation = appBuildEvaluations.get(3);
    assertPolicyEvaluation(buildReEvaluation, Stage.ID_BUILD, true, false, app1.getId(), "buildScan", 1395857108658l);
    violations = policyViolationDAO.getByEvaluationId(buildReEvaluation.getId());
    assertThat(violations, hasSize(1));
    assertAntlrComponent(buildReEvaluation.getId(), violations.get(0));

    // build: last is the original build evaluation
    PolicyEvaluation buildEvaluation = appBuildEvaluations.get(4);
    assertPolicyEvaluation(buildEvaluation, Stage.ID_BUILD, false, false, app1.getId(), "buildScan", 1395857108656l);
    violations = policyViolationDAO.getByEvaluationId(buildEvaluation.getId());
    assertThat(violations, hasSize(2));
    assertAntlrComponent(buildEvaluation.getId(), violations.get(0));
    assertUnknownComponent(buildEvaluation.getId(), violations.get(1));

    //we should also find 3 records for the first app in stage-release, monitoring, a re-eval and the original
    List<PolicyEvaluation> stageReleaseEvaluations = policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(), Stage.ID_STAGE_RELEASE);
    assertThat(stageReleaseEvaluations, hasSize(3));

    // stage: monitoring evaluation
    PolicyEvaluation stageReleaseMonitoringEvaluation = stageReleaseEvaluations.get(0);
    assertMonitoringPolicyEvaluation(stageReleaseMonitoringEvaluation, Stage.ID_STAGE_RELEASE, true, true, app1.getId(), "stageReleaseScan", 1395854414876l);
    List<PolicyViolation> stageReleaseMonitoringViolations = policyViolationDAO
        .getByEvaluationId(stageReleaseMonitoringEvaluation.getId());
    assertThat(stageReleaseMonitoringViolations, hasSize(1));
    assertAntlrComponent(stageReleaseMonitoringEvaluation.getId(), stageReleaseMonitoringViolations.get(0));

    // stage: re-evaluation
    PolicyEvaluation stageReleaseReevaluation = stageReleaseEvaluations.get(1);
    assertPolicyEvaluation(stageReleaseReevaluation, Stage.ID_STAGE_RELEASE, true, false, app1.getId(),
        "stageReleaseScan", 1395854414876l);
    List<PolicyViolation> stageReleaseReevalViolations = policyViolationDAO.getByEvaluationId(stageReleaseReevaluation
        .getId());
    assertThat(stageReleaseReevalViolations, hasSize(2));
    assertCarrotSearchComponent(stageReleaseReevaluation.getId(), stageReleaseReevalViolations.get(0));
    assertUnknownComponent(stageReleaseReevaluation.getId(), stageReleaseReevalViolations.get(1));

    // stage: original evaluation
    PolicyEvaluation originalStageReleaseEvaluation = stageReleaseEvaluations.get(2);
    assertPolicyEvaluation(originalStageReleaseEvaluation, Stage.ID_STAGE_RELEASE, false, false, app1.getId(),
        "stageReleaseScan", 1395854414875l);
    List<PolicyViolation> stageReleaseViolations = policyViolationDAO.getByEvaluationId(originalStageReleaseEvaluation
        .getId());
    assertThat(stageReleaseViolations, hasSize(2));
    assertCarrotSearchComponent(originalStageReleaseEvaluation.getId(), stageReleaseViolations.get(0));
    assertUnknownComponent(originalStageReleaseEvaluation.getId(), stageReleaseViolations.get(1));

    Collection<StageType> emptyStages = new ArrayList<>(StageTypes.getAll());
    emptyStages.remove(StageTypes.getById(Stage.ID_BUILD));
    emptyStages.remove(StageTypes.getById(Stage.ID_STAGE_RELEASE));
    for (StageType stageType : emptyStages) {
      assertThat(policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(), stageType.getId()), empty());
    }

    // "newest" policy violations for app1
    List<PolicyViolation> newestPolicyViolations = policyViolationDAO.getNewestByApplicationId(app1.getId());
    assertThat(newestPolicyViolations, hasSize(1));
    assertAntlrComponent(stageReleaseMonitoringEvaluation.getId(), newestPolicyViolations.get(0));
    NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();
    NewestPolicyViolation newestPolicyViolation = newestPolicyViolationDAO.getById(newestPolicyViolations.get(0)
        .getId());
    assertThat(newestPolicyViolation.getStageTypeId(), is(Stage.ID_STAGE_RELEASE));

    //second app
    List<PolicyEvaluation> app2BuildEvaluations = policyEvaluationDAO.getAllByApplicationIdAndStageId(app2.getId(), Stage.ID_RELEASE);
    assertThat(app2BuildEvaluations, hasSize(1));
    PolicyEvaluation policyEvaluation = app2BuildEvaluations.get(0);
    assertPolicyEvaluation(policyEvaluation, Stage.ID_RELEASE, false, false, app2.getId(), "86239d1f600444968309a763c71955e7", 1395857108659l);
    List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationId(policyEvaluation.getId());
    assertThat(policyViolations, hasSize(2));
    assertCarrotSearchComponent(policyEvaluation.getId(), policyViolations.get(0));
    assertUnknownComponent(policyEvaluation.getId(), policyViolations.get(1));

    emptyStages = new ArrayList<>(StageTypes.getAll());
    emptyStages.remove(StageTypes.getById(Stage.ID_RELEASE));
    for (StageType stageType : emptyStages) {
      assertThat(policyEvaluationDAO.getAllByApplicationIdAndStageId(app2.getId(), stageType.getId()), empty());
    }

    // "newest" policy violations for app2
    newestPolicyViolations = policyViolationDAO.getNewestByApplicationId(app2.getId());
    assertThat(newestPolicyViolations, hasSize(2));
    assertCarrotSearchComponent(policyEvaluation.getId(), newestPolicyViolations.get(0));
    assertUnknownComponent(policyEvaluation.getId(), newestPolicyViolations.get(1));
    newestPolicyViolation = newestPolicyViolationDAO.getById(newestPolicyViolations.get(0).getId());
    assertThat(newestPolicyViolation.getStageTypeId(), is(Stage.ID_RELEASE));
    newestPolicyViolation = newestPolicyViolationDAO.getById(newestPolicyViolations.get(1).getId());
    assertThat(newestPolicyViolation.getStageTypeId(), is(Stage.ID_RELEASE));
  }

  @Test
  public void testAppWithNoEvals() throws Exception {
    policyEvaluationMigrator.migrate();
    for (StageType stageType : StageTypes.getAll()) {
      assertThat(policyEvaluationDAO.getAllByApplicationIdAndStageId(appNoEvals.getId(), stageType.getId()), empty());
    }
    assertThat(policyViolationDAO.getNewestByApplicationId(appNoEvals.getId()), hasSize(0));
  }

  @Test
  public void testDeletionOfTrendingReport() throws Exception {
    assertThat("Trending report file is present before migration",
        new File(insightWork.getReportDir(), "trending-report.json").exists(), is(true));
    policyEvaluationMigrator.migrate();
    assertThat("Trending report file is deleted after migration",
        new File(insightWork.getReportDir(), "trending-report.json").exists(), is(false));
  }

  private void assertUnknownComponent(final String evaluationId, final PolicyViolation policyViolation) {
    assertPolicyViolation(evaluationId, policyViolation, "f8d39103fab24ec8a2677942640d3527",
        "Component-Unknown", 1, null, null, null, "318ed314c3e5bfb0bacb", PolicyThreatCategory.OTHER);
  }

  private void assertAntlrComponent(final String evaluationId, final PolicyViolation policyViolation) {
    assertPolicyViolation(evaluationId, policyViolation, "492542d33d1d42e8bc37a55e7130cbc0",
        "License-Declared Only", 5, "antlr", "antlr", "2.7.7", "83cd2cd674a217ade95a", PolicyThreatCategory.LICENSE);
  }

  private void assertCarrotSearchComponent(final String evaluationId, final PolicyViolation policyViolation) {
    assertPolicyViolation(evaluationId, policyViolation, "492542d33d1d42e8bc37a55e7130cbc0",
        "License-Declared Only", 5, "com.carrotsearch", "hppc", "0.5.2", "074bcc9d152a928a4ea9", PolicyThreatCategory.LICENSE);
  }


  private void assertPolicyViolation(final String evaluationId, final PolicyViolation policyViolation,
                                 final String policyId, final String policyName, final int threatLevel,
                                 final String groupId, final String artifactId, final String version,
                                 final String hash, final PolicyThreatCategory threatCategory)
  {
    assertThat(policyViolation.getPolicyEvaluationId(), is(evaluationId));
    assertThat(policyViolation.getPolicyId(), is(policyId));
    assertThat(policyViolation.getPolicyName(), is(policyName));
    assertThat(policyViolation.getThreatLevel(), is(threatLevel));
    assertThat(policyViolation.getGroupId(), is(groupId));
    assertThat(policyViolation.getArtifactId(), is(artifactId));
    assertThat(policyViolation.getVersion(), is(version));
    assertThat(policyViolation.getHash(), is(hash));
    assertThat(policyViolation.getThreatCategory(), is(threatCategory));
    assertThat(policyViolation.getConstraintFactsJson().length(), greaterThan(0));
  }

  private void assertPolicyEvaluation(final PolicyEvaluation policyEvaluation, final String stageId,
                                      final boolean reEvaluation, final boolean monitoring, final String applicationId,
                                      final String scanId, final long time) {
    assertThat(policyEvaluation.getStageTypeId(), is(stageId));
    assertThat(policyEvaluation.isReevaluation(), is(reEvaluation));
    assertThat(policyEvaluation.isForMonitoring(), is(monitoring));
    assertThat(policyEvaluation.getApplicationId(), is(applicationId));
    assertThat(policyEvaluation.getScanId(), is(scanId));
    assertThat(policyEvaluation.getTime(), is(new Date(time)));
  }

  /**
   * monitoring records use the file timestamp, so we check that indeed monitoring happened AFTER a known previous record
   */
  private void assertMonitoringPolicyEvaluation(final PolicyEvaluation policyEvaluation, final String stageId,
                                                final boolean reEvaluation, final boolean monitoring, final String applicationId,
                                                final String scanId, final long time) {
    assertThat(policyEvaluation.getStageTypeId(), is(stageId));
    assertThat(policyEvaluation.isReevaluation(), is(reEvaluation));
    assertThat(policyEvaluation.isForMonitoring(), is(monitoring));
    assertThat(policyEvaluation.getApplicationId(), is(applicationId));
    assertThat(policyEvaluation.getScanId(), is(scanId));
    assertThat(policyEvaluation.getTime().getTime(), greaterThan(time));
  }
}
