/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

import com.google.common.collect.Lists;
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

  private PolicyEvaluation toPolicyEvaluation;

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

  @Test
  public void testPullRequestFeedback() throws Exception {
    //setup test data
    setupTestData();

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, toPolicyEvaluation, diff, app,
            lookup(BaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final Path path =
        Paths.get(getClass().getResource("/PullRequestFeedbackDetailsTest/PullRequestFeedback.md").toURI());
    final String expectedContent = new String(Files.readAllBytes(path));
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
    final ReportEntry bomEntry = reportService.getBomForPolicyEvaluation(toPolicyEvaluation);

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, toPolicyEvaluation, diff, app, lookup(BaseUrl.class).getConfigured());

    //then assert that created contents has singular violation in heading
    final Optional<String> contents = details.getContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).startsWith("## New Nexus IQ Policy Violation found");
  }

  @Test
  public void testPullRequestFeedback_emptyBomData() throws IOException, URISyntaxException {
    //setup test data
    setupTestData("/PullRequestFeedbackDetailsTest/from-report", "/PullRequestFeedbackDetailsTest/to-report-empty-bom");

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, toPolicyEvaluation, diff, app,
            lookup(BaseUrl.class).getConfigured());

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
        new PullRequestFeedbackDetails(null, toPolicyEvaluation, diff, app,
            lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_emptyAddedViolations() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();
    diff.getAppeared().clear();

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(bomEntry, toPolicyEvaluation, diff, app,
            lookup(BaseUrl.class).getConfigured());

    //then assert that created contents is not available
    final Optional<String> contents = details.getContents();
    assertThat(contents).isEmpty();
  }

  @Test
  public void testPullRequestFeedback_nullDiff() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(bomEntry, toPolicyEvaluation, null, app,
            lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_nullApp() throws URISyntaxException, IOException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(bomEntry, toPolicyEvaluation, diff, null,
            lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_invalidAppNoOrg() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();
    app.setOrganizationId("FAKE_ID");

    //when
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(bomEntry, toPolicyEvaluation, diff, app,
            lookup(BaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_nullEvaluation() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(bomEntry, null, diff, app,
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
    componentMap.put("NAME", Lists.newArrayList(diff.getAppeared().get(0)));
    componentMap.put("NAME_EMPTY", Lists.newArrayList());
    // when
    final List<Map<String, Object>> result = PullRequestFeedbackDetails.getComponentFeedbackList(componentMap,
        config.getBaseUrl());

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0)).isNotNull();
    assertThat(result.get(0).get("componentNameAndVersion")).isEqualTo("NAME");
    assertThat(result.get(0).get("highestThreatLevel")).isEqualTo(diff.getAppeared().get(0).getThreatLevel());
    assertThat((ArrayList) (result.get(0).get("policiesViolated"))).hasSize(1);
    assertThat(result.get(1)).isNotNull();
    assertThat(result.get(1).get("componentNameAndVersion")).isEqualTo("NAME_EMPTY");
    assertThat(result.get(1).get("highestThreatLevel")).isEqualTo(0);
    assertThat((ArrayList) (result.get(1).get("policiesViolated"))).isEmpty();
  }

  @Test
  public void testGetHighestThreatLevel_noViolations() {
    assertThat(PullRequestFeedbackDetails.getHighestThreatLevel(Lists.newArrayList())).isEqualTo(0);
  }

  @Test
  public void testGetHighestThreatLevel_singleViolation() throws IOException, URISyntaxException {
    // setup
    setupTestData();
    final int result = PullRequestFeedbackDetails.getHighestThreatLevel(Lists.newArrayList(diff.getAppeared().get(0)));

    assertThat(result).isEqualTo(diff.getAppeared().get(0).getThreatLevel());
  }

  @Test
  public void testGetPoliciesViolatedMap_noPolicies() {
    assertThat(PullRequestFeedbackDetails.getPoliciesViolatedMap(Lists.newArrayList(), config.getBaseUrl())).isEmpty();
  }

  @Test
  public void testGetPoliciesViolatedMap_allSamePolicyId() throws IOException, URISyntaxException {
    // setup
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails.getPoliciesViolatedMap(
        diff.getAppeared().stream().peek(policyViolation -> policyViolation.setPolicyId("1"))
            .collect(Collectors.toList()), config.getBaseUrl());

    assertThat(result).hasSize(1);
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_NoViolations() {
    assertThat(PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Lists.newArrayList(), config.getBaseUrl()))
        .isEmpty();
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_SingleViolation() throws IOException, URISyntaxException {
    // setup
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Lists.newArrayList(diff.getAppeared().get(0)), config.getBaseUrl());

    assertThat(result).hasSize(1);
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_MultipleViolationsNoConstraints()
      throws IOException, URISyntaxException
  {
    // setup
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(diff.getAppeared().stream().peek(policyViolation -> {
          policyViolation.setPolicyId("1");
          policyViolation.setConstraintFacts(Lists.newArrayList(policyViolation.getConstraintFacts()));
          policyViolation.getConstraintFacts().clear();
        }).collect(Collectors.toList()), config.getBaseUrl());

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_MultipleViolationsWithConstraints()
      throws IOException, URISyntaxException
  {
    // setup
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
    // setup
    setupTestData();
    diff.getAppeared().get(0).setConstraintFacts(Lists.newArrayList(diff.getAppeared().get(0).getConstraintFacts()));
    diff.getAppeared().get(0).getConstraintFacts().clear();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Lists.newArrayList(diff.getAppeared().get(0)), config.getBaseUrl());

    assertThat(result).isEmpty();
  }

  private String removeDateFromOutput(final String value) {
    return value.replaceAll("\\*\\*Date\\*\\*:\\ .*", "");
  }

  private void setupTestData() throws IOException, URISyntaxException {
    setupTestData("/PullRequestFeedbackDetailsTest/from-report", "/PullRequestFeedbackDetailsTest/to-report");
  }

  private void setupTestData(final String fromReportLocation, final String toReportLocation)
      throws IOException, URISyntaxException
  {
    //setup reports
    createReportFile(app.getId(), FROM_SCAN_ID, zipReportDir(fromReportLocation, tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir(toReportLocation, tempDir),
        insightWork);

    //setup evaluations
    final PolicyEvaluation fromPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID);
    toPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID);

    //setup diff
    diff = policyEvaluationDiffService.createPolicyViolationDiff(fromPolicyEvaluation, toPolicyEvaluation).get();

    //setup bom report entry
    bomEntry = reportService.getBomForPolicyEvaluation(toPolicyEvaluation);
  }
}
