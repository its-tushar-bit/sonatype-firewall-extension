/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.git.PullRequestLineCommentDTO;
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
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.dto.DiffPosition;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightReportOutcome;
import com.sonatype.nexus.scm.bitbucket.BitbucketLinkDataParameter;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.policy.evaluator.PullRequestDetailsBase.DATE_TIME_FORMATTER;
import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PullRequestCodeInsightsDetailsTest
    extends AbstractComponentTest
{
  private static final String FROM_SCAN_ID = "fromScanId";

  private static final String TO_SCAN_ID = "toScanId";

  private static final String ORG_NAME = "TEST ORG";

  private static final String ORG_ID = "TEST_ORG_ID";

  private static final String APP_NAME = "TEST APP";

  private static final String APP_INTERNAL_ID = "TEST_APP_INTERNAL_ID";

  private static final String APP_PUBLIC_ID = "TEST_APP_PUBLIC_ID";

  private static final URI EXPECTED_REPORT_URI = URI
      .create("http://localhost:1122/ui/links/application/" + APP_PUBLIC_ID + "/report/" + TO_SCAN_ID);

  private PolicyEvaluation featureBranchPolicyEvaluation;

  private PolicyEvaluation defaultBranchPolicyEvaluation;

  private PolicyViolationDiff<PolicyViolation> diff;

  private Map<ComponentIdentifier, String> remediationVersionMap;

  private List<PullRequestLineCommentDTO> pullRequestLineComments;

  private GitRepositoryInfo bitbucketGitRepositoryInfo;

  private ReportEntry bomEntry;

  private String bomTimestamp;

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
    PullRequestCodeInsightsDetails.clock = Clock
        .fixed(Instant.parse("2019-11-26T18:15:30Z"), ZoneId.of("America/Los_Angeles"));
  }

  @Test
  public void testPullRequestCodeInsights_addedOnly() throws Exception {
    //setup test data
    setupTestData();

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured());

    //then assert that created contents match expected
    assertThat(details.getReportDetails())
        .isEqualTo("On " + bomTimestamp + ", Nexus IQ found 39 new policy violations affecting 4 components.");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.FAIL);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(32, 3, 4));
  }

  @Test
  public void testPullRequestCodeInsights_clearedOnly() throws Exception {
    //setup test data (reversed)
    setupTestData("/PullRequestCodeInsightsDetailsTest/to-report", "/PullRequestCodeInsightsDetailsTest/from-report");

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured());

    //then assert that created contents match expected
    assertThat(details.getReportDetails()).isEqualTo("Nexus IQ found no new policy violations on " + bomTimestamp +
        ". 39 outstanding policy violations fixed, affecting 4 components");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.PASS);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(0, 0, 0));
  }

  @Test
  public void testPullRequestCodeInsights_addedAndCleared() throws Exception {
    //setup test data
    setupTestData();
    // create cleared policy violation that does not exist in the bom file
    PolicyViolation existingViolation = diff.getAppeared().get(0);
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setHash("12345678abcd12345678");
    policyViolation.setComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("org.group.fixed", "fixed-artifact", "1.0"));
    policyViolation.setConstraintFacts(existingViolation.getConstraintFacts());
    policyViolation.setPolicyId(existingViolation.getPolicyId());
    policyViolation.setPolicyName(existingViolation.getPolicyName());
    diff.getCleared().add(policyViolation);

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured());

    //then assert that created contents match expected
    assertThat(details.getReportDetails()).isEqualTo("On " + bomTimestamp + ", Nexus IQ found 39 new policy " +
        "violations affecting 4 components. 1 outstanding policy violation fixed, affecting 1 component");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.FAIL);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(32, 3, 4));
  }

  @Test
  public void testPullRequestCodeInsights_noAddedOrCleared() throws Exception {
    //setup test data
    setupTestData();
    diff.getAppeared().clear();

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured());

    //then assert that created contents match expected
    assertThat(details.getReportDetails())
        .isEqualTo("Nexus IQ found no new policy violations on " + bomTimestamp + ".");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.PASS);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(0, 0, 0));
  }

  @Test
  public void testPullRequestCodeInsights_singlePolicyViolationPlurality() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getAppeared().add(first);

    //setup bom report entry
    final ReportEntry bomEntry = reportService.getBomForPolicyEvaluation(featureBranchPolicyEvaluation);

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured());

    //then assert that created contents has singular violation in heading
    assertThat(details.getReportDetails())
        .startsWith("On " + bomTimestamp + ", Nexus IQ found 1 new policy violation affecting 1 component.");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.FAIL);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(1, 0, 0));
  }

  @Test
  public void testPullRequestCodeInsights_singleClearedViolationPlurality() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getCleared().add(first);

    //setup bom report entry
    final ReportEntry bomEntry = reportService.getBomForPolicyEvaluation(featureBranchPolicyEvaluation);

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured());

    //then assert that created contents has singular violation in heading
    assertThat(details.getReportDetails()).contains("Nexus IQ found no new policy violations on " + bomTimestamp +
        ". 1 outstanding policy violation fixed, affecting 1 component");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.PASS);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(0, 0, 0));
  }

  @Test
  public void testPullRequestCodeInsights_emptyBomData() throws IOException, URISyntaxException {
    //setup test data
    setupTestData("/PullRequestCodeInsightsDetailsTest/from-report",
        "/PullRequestCodeInsightsDetailsTest/to-report-empty-bom");

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured());

    //then assert that created contents is not available
    String contents = details.getReportDetails();
    assertThat(contents).isEqualTo("Nexus IQ found no new policy violations on " + bomTimestamp + ".");
  }

  @Test
  public void testPullRequestCodeInsights_nullBom() throws IOException, URISyntaxException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.repositoryUrl, app, null, featureBranchPolicyEvaluation, diff,
            lookup(BaseUrl.class).getConfigured()))
        .withMessage("bomReportEntry is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullDiff() throws IOException, URISyntaxException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, featureBranchPolicyEvaluation, null,
            lookup(BaseUrl.class).getConfigured()))
        .withMessage("policyViolationDiff is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullApp() throws URISyntaxException, IOException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.repositoryUrl, null, bomEntry, featureBranchPolicyEvaluation, diff,
            lookup(BaseUrl.class).getConfigured()))
        .withMessage("app is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullRepoUrl() throws URISyntaxException, IOException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            null, app, bomEntry, featureBranchPolicyEvaluation, diff,
            lookup(BaseUrl.class).getConfigured()))
        .withMessage("repositoryUrl is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullFeatureBranchEvaluation() throws IOException, URISyntaxException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, null, diff,
            lookup(BaseUrl.class).getConfigured()))
        .withMessage("featureBranchEvaluation is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullBaseUrl() throws IOException, URISyntaxException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.repositoryUrl, app, bomEntry, featureBranchPolicyEvaluation, diff,
            null))
        .withMessage("baseUrl is required and cannot be null");
  }

  private void setupTestData() throws IOException, URISyntaxException {
    setupTestData("/PullRequestCodeInsightsDetailsTest/from-report", "/PullRequestCodeInsightsDetailsTest/to-report");
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
    featureBranchPolicyEvaluation.setTime(new GregorianCalendar(2020, 5, 21, 9, 15, 32).getTime());

    //setup diff
    diff = policyEvaluationDiffService.createPolicyViolationDiff(defaultBranchPolicyEvaluation,
        featureBranchPolicyEvaluation).get();

    //setup remediationVersionMap
    remediationVersionMap = new HashMap<>();
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.190", "", "jar");
    remediationVersionMap.put(ci, "1.4.200");

    //setup pullRequestLineComments
    pullRequestLineComments = new ArrayList<>();
    PullRequestLineCommentDTO lineCommentDTO = new PullRequestLineCommentDTO(ci, new DiffPosition("path", 1, 1, 1));
    lineCommentDTO.setScmId(12345);
    pullRequestLineComments.add(lineCommentDTO);

    //setup gitRepositoryInfo
    bitbucketGitRepositoryInfo =
        new GitRepositoryInfo("https://bitbucket.com/scm/sonatype/enhanced-commit-information", "user", "token",
            SourceControlProvider.BITBUCKET, "master", true, true);

    //setup bom report entry
    bomEntry = reportService.getBomForPolicyEvaluation(featureBranchPolicyEvaluation);

    bomTimestamp = DATE_TIME_FORMATTER
        .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(bomEntry.time), ZoneId.systemDefault()));
  }

  private Map<String, Object> expectedReportData(
      final Object criticalCount,
      final Object moderateCount,
      final Object severeCount)
  {
    return ImmutableMap.<String, Object>builder()
        .put("Critical", criticalCount)
        .put("Details",
            new BitbucketLinkDataParameter(bitbucketGitRepositoryInfo.repositoryUrl, "Application Report",
                EXPECTED_REPORT_URI))
        .put("Moderate", moderateCount)
        .put("Organization", "TEST ORG")
        .put("Severe", severeCount)
        .put("Stage", "release")
        .build();
  }
}
