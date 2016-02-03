/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.FirstOccurrencePolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

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

    // provide evaluation logs and dummied up reports(zip has no content and the report.cache files are loaded) for test
    // purposes
    FileUtils.copyDirectory(new File("target/test-classes", testDataDir), sonatypeWork);
    Organization organization = tempEntity.newOrganization();
    app1 = tempEntity.newApplicationWithSpecificId("app1", "PolicyEvaluationMigratorTest",
        "PolicyEvaluationMigratorTest", organization.getId());
    app2 = tempEntity.newApplicationWithSpecificId("app2", "PolicyEvaluationMigratorTest2",
        "PolicyEvaluationMigratorTest2", organization.getId());

    // this app has no evals at all, it should not contribute a new policy evaluation
    appNoEvals = tempEntity.newApplicationWithSpecificId("appNoEvals", "NoEvals", "NoEvals", organization.getId());
  }

  private void assertPolicyViolationActions(PolicyViolation policyViolation,
                                            String actionTypeId,
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

    // The build copies the resource files to target and that changes the file time stamps.
    // We use the file time stamps to populate monitoring policy evaluation times, so we need to fix the time stamps
    // here.
    // The file time stamp resolution is limited to seconds on some linux systems, so it is important to use times that
    // are whole seconds only here.
    fixReportEntryLastModified(app1.getId(), "buildScan", PolicyEvaluationMigrator.MONITOR_POLICY_ALERTS_FILE,
        1395857105000L);
    fixReportEntryLastModified(app1.getId(), "stageReleaseScan", PolicyEvaluationMigrator.MONITOR_POLICY_ALERTS_FILE,
        1395854414000L);

    policyEvaluationMigrator.migrate();

    // we should find 5 records for the first app in the build stage, one of them for monitoring, 2 originals and 2 for
    // a re-evaluation(different scans)
    List<PolicyEvaluation> app1BuildEvals = policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(),
        Stage.ID_BUILD);
    assertThat(app1BuildEvals, hasSize(5));
    for (PolicyEvaluation policyEvaluation : app1BuildEvals) {
      System.out.println(policyEvaluation);
    }

    // build: re-eval of second scan
    PolicyEvaluation build2Reeval = app1BuildEvals.get(0);
    assertPolicyEvaluation(build2Reeval, Stage.ID_BUILD, true, false, app1.getId(), "buildScan2", 1395857140660L, false);
    List<PolicyViolation> violations = policyViolationDAO.getActiveByEvaluationId(build2Reeval.getId());
    assertThat(violations, hasSize(1));
    assertAntlrPolicyViolation(build2Reeval.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_FAIL, "me@example.com", "you@example.com");

    // build: subset of results from other "original" scan, just newer
    PolicyEvaluation build2Eval = app1BuildEvals.get(1);
    assertPolicyEvaluation(build2Eval, Stage.ID_BUILD, false, false, app1.getId(), "buildScan2", 1395857130659L, false);
    violations = policyViolationDAO.getActiveByEvaluationId(build2Eval.getId());
    assertThat(violations, hasSize(1));
    assertAntlrPolicyViolation(build2Eval.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_WARN, "him@example.com");

    // build: next, only one of the two re-evaluations in the log is recorded
    PolicyEvaluation buildReeval = app1BuildEvals.get(2);
    assertPolicyEvaluation(buildReeval, Stage.ID_BUILD, true, false, app1.getId(), "buildScan", 1395857120658L, false);
    violations = policyViolationDAO.getActiveByEvaluationId(buildReeval.getId());
    assertThat(violations, hasSize(1));
    assertAntlrPolicyViolation(buildReeval.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_WARN);

    // build: monitoring
    PolicyEvaluation buildMonitoringEval = app1BuildEvals.get(3);
    assertPolicyEvaluation(buildMonitoringEval, Stage.ID_BUILD, true, true, app1.getId(), "buildScan", 1395857105000L,
        false);
    violations = policyViolationDAO.getActiveByEvaluationId(buildMonitoringEval.getId());
    assertThat(violations, hasSize(2));
    assertAntlrPolicyViolation(buildMonitoringEval.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_FAIL, "her@example.com");
    assertUnknownPolicyViolation(buildMonitoringEval.getId(), violations.get(1));
    assertPolicyViolationActions(violations.get(1), null /* actionTypeId */);

    // build: last is the original build evaluation
    PolicyEvaluation buildEval = app1BuildEvals.get(4);
    assertPolicyEvaluation(buildEval, Stage.ID_BUILD, false, false, app1.getId(), "buildScan", 1395857100656L, false);
    violations = policyViolationDAO.getActiveByEvaluationId(buildEval.getId());
    assertThat(violations, hasSize(2));
    assertAntlrPolicyViolation(buildEval.getId(), violations.get(0));
    assertPolicyViolationActions(violations.get(0), Action.ID_FAIL, "her@example.com");
    assertUnknownPolicyViolation(buildEval.getId(), violations.get(1));
    assertPolicyViolationActions(violations.get(1), null /* actionTypeId */);

    // we should also find 3 records for the first app in stage-release, monitoring, a re-eval and the original
    List<PolicyEvaluation> app1StageReleaseEvals = policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(),
        Stage.ID_STAGE_RELEASE);
    assertThat(app1StageReleaseEvals, hasSize(3));

    // stage: re-evaluation
    PolicyEvaluation stageReleaseReeval = app1StageReleaseEvals.get(0);
    assertPolicyEvaluation(stageReleaseReeval, Stage.ID_STAGE_RELEASE, true, false, app1.getId(), "stageReleaseScan",
        1395854420876L, false);
    List<PolicyViolation> stageReleaseReevalViolations = policyViolationDAO.getActiveByEvaluationId(stageReleaseReeval
        .getId());
    assertThat(stageReleaseReevalViolations, hasSize(2));
    assertCarrotPolicyViolation(stageReleaseReeval.getId(), stageReleaseReevalViolations.get(0));
    assertUnknownPolicyViolation(stageReleaseReeval.getId(), stageReleaseReevalViolations.get(1));
    assertPolicyViolationActions(stageReleaseReevalViolations.get(0), Action.ID_WARN);
    assertPolicyViolationActions(stageReleaseReevalViolations.get(1), null /* actionTypeId */);

    // stage: monitoring evaluation
    PolicyEvaluation stageReleaseMonitoringEval = app1StageReleaseEvals.get(1);
    assertPolicyEvaluation(stageReleaseMonitoringEval, Stage.ID_STAGE_RELEASE, true, true, app1.getId(),
        "stageReleaseScan", 1395854414000L, false);
    List<PolicyViolation> stageReleaseMonitoringViolations = policyViolationDAO
        .getActiveByEvaluationId(stageReleaseMonitoringEval.getId());
    assertThat(stageReleaseMonitoringViolations, hasSize(1));
    assertAntlrPolicyViolation(stageReleaseMonitoringEval.getId(), stageReleaseMonitoringViolations.get(0));
    assertPolicyViolationActions(stageReleaseMonitoringViolations.get(0), Action.ID_WARN);

    // stage: original evaluation
    PolicyEvaluation stageReleaseEval = app1StageReleaseEvals.get(2);
    assertPolicyEvaluation(stageReleaseEval, Stage.ID_STAGE_RELEASE, false, false, app1.getId(), "stageReleaseScan",
        1395854410874L, false);
    List<PolicyViolation> stageReleaseViolations = policyViolationDAO.getActiveByEvaluationId(stageReleaseEval.getId());
    assertThat(stageReleaseViolations, hasSize(2));
    assertCarrotPolicyViolation(stageReleaseEval.getId(), stageReleaseViolations.get(0));
    assertUnknownPolicyViolation(stageReleaseEval.getId(), stageReleaseViolations.get(1));
    assertPolicyViolationActions(stageReleaseViolations.get(0), Action.ID_WARN);
    assertPolicyViolationActions(stageReleaseViolations.get(1), null /* actionTypeId */);

    Collection<StageType> emptyStages = new ArrayList<>(StageTypes.getAll());
    emptyStages.remove(StageTypes.BUILD);
    emptyStages.remove(StageTypes.STAGE_RELEASE);
    for (StageType stageType : emptyStages) {
      assertThat(policyEvaluationDAO.getAllByApplicationIdAndStageId(app1.getId(), stageType.getId()), empty());
    }

    // first occurrence policy violations for app1
    List<PolicyViolation> firstOccurrencePolicyViolations = policyViolationDAO
        .getFirstOccurrenceByApplicationIdAndStageTypeId(app1.getId(), Stage.ID_BUILD);
    assertThat(firstOccurrencePolicyViolations, hasSize(1));
    assertAntlrPolicyViolation(buildEval.getId(), firstOccurrencePolicyViolations.get(0));
    FirstOccurrencePolicyViolationDAO firstOccurrencePolicyViolationDAO = new FirstOccurrencePolicyViolationDAO();
    FirstOccurrencePolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationDAO
        .getById(firstOccurrencePolicyViolations.get(0).getId());
    assertThat(firstOccurrencePolicyViolation.getStageTypeId(), is(Stage.ID_BUILD));
    firstOccurrencePolicyViolations = policyViolationDAO.getFirstOccurrenceByApplicationIdAndStageTypeId(app1.getId(),
        Stage.ID_STAGE_RELEASE);
    assertThat(firstOccurrencePolicyViolations, hasSize(2));
    firstOccurrencePolicyViolations = sort(firstOccurrencePolicyViolations);
    assertCarrotPolicyViolation(stageReleaseReeval.getId(), firstOccurrencePolicyViolations.get(0));
    firstOccurrencePolicyViolation = firstOccurrencePolicyViolationDAO.getById(firstOccurrencePolicyViolations.get(0)
        .getId());
    assertThat(firstOccurrencePolicyViolation.getStageTypeId(), is(Stage.ID_STAGE_RELEASE));
    assertUnknownPolicyViolation(stageReleaseReeval.getId(), firstOccurrencePolicyViolations.get(1));
    firstOccurrencePolicyViolation = firstOccurrencePolicyViolationDAO.getById(firstOccurrencePolicyViolations.get(1)
        .getId());
    assertThat(firstOccurrencePolicyViolation.getStageTypeId(), is(Stage.ID_STAGE_RELEASE));

    // application components for app1
    ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();
    List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(app1.getId(),
        Stage.ID_BUILD);
    assertThat(appComponents, hasSize(3));
    assertCarrotAppComponent(appComponents, Stage.ID_BUILD, build2Reeval.getTime());
    assertAntlrAppComponent(appComponents, Stage.ID_BUILD, build2Reeval.getTime());
    assertUnknownAppComponent(appComponents, Stage.ID_BUILD, build2Reeval.getTime());
    appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(app1.getId(), Stage.ID_STAGE_RELEASE);
    assertThat(appComponents, hasSize(4));
    assertCarrotAppComponent(appComponents, Stage.ID_STAGE_RELEASE, stageReleaseReeval.getTime());
    assertAntlrAppComponent(appComponents, Stage.ID_STAGE_RELEASE, stageReleaseReeval.getTime());
    assertUnknownAppComponent(appComponents, Stage.ID_STAGE_RELEASE, stageReleaseReeval.getTime());

    // second app
    List<PolicyEvaluation> app2ReleaseEvals = policyEvaluationDAO.getAllByApplicationIdAndStageId(app2.getId(),
        Stage.ID_RELEASE);
    assertThat(app2ReleaseEvals, hasSize(1));
    PolicyEvaluation app2ReleaseEval = app2ReleaseEvals.get(0);
    assertPolicyEvaluation(app2ReleaseEval, Stage.ID_RELEASE, false, false, app2.getId(),
        "86239d1f600444968309a763c71955e7", 1395857108659l, false);
    List<PolicyViolation> policyViolations = policyViolationDAO.getActiveByEvaluationId(app2ReleaseEval.getId());
    assertThat(policyViolations, hasSize(2));
    assertCarrotPolicyViolation(app2ReleaseEval.getId(), policyViolations.get(0));
    assertUnknownPolicyViolation(app2ReleaseEval.getId(), policyViolations.get(1));

    emptyStages = new ArrayList<>(StageTypes.getAll());
    emptyStages.remove(StageTypes.RELEASE);
    for (StageType stageType : emptyStages) {
      assertThat(policyEvaluationDAO.getAllByApplicationIdAndStageId(app2.getId(), stageType.getId()), empty());
    }

    // first occurrence policy violations for app2
    firstOccurrencePolicyViolations = policyViolationDAO.getFirstOccurrenceByApplicationIdAndStageTypeId(app2.getId(),
        Stage.ID_RELEASE);
    assertThat(firstOccurrencePolicyViolations, hasSize(2));
    firstOccurrencePolicyViolations = sort(firstOccurrencePolicyViolations);
    assertCarrotPolicyViolation(app2ReleaseEval.getId(), firstOccurrencePolicyViolations.get(0));
    assertUnknownPolicyViolation(app2ReleaseEval.getId(), firstOccurrencePolicyViolations.get(1));
    firstOccurrencePolicyViolation = firstOccurrencePolicyViolationDAO.getById(firstOccurrencePolicyViolations.get(0)
        .getId());
    assertThat(firstOccurrencePolicyViolation.getStageTypeId(), is(Stage.ID_RELEASE));
    firstOccurrencePolicyViolation = firstOccurrencePolicyViolationDAO.getById(firstOccurrencePolicyViolations.get(1)
        .getId());
    assertThat(firstOccurrencePolicyViolation.getStageTypeId(), is(Stage.ID_RELEASE));

    // application components for app2
    appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(app2.getId(), Stage.ID_RELEASE);
    assertThat(appComponents, hasSize(2));
    assertCarrotAppComponent(appComponents, Stage.ID_RELEASE, app2ReleaseEval.getTime());
    assertUnknownAppComponent(appComponents, Stage.ID_RELEASE, app2ReleaseEval.getTime());

    // application without policy evaluations
    for (StageType stageType : StageTypes.getAll()) {
      assertThat(policyEvaluationDAO.getAllByApplicationIdAndStageId(appNoEvals.getId(), stageType.getId()), empty());
    }
  }

  private List<PolicyViolation> sort(List<PolicyViolation> policyViolations) {
    List<PolicyViolation> result = new ArrayList<>(policyViolations);
    Collections.sort(result, PolicyViolationComparator.COMPARATOR);
    return result;
  }

  @Test
  public void testComponentWithoutHash() throws Exception {
    setup("PolicyEvaluationMigratorTest/ComponentWithoutHash");

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
        assertAppComponent(appComponent, null /* componentIdentifier */, stageTypeId, time, MatchState.UNKNOWN.getId(),
            IdentificationSource.SONATYPE.getId(), false /* proprietary */,
            Lists.newArrayList("commons-httpclient-3.1.SONATYPE.jar"));
        return;
      }
    }
    fail("Cannot find unknown ApplicationComponent");
  }

  private void assertCarrotAppComponent(List<ApplicationComponent> appComponents, String stageTypeId, Date time) {
    for (ApplicationComponent appComponent : appComponents) {
      if (COMPONENT_HASH_CARROT.equals(appComponent.getHash())) {
        assertAppComponent(appComponent,
            ComponentIdentifier.createMavenCoordinates("com.carrotsearch", "hppc", "0.5.2"), stageTypeId, time,
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
        assertAppComponent(appComponent, ComponentIdentifier.createMavenCoordinates("antlr", "antlr", "2.7.7"),
            stageTypeId, time, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(),
            false /* proprietary */, Lists.newArrayList("antlr.antlr.2.7.7.jar", "shaded-product.jar"));
        return;
      }
    }
    fail("Cannot find antlr ApplicationComponent");
  }

  private void assertAppComponent(ApplicationComponent actual,
                                  ComponentIdentifier componentIdentifier,
                                  String stageTypeId,
                                  Date time,
                                  String matchStateId,
                                  String identificationSourceId,
                                  boolean proprietary,
                                  List<String> pathnames)
  {
    assertThat(actual.getComponentIdentifier(), is(componentIdentifier));
    assertThat(actual.getStageTypeId(), is(stageTypeId));
    assertThat(actual.getTime(), is(time));
    assertThat(actual.getMatchStateId(), is(matchStateId));
    assertThat(actual.getIdentificationSourceId(), is(identificationSourceId));
    assertThat(actual.isProprietary(), is(proprietary));
    assertThat(actual.getPathnames(), is(pathnames));
  }

  private void assertPolicyViolation(final String evaluationId,
                                     final PolicyViolation policyViolation,
                                     final String policyId,
                                     final String policyName,
                                     final int threatLevel,
                                     final String groupId,
                                     final String artifactId,
                                     final String version,
                                     final String hash,
                                     final PolicyThreatCategory threatCategory,
                                     final List<String> pathnames)
  {
    assertThat(policyViolation.getPolicyEvaluationId(), is(evaluationId));
    assertThat(policyViolation.getPolicyId(), is(policyId));
    assertThat(policyViolation.getPolicyName(), is(policyName));
    assertThat(policyViolation.getThreatLevel(), is(threatLevel));
    ComponentIdentifier componentIdentifier = (groupId != null ? ComponentIdentifier.createMavenCoordinates(groupId,
        artifactId, version) : null);
    assertThat(policyViolation.getComponentIdentifier(), is(componentIdentifier));
    assertThat(policyViolation.getHash(), is(hash));
    assertThat(policyViolation.getThreatCategory(), is(threatCategory));
    assertThat(policyViolation.getPathnames(), is(pathnames));
    assertThat(policyViolation.getConstraintFactsJson().length(), greaterThan(0));
  }

  private void assertPolicyEvaluation(final PolicyEvaluation policyEvaluation,
                                      final String stageId,
                                      final boolean reEvaluation,
                                      final boolean monitoring,
                                      final String applicationId,
                                      final String scanId,
                                      final long time,
                                      final boolean isForObsoleteScan)
  {
    assertThat(policyEvaluation.getStageTypeId(), is(stageId));
    assertThat(policyEvaluation.isReevaluation(), is(reEvaluation));
    assertThat(policyEvaluation.isForMonitoring(), is(monitoring));
    assertThat(policyEvaluation.getApplicationId(), is(applicationId));
    assertThat(policyEvaluation.getScanId(), is(scanId));
    assertThat(policyEvaluation.getTime(), is(new Date(time)));
    assertThat(policyEvaluation.isForObsoleteScan(), is(isForObsoleteScan));
  }

  private void fixReportEntryLastModified(String appId, String scanId, String fileName, long time) {
    File reportFile = PolicyEvaluationMigrator.getReport(insightWork, appId, scanId);
    File reportEntryFile = Report.getCacheFile(reportFile, fileName);
    reportEntryFile.setLastModified(time);
  }
}
