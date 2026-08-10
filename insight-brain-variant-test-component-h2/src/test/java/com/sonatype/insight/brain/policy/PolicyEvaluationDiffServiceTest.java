/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.82.0
 */
@ComponentH2Test
public class PolicyEvaluationDiffServiceTest
    extends AbstractComponentH2Test
{
  private static final String FROM_SCAN_ID = "fromScanId";

  private static final String TO_SCAN_ID = "toScanId";

  @Inject
  private PolicyEvaluationDiffService policyEvaluationDiffService;

  @Inject
  private InsightWork insightWork;

  private Application app;

  @Rule
  public LogOutput logOutput =
      new LogOutput(1, PolicyEvaluationDiffServiceTest.class, PolicyEvaluationDiffService.class);

  @BeforeEach
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testCreatePolicyViolationDiff() throws URISyntaxException, IOException {
    // setup reports
    createReportFile(app.getId(), FROM_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/from-report", tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/to-report", tempDir),
        insightWork);

    // setup evaluations
    PolicyEvaluation from = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    PolicyEvaluation to = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);

    // create diff
    Optional<PolicyViolationDiff<PolicyViolation>> diffOptional =
        policyEvaluationDiffService.createPolicyViolationDiff(from, to);

    // assert added, same and cleared are correct
    assertThat(diffOptional).isNotEmpty();
    assertThat(diffOptional.get().getSame()).isNotNull();
    assertThat(diffOptional.get().getAppeared()).isNotNull();
    assertThat(diffOptional.get().getCleared()).isNotNull();
    assertThat(diffOptional.get().getSame()).hasSize(3);
    assertThat(diffOptional.get().getSame().values()).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder("same_1", "same_2", "same_3");
    assertThat(diffOptional.get().getAppeared()).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder("appeared_1", "appeared_2", "appeared_3", "appeared_4");
    assertThat(diffOptional.get().getAppeared()).hasSize(4);
    assertThat(diffOptional.get().getCleared()).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder("cleared_1", "cleared_2", "cleared_3", "cleared_4");
    assertThat(diffOptional.get().getCleared()).hasSize(4);
  }

  @Test
  public void testCreatePolicyViolationDiffByComponents() throws Exception {
    testCreatePolicyViolationDiffByComponents(0);
  }

  @Test
  public void testCreatePolicyViolationDiffByComponents_minimumThreatLevel() throws Exception {
    testCreatePolicyViolationDiffByComponents(2);
  }

  private void testCreatePolicyViolationDiffByComponents(
      int minimumThreatLevel) throws Exception
  {
    // setup reports
    createReportFile(app.getId(), FROM_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/from-report1", tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/to-report", tempDir),
        insightWork);

    // setup evaluations
    PolicyEvaluation from = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    PolicyEvaluation to = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);

    // create diff
    Optional<PolicyViolationDiff<PolicyViolation>> diffOptional =
        policyEvaluationDiffService.createPolicyViolationDiffByComponents(from, to, minimumThreatLevel);

    // assert added and cleared are correct
    assertThat(diffOptional).isNotEmpty();
    assertThat(diffOptional.get().getSame()).isNotNull();
    assertThat(diffOptional.get().getAppeared()).isNotNull();
    assertThat(diffOptional.get().getCleared()).isNotNull();
    assertThat(diffOptional.get().getAppeared()).extracting(PolicyViolation::getId)
        .containsExactlyInAnyOrder("appeared_1", "appeared_2", "same_3", "appeared_3", "appeared_4");

    if (minimumThreatLevel > 0) {
      assertThat(diffOptional.get().getCleared()).extracting(PolicyViolation::getId)
          .containsExactlyInAnyOrder("cleared_1", "cleared_2", "cleared_3"); // "cleared_4" is filtered out
      assertThat(diffOptional.get().getCleared()).hasSize(3); // "cleared_4" is filtered out
    }
    else {
      assertThat(diffOptional.get().getCleared()).extracting(PolicyViolation::getId)
          .containsExactlyInAnyOrder("cleared_1", "cleared_2", "cleared_3", "cleared_4");
      assertThat(diffOptional.get().getCleared()).hasSize(4);
    }
  }

  @Test
  public void testCreatePolicyViolationDiff_FromReportDoesNotExist() {
    // setup evaluations
    PolicyEvaluation from = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    PolicyEvaluation to = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);

    // create diff
    Optional<PolicyViolationDiff<PolicyViolation>> diffOptional =
        policyEvaluationDiffService.createPolicyViolationDiff(from, to);

    // assert empty optional
    assertThat(diffOptional).isEmpty();
    assertLogDebug(String.format(
        "Could not find report file for 'from' scan report with commit %s, " +
            "policy evaluation id %s and application id %s",
        from.getCommitHash(), from.getId(), from.getOwnerId()));
  }

  @Test
  public void testCreatePolicyViolationDiff_ToReportDoesNotExist() throws URISyntaxException, IOException {
    // setup from report
    createReportFile(app.getId(), FROM_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/from-report", tempDir),
        insightWork);

    // setup evaluations
    PolicyEvaluation from = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    PolicyEvaluation to = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);

    // create diff
    Optional<PolicyViolationDiff<PolicyViolation>> diffOptional =
        policyEvaluationDiffService.createPolicyViolationDiff(from, to);

    // assert empty optional
    assertThat(diffOptional).isEmpty();
    assertLogDebug(String.format(
        "Could not find report file for 'to' scan report with commit %s, " +
            "policy evaluation id %s and application id %s",
        to.getCommitHash(), to.getId(), to.getOwnerId()));
  }

  @Test
  public void testCreatePolicyViolationDiff_FromReportMissingAlerts() throws URISyntaxException, IOException {
    // setup reports (no policy alerts for from)
    createReportFile(app.getId(), FROM_SCAN_ID,
        zipReportDir("/PolicyEvaluationDiffServiceTest/report-missing-policyalerts", tempDir), insightWork);
    createReportFile(app.getId(), TO_SCAN_ID,
        zipReportDir("/PolicyEvaluationDiffServiceTest/to-report", tempDir), insightWork);

    // setup evaluations
    PolicyEvaluation from = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    PolicyEvaluation to = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);

    // create diff
    Optional<PolicyViolationDiff<PolicyViolation>> diffOptional =
        policyEvaluationDiffService.createPolicyViolationDiff(from, to);

    // assert empty optional
    assertThat(diffOptional).isEmpty();
    assertLogDebug(String.format(
        "Could not find policy alerts for 'from' scan report with commit %s, " +
            "policy evaluation id %s, application id %s and scan report",
        from.getCommitHash(), from.getId(), from.getOwnerId()));
  }

  @Test
  public void testCreatePolicyViolationDiff_ToReportMissingAlerts() throws URISyntaxException, IOException {
    // setup reports (no policy alerts for to)
    createReportFile(app.getId(), FROM_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/from-report", tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID,
        zipReportDir("/PolicyEvaluationDiffServiceTest/report-missing-policyalerts", tempDir), insightWork);

    // setup evaluations
    PolicyEvaluation from = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    PolicyEvaluation to = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);

    // create diff
    Optional<PolicyViolationDiff<PolicyViolation>> diffOptional =
        policyEvaluationDiffService.createPolicyViolationDiff(from, to);

    // assert empty optional
    assertThat(diffOptional).isEmpty();
    assertLogDebug(String.format(
        "Could not find policy alerts for 'to' scan report with commit %s, " +
            "policy evaluation id %s, application id %s and scan report",
        to.getCommitHash(), to.getId(), to.getOwnerId()));
  }

  private void assertLogDebug(String debugLog) {
    assertThat(logOutput).atDebugLevel()
        .contains(debugLog);
  }
}
