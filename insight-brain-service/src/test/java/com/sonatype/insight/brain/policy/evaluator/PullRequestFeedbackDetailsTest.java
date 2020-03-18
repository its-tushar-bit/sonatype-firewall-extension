/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PullRequestFeedbackDetailsTest
    extends AbstractComponentTest
{
  private static final String FROM_SCAN_ID = "fromScanId";

  private static final String TO_SCAN_ID = "toScanId";

  private static final String ORG_NAME = "TEST ORG";

  private static final String ORG_ID = "TEST_ORG_ID";

  private static final String APP_NAME = "TEST APP";

  private static final String APP_INTERNAL_ID = "TEST_APP_INTERNAL_ID";

  private static final String APP_PUBLIC_ID = "TEST_APP_PUBLIC_ID";

  private PolicyEvaluation featureBranchPolicyEvaluation;

  private PolicyEvaluation defaultBranchPolicyEvaluation;

  private PolicyViolationDiff<PolicyViolation> diff;

  private ReportEntry bomEntry;

  @Inject
  private PolicyEvaluationDiffService policyEvaluationDiffService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private ReportService reportService;

  @Inject
  private InsightConfig config;

  private Application app;

  @Before
  public void before() {
    config.setBaseUrl("http://localhost:1122");
    tempEntity.newOrganizationWithSpecificId(ORG_ID, ORG_NAME);
    app = tempEntity.newApplicationWithSpecificId(APP_INTERNAL_ID, APP_NAME, APP_PUBLIC_ID, ORG_ID);
  }

  private String readResource(String resourceName) throws Exception {
    final Path path = Paths.get(getClass().getResource("/PullRequestFeedbackDetailsTest/" + resourceName).toURI());
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  @Test
  public void testPullRequestFeedback_addedOnly() throws Exception {
    //setup test data
    setupTestData();

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff,
            app, lookup(BaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Added.md");
    final Optional<String> contents = details.getContents();
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }

  @Test
  public void testPullRequestFeedback_clearedOnly() throws Exception {
    //setup test data
    setupTestData("/PullRequestFeedbackDetailsTest/to-report", "/PullRequestFeedbackDetailsTest/from-report");

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff,
            app,
            lookup(BaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Cleared.md");
    final Optional<String> contents = details.getContents();
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }

  @Test
  public void testPullRequestFeedback_addedAndCleared() throws Exception {
    //setup test data
    setupTestData();
    diff.getCleared().addAll(diff.getAppeared());

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff,
            app,
            lookup(BaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_AddedAndCleared.md");
    final Optional<String> contents = details.getContents();
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }

  @Test
  public void testPullRequestFeedback_noAddedOrCleared() throws Exception {
    //setup test data
    setupTestData();
    diff.getAppeared().clear();

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff,
            app,
            lookup(BaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_NoAddedOrCleared.md");
    final Optional<String> contents = details.getContents();
    assertThat(contents).isNotEmpty();
    assertThat(removeDateFromOutput(contents.get())).isEqualTo(removeDateFromOutput(expectedContent));
  }

  @Test
  public void testPullRequestFeedback_singlePolicyViolationPlurality() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getAppeared().add(first);

    //setup bom report entry
    final ReportEntry bomEntry = reportService.getBomForPolicyEvaluation(featureBranchPolicyEvaluation);

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff,
            app, lookup(BaseUrl.class).getConfigured());

    //then assert that created contents has singular violation in heading
    final Optional<String> contents = details.getContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).startsWith("###  \uD83E\uDD14 Nexus IQ found a policy violation");
  }

  @Test
  public void testPullRequestFeedback_singleClearedViolationPlurality() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getCleared().add(first);

    //setup bom report entry
    final ReportEntry bomEntry = reportService.getBomForPolicyEvaluation(featureBranchPolicyEvaluation);

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff,
            app, lookup(BaseUrl.class).getConfigured());

    //then assert that created contents has singular violation in heading
    final Optional<String> contents = details.getContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get())
        .contains("#### \uD83D\uDE03\uD83C\uDFC6 Nice work! Nexus IQ determined that you fixed an outstanding policy violation");
  }

  @Test
  public void testPullRequestFeedback_emptyBomData() throws IOException, URISyntaxException {
    //setup test data
    setupTestData("/PullRequestFeedbackDetailsTest/from-report", "/PullRequestFeedbackDetailsTest/to-report-empty-bom");

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff,
            app, lookup(BaseUrl.class).getConfigured());

    //then assert that created contents is not available
    final Optional<String> contents = details.getContents();
    assertThat(contents).isEmpty();
  }

  @Test
  public void testPullRequestFeedback_nullBom() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(null, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff, app,
            lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_nullDiff() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, null,
            app, lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_nullApp() throws URISyntaxException, IOException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff,
            null, lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_invalidAppNoOrg() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();
    app.setOrganizationId("FAKE_ID");

    //when
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation, diff,
            app, lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_nullFeatureBranchEvaluation() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(bomEntry, null, defaultBranchPolicyEvaluation, diff, app,
            lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_nullDefaultBranchEvaluation() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(bomEntry, featureBranchPolicyEvaluation, null, diff, app,
            lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testGetComponentFeedbackList_noComponents() {
    // when
    final List<Map<String, Object>> result = PullRequestFeedbackDetails.getComponentFeedbackList(new HashMap<>(),
        config.getBaseUrl());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetComponentFeedbackList_componentWithoutViolations() throws IOException, URISyntaxException {
    // setup
    setupTestData();
    final Map<String, List<PolicyViolation>> componentMap = new HashMap<>();
    componentMap.put("NAME", Collections.singletonList(diff.getAppeared().get(0)));
    componentMap.put("NAME_EMPTY", Collections.emptyList());
    // when
    final List<Map<String, Object>> result = PullRequestFeedbackDetails.getComponentFeedbackList(componentMap,
        config.getBaseUrl());

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0)).isNotNull();
    assertThat(result.get(0).get("componentNameAndVersion")).isEqualTo("NAME");
    assertThat(result.get(0).get("highestThreatLevel")).isEqualTo(diff.getAppeared().get(0).getThreatLevel());
    assertThat((List<?>) (result.get(0).get("policiesViolated"))).hasSize(1);
    assertThat(result.get(1)).isNotNull();
    assertThat(result.get(1).get("componentNameAndVersion")).isEqualTo("NAME_EMPTY");
    assertThat(result.get(1).get("highestThreatLevel")).isEqualTo(0);
    assertThat((List<?>) (result.get(1).get("policiesViolated"))).isEmpty();
  }

  @Test
  public void testGetHighestThreatLevel_noViolations() {
    assertThat(PullRequestFeedbackDetails.getHighestThreatLevel(Collections.emptyList())).isEqualTo(0);
  }

  @Test
  public void testGetHighestThreatLevel_singleViolation() throws IOException, URISyntaxException {
    setupTestData();
    final int result =
        PullRequestFeedbackDetails.getHighestThreatLevel(Collections.singletonList(diff.getAppeared().get(0)));

    assertThat(result).isEqualTo(diff.getAppeared().get(0).getThreatLevel());
  }

  @Test
  public void testGetPoliciesViolatedMap_noPolicies() {
    assertThat(PullRequestFeedbackDetails.getPoliciesViolatedMap(Collections.emptyList(), config.getBaseUrl()))
        .isEmpty();
  }

  @Test
  public void testGetPoliciesViolatedMap_allSamePolicyId() throws IOException, URISyntaxException {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails.getPoliciesViolatedMap(
        diff.getAppeared().stream().peek(policyViolation -> policyViolation.setPolicyId("1"))
            .collect(Collectors.toList()), config.getBaseUrl());

    assertThat(result).hasSize(1);
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_NoViolations() {
    assertThat(PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Collections.emptyList(), config.getBaseUrl()))
        .isEmpty();
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_SingleViolation() throws IOException, URISyntaxException {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Collections.singletonList(diff.getAppeared().get(0)),
            config.getBaseUrl());

    assertThat(result).hasSize(1);
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_MultipleViolationsNoConstraints()
      throws IOException, URISyntaxException
  {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(diff.getAppeared().stream().peek(policyViolation -> {
          policyViolation.setPolicyId("1");
          policyViolation.setConstraintFacts(new ArrayList<>(policyViolation.getConstraintFacts()));
          policyViolation.getConstraintFacts().clear();
        }).collect(Collectors.toList()), config.getBaseUrl());

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_MultipleViolationsWithConstraints()
      throws IOException, URISyntaxException
  {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(diff.getAppeared().stream().peek(policyViolation -> {
          policyViolation.setPolicyId("1");
        }).collect(Collectors.toList()), config.getBaseUrl());

    assertThat(result).hasSize(6);
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_SingleViolationWithoutConstraints()
      throws IOException, URISyntaxException
  {
    setupTestData();
    diff.getAppeared().get(0).setConstraintFacts(new ArrayList<>(diff.getAppeared().get(0).getConstraintFacts()));
    diff.getAppeared().get(0).getConstraintFacts().clear();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Collections.singletonList(diff.getAppeared().get(0)),
            config.getBaseUrl());

    assertThat(result).isEmpty();
  }

  private String removeDateFromOutput(final String value) {
    return value.replaceAll("\\*\\*Date\\*\\*:\\ .*", "");
  }

  private void setupTestData() throws IOException, URISyntaxException {
    setupTestData("/PullRequestFeedbackDetailsTest/from-report", "/PullRequestFeedbackDetailsTest/to-report");
  }

  private void setupTestData(final String defaultBranchReportLocation, final String featureBranchReportLocation)
      throws IOException, URISyntaxException
  {
    //setup reports
    createReportFile(app.getId(), FROM_SCAN_ID, zipReportDir(defaultBranchReportLocation, tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir(featureBranchReportLocation, tempDir),
        insightWork);

    //setup evaluations
    defaultBranchPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    featureBranchPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);

    //setup diff
    diff = policyEvaluationDiffService.createPolicyViolationDiff(defaultBranchPolicyEvaluation,
        featureBranchPolicyEvaluation).get();

    //setup bom report entry
    bomEntry = reportService.getBomForPolicyEvaluation(featureBranchPolicyEvaluation);
  }
}
