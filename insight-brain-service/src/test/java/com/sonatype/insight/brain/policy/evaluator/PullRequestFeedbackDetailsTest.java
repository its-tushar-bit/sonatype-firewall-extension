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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.git.PullRequestLineCommentDTO;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.SourceControlComponentDetails;
import com.sonatype.insight.brain.git.SourceControlComponentDetails.ComponentInfo;
import com.sonatype.insight.brain.git.SourceControlComponentLoader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.DefaultBaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.DiffPosition;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.git.PullRequestCommentingService.MINIMUM_THREAT_LEVEL;
import static com.sonatype.insight.brain.policy.evaluator.PullRequestDetailsBaseTest.CONVERT_URLS;
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

  private Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap;

  private List<PullRequestLineCommentDTO> pullRequestLineComments;

  private GitRepositoryInfo githubGitRepositoryInfo;

  private GitRepositoryInfo gitlabGitRepositoryInfo;

  private GitRepositoryInfo bitbucketGitRepositoryInfo;

  private GitRepositoryInfo azureGitRepositoryInfo;

  private GitRepositoryInfo azureOnPremGitRepositoryInfo;

  private int pullRequestNumber = 10;

  private SourceControlComponentDetails componentDetails;

  @Inject
  private PolicyEvaluationDiffService policyEvaluationDiffService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private SourceControlComponentLoader sourceControlComponentLoader;

  @Inject
  private InsightConfig config;

  private Application app;

  private TimeZone initialTimezone;

  @Before
  public void before() {
    config.setBaseUrl("http://localhost:1122");
    tempEntity.newOrganizationWithSpecificId(ORG_ID, ORG_NAME);
    app = tempEntity.newApplicationWithSpecificId(APP_INTERNAL_ID, APP_NAME, APP_PUBLIC_ID, ORG_ID);
    initialTimezone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @After
  public void after() {
    TimeZone.setDefault(initialTimezone);
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
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Added.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedOnly_noEmbeddedHtml() throws Exception {
    //setup test data
    setupTestData();

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Added_noEmbeddedHtml.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_AzureCloud_richHtml() throws Exception {
    //setup test data
    setupTestData();

    //when we get details for azure cloud
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, azureGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents follow the HTML template
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).contains("<details>");
  }

  @Test
  public void testPullRequestFeedback_AzureOnPrem_noEmbeddedHtml() throws Exception {
    //setup test data
    setupTestData();

    //when get details for azure on prem
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, azureOnPremGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents are following the no-html template
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).doesNotContain("<details>");
  }

  @Test
  public void testPullRequestFeedback_addedOnly_GitLab() throws Exception {
    //setup test data
    setupTestData();

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, gitlabGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Added_GitLab.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_clearedOnly() throws Exception {
    //setup test data
    setupTestData("/PullRequestFeedbackDetailsTest/to-report", "/PullRequestFeedbackDetailsTest/from-report");

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff, remediationVersionMap, pullRequestLineComments,
            githubGitRepositoryInfo, pullRequestNumber, app, lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Cleared.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_clearedOnly_noEmbeddedHtml() throws Exception {
    //setup test data
    setupTestData("/PullRequestFeedbackDetailsTest/to-report", "/PullRequestFeedbackDetailsTest/from-report");

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Cleared_noEmbeddedHtml.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_clearedOnly_GitLab() throws Exception {
    //setup test data
    setupTestData("/PullRequestFeedbackDetailsTest/to-report", "/PullRequestFeedbackDetailsTest/from-report");

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, gitlabGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_Cleared_GitLab.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedAndCleared() throws Exception {
    //setup test data
    setupTestData();
    // create cleared policy violation that does not exist in the bom file
    PolicyViolation policyViolation = createClearedPolicyViolation();
    diff.getCleared().add(policyViolation);

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_AddedAndCleared.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedAndCleared_noEmbeddedHtml() throws Exception {
    //setup test data
    setupTestData();
    diff.getCleared().addAll(diff.getAppeared());

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_AddedAndCleared_noEmbeddedHtml.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_addedAndCleared_GitLab() throws Exception {
    //setup test data
    setupTestData();
    // create cleared policy violation that does not exist in the bom file
    PolicyViolation policyViolation = createClearedPolicyViolation();
    diff.getCleared().add(policyViolation);

    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, gitlabGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_AddedAndCleared_GitLab.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_noAddedOrCleared() throws Exception {
    //setup test data
    setupTestData();
    diff.getAppeared().clear();

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_NoAddedOrCleared.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_noAddedOrCleared_noEmbeddedHtml() throws Exception {
    //setup test data
    setupTestData();
    diff.getAppeared().clear();

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, bitbucketGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_NoAddedOrCleared_noEmbeddedHtml.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_noAddedOrCleared_GitLab() throws Exception {
    //setup test data
    setupTestData();
    diff.getAppeared().clear();

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, gitlabGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents match expected
    final String expectedContent = readResource("PullRequestFeedback_NoAddedOrCleared_GitLab.md");
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).isEqualTo(expectedContent);
  }

  @Test
  public void testPullRequestFeedback_singlePolicyViolationPlurality() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getAppeared().add(first);

    //setup source control component details
    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents has singular violation in heading
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get()).startsWith("### :thinking: Nexus IQ found a policy violation");
  }

  @Test
  public void testPullRequestFeedback_singleClearedViolationPlurality() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getCleared().add(first);

    //setup source control component details
    SourceControlComponentDetails sourceControlComponentDetails =
        sourceControlComponentLoader.getSourceControlComponentDetails(
            featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(sourceControlComponentDetails, diff.getCleared());

    //when
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(sourceControlComponentDetails, featureBranchPolicyEvaluation,
            defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    //then assert that created contents has singular violation in heading
    final Optional<String> contents = details.renderTemplateAndGetContents();
    assertThat(contents).isNotEmpty();
    assertThat(contents.get())
        .contains("### :sunglasses: Nexus IQ determined that you fixed an outstanding policy violation:");
  }

  @Test
  public void testPullRequestFeedback_nullBom() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(null, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_nullDiff() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            null, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_nullApp() throws URISyntaxException, IOException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, null,
            lookup(DefaultBaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_invalidAppNoOrg() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();
    app.setOrganizationId("FAKE_ID");

    //when
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured()).renderTemplateAndGetContents());
  }

  @Test
  public void testPullRequestFeedback_nullFeatureBranchEvaluation() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(componentDetails, null, defaultBranchPolicyEvaluation, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured()));
  }

  @Test
  public void testPullRequestFeedback_nullDefaultBranchEvaluation() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    //when
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, null, diff,
            remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured()));
  }

  @Test
  public void testGetComponentFeedbackList_noComponents() throws IOException, URISyntaxException {
    // given
    setupTestData();
    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    // when
    final List<Map<String, Object>> result = details.getNewComponentFeedbackList(new HashMap<>(),
        remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber,
        config.getBaseUrl());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetComponentFeedbackList_componentWithoutViolations() throws IOException, URISyntaxException {
    // setup
    setupTestData();

    final Map<String, List<PolicyViolation>> componentMap = new HashMap<>();
    componentMap.put("hash-1", Collections.singletonList(diff.getAppeared().get(0)));
    componentMap.put("hash-2", Collections.emptyList());

    SourceControlComponentDetails componentDetails = new SourceControlComponentDetails();
    ComponentInfo componentInfo = new ComponentInfo("NAME", true);
    componentDetails.getHashToComponentInfoMap().put("hash-1", componentInfo);
    componentInfo = new ComponentInfo("NAME_EMPTY", true);
    componentDetails.getHashToComponentInfoMap().put("hash-2", componentInfo);

    final PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, featureBranchPolicyEvaluation, defaultBranchPolicyEvaluation,
            diff, remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber, app,
            lookup(DefaultBaseUrl.class).getConfigured());

    // when
    final List<Map<String, Object>> result = details.getNewComponentFeedbackList(componentMap,
        remediationVersionMap, pullRequestLineComments, githubGitRepositoryInfo, pullRequestNumber,
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
    assertThat(
        PullRequestFeedbackDetails.getPoliciesViolatedMap(Collections.emptyList(), config.getBaseUrl(), CONVERT_URLS))
        .isEmpty();
  }

  @Test
  public void testGetPoliciesViolatedMap_allSamePolicyId() throws IOException, URISyntaxException {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails.getPoliciesViolatedMap(
        diff.getAppeared().stream().peek(policyViolation -> policyViolation.setPolicyId("1"))
            .collect(Collectors.toList()), config.getBaseUrl(), CONVERT_URLS);

    assertThat(result).hasSize(1);
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_NoViolations() {
    assertThat(PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Collections.emptyList(), config.getBaseUrl(), CONVERT_URLS))
        .isEmpty();
  }

  @Test
  public void testGetConstraintsForPolicyViolationsPerPolicy_SingleViolation() throws IOException, URISyntaxException {
    setupTestData();

    final List<Map<String, Object>> result = PullRequestFeedbackDetails
        .getConstraintsForPolicyViolationsPerPolicy(Collections.singletonList(diff.getAppeared().get(0)),
            config.getBaseUrl(), CONVERT_URLS);

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
        }).collect(Collectors.toList()), config.getBaseUrl(), CONVERT_URLS);

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
        }).collect(Collectors.toList()), config.getBaseUrl(), CONVERT_URLS);

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
            config.getBaseUrl(), CONVERT_URLS);

    assertThat(result).isEmpty();
  }

  private PolicyViolation createClearedPolicyViolation() {
    PolicyViolation existingViolation = diff.getAppeared().get(0);
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setHash("12345678abcd12345678");
    policyViolation.setComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("org.group.fixed", "fixed-artifact", "1.0"));
    policyViolation.setConstraintFacts(existingViolation.getConstraintFacts());
    policyViolation.setPolicyId(existingViolation.getPolicyId());
    policyViolation.setPolicyName(existingViolation.getPolicyName());
    return policyViolation;
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
    featureBranchPolicyEvaluation.setTime(new GregorianCalendar(2020, 5, 21, 9, 15, 32).getTime());

    //setup diff
    diff = policyEvaluationDiffService.createPolicyViolationDiff(defaultBranchPolicyEvaluation,
        featureBranchPolicyEvaluation, MINIMUM_THREAT_LEVEL).get();

    //setup remediationVersionMap
    remediationVersionMap = new HashMap<>();
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.190", "", "jar");
    remediationVersionMap
        .put(ci, new RemediationVersionDTO("1.4.200", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, 3));
    //setup it is explicitly set the remediation with dependencies in order to check the variation on the message
    ComponentIdentifier ci2 = ComponentIdentifier
        .createMavenCoordinates("org.springframework.security", "spring-security-web", "4.2.3.RELEASE", "", "jar");
    remediationVersionMap.put(ci2, new RemediationVersionDTO("4.5.0.RELEASE",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES));

    //setup pullRequestLineComments
    pullRequestLineComments = new ArrayList<>();
    PullRequestLineCommentDTO lineCommentDTO = new PullRequestLineCommentDTO(ci, new DiffPosition("path", 1, 0, 1, 1));
    lineCommentDTO.setScmId(12345);
    pullRequestLineComments.add(lineCommentDTO);

    //setup gitRepositoryInfo
    githubGitRepositoryInfo =
        new GitRepositoryInfo("https://github.com/sonatype/enhanced-commit-information", null, null, "token",
            SourceControlProvider.GITHUB, "master", true, true, true, true, false, null);

    gitlabGitRepositoryInfo =
        new GitRepositoryInfo("https://gitlab.com/sonatype/enhanced-commit-information", null, null, "token",
            SourceControlProvider.GITLAB, "master", true, true, true, true, false, null);

    bitbucketGitRepositoryInfo =
        new GitRepositoryInfo("https://bitbucket.com/scm/sonatype/enhanced-commit-information", null, "user", "token",
            SourceControlProvider.BITBUCKET, "master", true, true, true, true, false, null);

    azureGitRepositoryInfo =
        new GitRepositoryInfo("https://dev.azure.com/sonatype/int/_git/enhanced-commit-information", null,
            "user@sonatype.com", "token", SourceControlProvider.AZURE, "main", true, true, true, true, false, null);

    azureOnPremGitRepositoryInfo =
        new GitRepositoryInfo("https://azure-on-prem.com/sonatype/int/_git/enhanced-commit-information", null,
            "user@sonatype.com", "token", SourceControlProvider.AZURE, "main", true, true, true, true, false, null);

    //setup source control component details
    componentDetails = sourceControlComponentLoader.getSourceControlComponentDetails(
        featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());

    // add some dependency info manually
    ComponentInfo componentInfo = componentDetails.getComponentInfo("df71536d44e3b07f0c15");
    ComponentInfo newComponentInfo = new ComponentInfo(componentInfo.getDisplayName(), true);
    componentDetails.getHashToComponentInfoMap().put("df71536d44e3b07f0c15", newComponentInfo);
    componentInfo = componentDetails.getComponentInfo("7a03e737484ca232d714");
    newComponentInfo = new ComponentInfo(componentInfo.getDisplayName(), false);
    componentDetails.getHashToComponentInfoMap().put("7a03e737484ca232d714", newComponentInfo);
  }
}
