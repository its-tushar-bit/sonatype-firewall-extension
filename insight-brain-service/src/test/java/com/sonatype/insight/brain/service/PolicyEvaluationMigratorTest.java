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

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.NewestPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PolicyEvaluationMigratorTest
{
  private static final String COMPONENT_HASH_ANTLR = "83cd2cd674a217ade95a";

  private static final String COMPONENT_HASH_CARROT = "074bcc9d152a928a4ea9";

  private static final String COMPONENT_HASH_UNKNOWN = "318ed314c3e5bfb0bacb";
  
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

  public void setup(String testDataDir) throws IOException {
    sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
    policyEvaluationMigrator = new PolicyEvaluationMigrator(insightWork);

    //provide evaluation logs and dummied up reports(zip has no content and the report.cache files are loaded) for test purposes
    FileUtils.copyDirectory(new File("target/test-classes", testDataDir), sonatypeWork);
    Organization organization = tempEntity.newOrganization();
    app1 = tempEntity.newApplicationWithSpecificId("app1",
        "PolicyEvaluationMigratorTest", "PolicyEvaluationMigratorTest", organization.getId());
    app2 = tempEntity.newApplicationWithSpecificId("app2",
        "PolicyEvaluationMigratorTest2", "PolicyEvaluationMigratorTest2", organization.getId());

    //this app has no evals at all, it should not contribute a new policy evaluation
    appNoEvals = tempEntity.newApplicationWithSpecificId("appNoEvals", "NoEvals", "NoEvals",
        organization.getId());
  }

  private void assertPolicyViolationActions(PolicyViolation policyViolation, String actionTypeId,
      String... notifications)
  {
    assertThat(policyViolation.getActionTypeId(), is(actionTypeId));
    if (notifications.length == 0) {
      assertThat(policyViolation.getNotifications(), hasSize(0));
    }
    else {
      assertThat(policyViolation.getNotifications(), containsInAnyOrder(notifications));
    }
  }
  
  @Test
  public void testMigrateMultipleApps() throws Exception {
    setup("PolicyEvaluationMigratorTest/MultipleApplications");

    policyEvaluationMigrator.migrate();

    //we should find 5 records for the first app in the build stage, one of them for monitoring, 2 originals and 2 for a re-evaluation(different scans)
    List<PolicyEvaluation> appBuildEvaluations = policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(), Stage.ID_BUILD);
    assertThat(appBuildEvaluations, hasSize(5));

    // build: monitoring is first because it is the newest evaluation
    PolicyEvaluation buildMonitoring = appBuildEvaluations.get(0);
    assertMonitoringPolicyEvaluation(buildMonitoring, Stage.ID_BUILD, true, true, app1.getId(), "buildScan", 1395857108660l);
    List<PolicyViolation> violations = policyViolationDAO.getByEvaluationId(buildMonitoring.getId());
    assertThat(violations, hasSize(1));
    assertCarrotPolicyViolation(buildMonitoring.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_WARN, "monitor@example.com");

    // build: re-eval of second scan
    PolicyEvaluation secondScanReeval = appBuildEvaluations.get(1);
    assertPolicyEvaluation(secondScanReeval, Stage.ID_BUILD, true, false, app1.getId(), "buildScan2", 1395857108660l);
    violations = policyViolationDAO.getByEvaluationId(secondScanReeval.getId());
    assertThat(violations, hasSize(1));
    assertAntlrPolicyViolation(secondScanReeval.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_FAIL, "me@example.com", "you@example.com");

    // build: subset of results from other "original" scan, just newer
    PolicyEvaluation secondScan = appBuildEvaluations.get(2);
    assertPolicyEvaluation(secondScan, Stage.ID_BUILD, false, false, app1.getId(), "buildScan2", 1395857108659l);
    violations = policyViolationDAO.getByEvaluationId(secondScan.getId());
    assertThat(violations, hasSize(1));
    assertAntlrPolicyViolation(secondScan.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_WARN, "him@example.com");

    // build: next, only one of the two re-evaluations in the log is recorded
    PolicyEvaluation buildReEvaluation = appBuildEvaluations.get(3);
    assertPolicyEvaluation(buildReEvaluation, Stage.ID_BUILD, true, false, app1.getId(), "buildScan", 1395857108658l);
    violations = policyViolationDAO.getByEvaluationId(buildReEvaluation.getId());
    assertThat(violations, hasSize(1));
    assertAntlrPolicyViolation(buildReEvaluation.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_WARN);

    // build: last is the original build evaluation
    PolicyEvaluation buildEvaluation = appBuildEvaluations.get(4);
    assertPolicyEvaluation(buildEvaluation, Stage.ID_BUILD, false, false, app1.getId(), "buildScan", 1395857108656l);
    violations = policyViolationDAO.getByEvaluationId(buildEvaluation.getId());
    assertThat(violations, hasSize(2));
    assertAntlrPolicyViolation(buildEvaluation.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_FAIL, "her@example.com");
    assertUnknownPolicyViolation(buildEvaluation.getId(), violations.get(1));
    assertPolicyViolationActions(violations.get(1), null /* actionTypeId */);

    //we should also find 3 records for the first app in stage-release, monitoring, a re-eval and the original
    List<PolicyEvaluation> stageReleaseEvaluations = policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(), Stage.ID_STAGE_RELEASE);
    assertThat(stageReleaseEvaluations, hasSize(3));

    // stage: monitoring evaluation
    PolicyEvaluation stageReleaseMonitoringEvaluation = stageReleaseEvaluations.get(0);
    assertMonitoringPolicyEvaluation(stageReleaseMonitoringEvaluation, Stage.ID_STAGE_RELEASE, true, true, app1.getId(), "stageReleaseScan", 1395854414876l);
    List<PolicyViolation> stageReleaseMonitoringViolations = policyViolationDAO
        .getByEvaluationId(stageReleaseMonitoringEvaluation.getId());
    assertThat(stageReleaseMonitoringViolations, hasSize(1));
    assertAntlrPolicyViolation(stageReleaseMonitoringEvaluation.getId(), stageReleaseMonitoringViolations.get(0));
    assertPolicyViolationActions(stageReleaseMonitoringViolations.get(0), Action.ID_WARN);

    // stage: re-evaluation
    PolicyEvaluation stageReleaseReevaluation = stageReleaseEvaluations.get(1);
    assertPolicyEvaluation(stageReleaseReevaluation, Stage.ID_STAGE_RELEASE, true, false, app1.getId(),
        "stageReleaseScan", 1395854414876l);
    List<PolicyViolation> stageReleaseReevalViolations = policyViolationDAO.getByEvaluationId(stageReleaseReevaluation
        .getId());
    assertThat(stageReleaseReevalViolations, hasSize(2));
    assertCarrotPolicyViolation(stageReleaseReevaluation.getId(), stageReleaseReevalViolations.get(0));
    assertUnknownPolicyViolation(stageReleaseReevaluation.getId(), stageReleaseReevalViolations.get(1));
    assertPolicyViolationActions(stageReleaseReevalViolations.get(0), Action.ID_WARN);
    assertPolicyViolationActions(stageReleaseReevalViolations.get(1), null /* actionTypeId */);

    // stage: original evaluation
    PolicyEvaluation originalStageReleaseEvaluation = stageReleaseEvaluations.get(2);
    assertPolicyEvaluation(originalStageReleaseEvaluation, Stage.ID_STAGE_RELEASE, false, false, app1.getId(),
        "stageReleaseScan", 1395854414875l);
    List<PolicyViolation> stageReleaseViolations = policyViolationDAO.getByEvaluationId(originalStageReleaseEvaluation
        .getId());
    assertThat(stageReleaseViolations, hasSize(2));
    assertCarrotPolicyViolation(originalStageReleaseEvaluation.getId(), stageReleaseViolations.get(0));
    assertUnknownPolicyViolation(originalStageReleaseEvaluation.getId(), stageReleaseViolations.get(1));
    assertPolicyViolationActions(stageReleaseViolations.get(0), Action.ID_WARN);
    assertPolicyViolationActions(stageReleaseViolations.get(1), null /* actionTypeId */);

    Collection<StageType> emptyStages = new ArrayList<>(StageTypes.getAll());
    emptyStages.remove(StageTypes.getById(Stage.ID_BUILD));
    emptyStages.remove(StageTypes.getById(Stage.ID_STAGE_RELEASE));
    for (StageType stageType : emptyStages) {
      assertThat(policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(), stageType.getId()), empty());
    }

    // "newest" policy violations for app1
    List<PolicyViolation> newestPolicyViolations = policyViolationDAO.getNewestByApplicationIdAndStageTypeId(
        app1.getId(), Stage.ID_BUILD);
    assertThat(newestPolicyViolations, hasSize(1));
    assertAntlrPolicyViolation(buildEvaluation.getId(), newestPolicyViolations.get(0));
    NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();
    NewestPolicyViolation newestPolicyViolation = newestPolicyViolationDAO.getById(newestPolicyViolations.get(0)
        .getId());
    assertThat(newestPolicyViolation.getStageTypeId(), is(Stage.ID_BUILD));
    newestPolicyViolations = policyViolationDAO.getNewestByApplicationIdAndStageTypeId(app1.getId(),
        Stage.ID_STAGE_RELEASE);
    assertThat(newestPolicyViolations, hasSize(1));
    assertAntlrPolicyViolation(stageReleaseMonitoringEvaluation.getId(), newestPolicyViolations.get(0));
    newestPolicyViolation = newestPolicyViolationDAO.getById(newestPolicyViolations.get(0).getId());
    assertThat(newestPolicyViolation.getStageTypeId(), is(Stage.ID_STAGE_RELEASE));

    // application components for app1
    ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();
    List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(app1.getId(),
        Stage.ID_BUILD);
    assertThat(appComponents, hasSize(3));
    assertCarrotAppComponent(appComponents, Stage.ID_BUILD, secondScanReeval.getTime());
    assertAntlrAppComponent(appComponents, Stage.ID_BUILD, secondScanReeval.getTime());
    assertUnknownAppComponent(appComponents, Stage.ID_BUILD, secondScanReeval.getTime());
    appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(app1.getId(), Stage.ID_STAGE_RELEASE);
    assertThat(appComponents, hasSize(4));
    assertCarrotAppComponent(appComponents, Stage.ID_STAGE_RELEASE, stageReleaseMonitoringEvaluation.getTime());
    assertAntlrAppComponent(appComponents, Stage.ID_STAGE_RELEASE, stageReleaseMonitoringEvaluation.getTime());
    assertUnknownAppComponent(appComponents, Stage.ID_STAGE_RELEASE, stageReleaseMonitoringEvaluation.getTime());

    //second app
    List<PolicyEvaluation> app2BuildEvaluations = policyEvaluationDAO.getAllByApplicationIdAndStageId(app2.getId(), Stage.ID_RELEASE);
    assertThat(app2BuildEvaluations, hasSize(1));
    PolicyEvaluation policyEvaluation = app2BuildEvaluations.get(0);
    assertPolicyEvaluation(policyEvaluation, Stage.ID_RELEASE, false, false, app2.getId(), "86239d1f600444968309a763c71955e7", 1395857108659l);
    List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationId(policyEvaluation.getId());
    assertThat(policyViolations, hasSize(2));
    assertCarrotPolicyViolation(policyEvaluation.getId(), policyViolations.get(0));
    assertUnknownPolicyViolation(policyEvaluation.getId(), policyViolations.get(1));

    emptyStages = new ArrayList<>(StageTypes.getAll());
    emptyStages.remove(StageTypes.getById(Stage.ID_RELEASE));
    for (StageType stageType : emptyStages) {
      assertThat(policyEvaluationDAO.getAllByApplicationIdAndStageId(app2.getId(), stageType.getId()), empty());
    }

    // "newest" policy violations for app2
    newestPolicyViolations = policyViolationDAO.getNewestByApplicationIdAndStageTypeId(app2.getId(), Stage.ID_RELEASE);
    assertThat(newestPolicyViolations, hasSize(2));
    assertCarrotPolicyViolation(policyEvaluation.getId(), newestPolicyViolations.get(0));
    assertUnknownPolicyViolation(policyEvaluation.getId(), newestPolicyViolations.get(1));
    newestPolicyViolation = newestPolicyViolationDAO.getById(newestPolicyViolations.get(0).getId());
    assertThat(newestPolicyViolation.getStageTypeId(), is(Stage.ID_RELEASE));
    newestPolicyViolation = newestPolicyViolationDAO.getById(newestPolicyViolations.get(1).getId());
    assertThat(newestPolicyViolation.getStageTypeId(), is(Stage.ID_RELEASE));

    // application components for app2
    appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(app2.getId(), Stage.ID_RELEASE);
    assertThat(appComponents, hasSize(2));
    assertCarrotAppComponent(appComponents, Stage.ID_RELEASE, policyEvaluation.getTime());
    assertUnknownAppComponent(appComponents, Stage.ID_RELEASE, policyEvaluation.getTime());

    // application without policy evaluations
    for (StageType stageType : StageTypes.getAll()) {
      assertThat(policyEvaluationDAO.getAllByApplicationIdAndStageId(appNoEvals.getId(), stageType.getId()), empty());
    }
  }

  @Test
  public void testComponentWithoutHash() throws Exception {
    setup("PolicyEvaluationMigratorTest/ComponentWithoutHash");

    policyEvaluationMigrator.migrate();

    List<PolicyEvaluation> appBuildEvaluations = policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(), Stage.ID_BUILD);
    assertThat(appBuildEvaluations, hasSize(1));

    ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();
    List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(app1.getId(),
        Stage.ID_BUILD);
    assertThat(appComponents, hasSize(0));
  }

  @Test
  public void testReportDoesNotExist() throws Exception {
    setup("PolicyEvaluationMigratorTest/ReportDoesNotExist");

    policyEvaluationMigrator.migrate();

    List<PolicyEvaluation> appBuildEvaluations = policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(),
        Stage.ID_BUILD);
    assertThat(appBuildEvaluations, hasSize(1));

    ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();
    List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(app1.getId(),
        Stage.ID_BUILD);
    assertThat(appComponents, hasSize(0));
  }

  @Test
  public void testBomJsonDoesNotExist() throws Exception {
    setup("PolicyEvaluationMigratorTest/BomJsonDoesNotExist");

    policyEvaluationMigrator.migrate();

    List<PolicyEvaluation> appBuildEvaluations = policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(),
        Stage.ID_BUILD);
    assertThat(appBuildEvaluations, hasSize(1));

    ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();
    List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(app1.getId(),
        Stage.ID_BUILD);
    assertThat(appComponents, hasSize(0));
  }

  private void assertUnknownPolicyViolation(final String evaluationId, final PolicyViolation policyViolation) {
    assertPolicyViolation(evaluationId, policyViolation, "f8d39103fab24ec8a2677942640d3527", "Component-Unknown", 1,
        null, null, null, COMPONENT_HASH_UNKNOWN, PolicyThreatCategory.OTHER,
        Lists.newArrayList("commons-httpclient-3.1.SONATYPE.jar"));
  }

  private void assertAntlrPolicyViolation(final String evaluationId, final PolicyViolation policyViolation) {
    assertPolicyViolation(evaluationId, policyViolation, "492542d33d1d42e8bc37a55e7130cbc0", "License-Declared Only",
        5, "antlr", "antlr", "2.7.7", COMPONENT_HASH_ANTLR, PolicyThreatCategory.LICENSE,
        Lists.newArrayList("antlr.antlr.2.7.7.jar", "shaded-product.jar"));
  }

  private void assertCarrotPolicyViolation(final String evaluationId, final PolicyViolation policyViolation) {
    assertPolicyViolation(evaluationId, policyViolation, "492542d33d1d42e8bc37a55e7130cbc0", "License-Declared Only",
        5, "com.carrotsearch", "hppc", "0.5.2", COMPONENT_HASH_CARROT, PolicyThreatCategory.LICENSE,
        Lists.newArrayList("com.carrotsearch.hppc.0.5.2.jar"));
  }

  private void assertUnknownAppComponent(List<ApplicationComponent> appComponents, String stageTypeId, Date time) {
    for (ApplicationComponent appComponent : appComponents) {
      if (COMPONENT_HASH_UNKNOWN.equals(appComponent.getHash())) {
        assertAppComponent(appComponent, null /* groupId */, null /* artifactId */, null /* version */, stageTypeId,
            time, MatchState.UNKNOWN.getId(), IdentificationSource.SONATYPE.getId(), false /* proprietary */,
            Lists.newArrayList("commons-httpclient-3.1.SONATYPE.jar"));
        return;
      }
    }
    fail("Cannot find unknown ApplicationComponent");
  }

  private void assertCarrotAppComponent(List<ApplicationComponent> appComponents, String stageTypeId, Date time) {
    for (ApplicationComponent appComponent : appComponents) {
      if (COMPONENT_HASH_CARROT.equals(appComponent.getHash())) {
        assertAppComponent(appComponent, "com.carrotsearch", "hppc", "0.5.2", stageTypeId, time,
            MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false /* proprietary */,
            Lists.newArrayList("com.carrotsearch.hppc.0.5.2.jar"));
        return;
      }
    }
    fail("Cannot find carrot ApplicationComponent");
  }

  private void assertAntlrAppComponent(List<ApplicationComponent> appComponents, String stageTypeId, Date time) {
    for (ApplicationComponent appComponent : appComponents) {
      if (COMPONENT_HASH_ANTLR.equals(appComponent.getHash())) {
        assertAppComponent(appComponent, "antlr", "antlr", "2.7.7", stageTypeId, time, MatchState.EXACT.getId(),
            IdentificationSource.SONATYPE.getId(), false /* proprietary */,
            Lists.newArrayList("antlr.antlr.2.7.7.jar", "shaded-product.jar"));
        return;
      }
    }
    fail("Cannot find antlr ApplicationComponent");
  }

  private void assertAppComponent(ApplicationComponent actual, String groupId, String artifactId, String version,
      String stageTypeId, Date time, String matchStateId, String identificationSourceId, boolean proprietary,
      List<String> pathnames)
  {
    assertThat(actual.getGroupId(), is(groupId));
    assertThat(actual.getArtifactId(), is(artifactId));
    assertThat(actual.getVersion(), is(version));
    assertThat(actual.getStageTypeId(), is(stageTypeId));
    assertThat(actual.getTime(), is(time));
    assertThat(actual.getMatchStateId(), is(matchStateId));
    assertThat(actual.getIdentificationSourceId(), is(identificationSourceId));
    assertThat(actual.isProprietary(), is(proprietary));
    assertThat(actual.getPathnames(), is(pathnames));
  }

  private void assertPolicyViolation(final String evaluationId, final PolicyViolation policyViolation,
      final String policyId, final String policyName, final int threatLevel, final String groupId,
      final String artifactId, final String version, final String hash, final PolicyThreatCategory threatCategory,
      final List<String> pathnames)
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
    assertThat(policyViolation.getPathnames(), is(pathnames));
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
