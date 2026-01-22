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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.git.PullRequestLineCommentDTO;
import com.sonatype.insight.brain.git.PullRequestLocationDiscoveryService;
import com.sonatype.insight.brain.git.SourceControlComponentDetails;
import com.sonatype.insight.brain.git.SourceControlComponentLoader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.RankedSourceLocation;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.DiffPosition;
import com.sonatype.nexus.scm.api.model.CodeInsightAnnotation;
import com.sonatype.nexus.scm.bitbucket.BitbucketApiClientUtils;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightAnnotationType;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightReportOutcome;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightSeverity;
import com.sonatype.nexus.scm.bitbucket.BitbucketLinkDataParameter;
import com.sonatype.nexus.scm.bitbucket.dto.v1.BitbucketV1CodeInsightAnnotation;
import com.sonatype.nexus.scm.bitbucket.dto.v2.BitbucketV2CodeInsightAnnotation;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.data.Index;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.policy.evaluator.PullRequestDetailsBase.DATE_TIME_FORMATTER;
import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
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

  private PullRequestLocationDiscoveryService pullRequestLocationDiscoveryService;

  private GitRepositoryInfo bitbucketGitRepositoryInfo;

  @Inject
  private PolicyEvaluationDiffService policyEvaluationDiffService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private SourceControlComponentLoader sourceControlComponentLoader;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  private Application app;

  private final String repositoryUrl;

  private LocationDiscoveryResult locationDiscoveryResult;

  private SourceControlComponentDetails componentDetails;

  private String bomTimestamp;

  public PullRequestCodeInsightsDetailsTest(final String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
  }

  @Parameterized.Parameters(name = "repositoryUrl={0}")
  public static Collection<Object[]> data() {
    String bitbucketServerRepoUrl = "http://localhost:7990/scm/test/testing-things.git";
    String bitbucketCloudRepoUrl = "https://bitbucket.org/sonatype/nexus-scm-client-testing.git";
    return Arrays.stream(new String[]{bitbucketCloudRepoUrl, bitbucketServerRepoUrl})
        .map(v -> new Object[]{v})
        .collect(Collectors.toList());
  }

  @Before
  public void before() {
    setBaseUrl("http://localhost:1122");
    tempEntity.newOrganizationWithSpecificId(ORG_ID, ORG_NAME);
    app = tempEntity.newApplicationWithSpecificId(APP_INTERNAL_ID, APP_NAME, APP_PUBLIC_ID, ORG_ID);
    PullRequestCodeInsightsDetails.clock = Clock
        .fixed(Instant.parse("2019-11-26T18:15:30Z"), ZoneId.of("America/Los_Angeles"));
    locationDiscoveryResult = new LocationDiscoveryResult();
    pullRequestLocationDiscoveryService = mock(PullRequestLocationDiscoveryService.class);
  }

  @Test
  public void testPullRequestCodeInsights_addedOnly() throws Exception {
    //setup test data
    setupTestData();
    createTestData_Policies(true);

    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.190", "", "jar");
    RankedSourceLocation sourceLocation1 = new RankedSourceLocation("/pom.xml", 19, "456", 1);
    RankedSourceLocation sourceLocation2 = new RankedSourceLocation("/pom.xml", 30, "456", 2);
    locationDiscoveryResult.getLocationMap().put(ci, Arrays.asList(sourceLocation1, sourceLocation2));

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false);

    //then assert that created contents match expected
    assertThat(details.getReportDetails())
        .isEqualTo("On " + bomTimestamp +
            ", Sonatype Lifecycle found 39 new policy violations affecting 4 components.");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.FAIL);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(32, 3, 4));

    List<CodeInsightAnnotation> annotations = details.getAnnotations();
    assertThat(annotations).hasSize(39);
    // assert a couple of annotations
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.HIGH,
        "10 - Unlikely Test Policy - org.springframework.security : spring-security-web : 4.2.3.RELEASE",
        "Nonsensical Constraint: Found licenses in the 'Liberal' license threat group ('Apache-2.0')",
        0);
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.HIGH,
        "10 - Unlikely Test Policy - com.h2database : h2 : 1.4.190",
        "Nonsensical Constraint: Found 1 security vulnerability: CVE-2018-14335",
        "/pom.xml", 19,
        22);
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.LOW,
        "2 - Component-Unknown - webgoat-server-8.0.0.M1.jar",
        "Unknown 3rd party component: Match state was 'Unknown', Component does not contain proprietary packages",
        34);
  }

  @Test
  public void testPullRequestCodeInsights_addedOnly_OutcomePass() throws Exception {
    //setup test data
    setupTestData();
    createTestData_Policies(false);

    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.190", "", "jar");
    RankedSourceLocation sourceLocation1 = new RankedSourceLocation("/pom.xml", 19, "456", 1);
    RankedSourceLocation sourceLocation2 = new RankedSourceLocation("/pom.xml", 30, "456", 2);
    locationDiscoveryResult.getLocationMap().put(ci, Arrays.asList(sourceLocation1, sourceLocation2));

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false);

    // Pass because the policies do not have FAIL action for RELEASE stage
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.PASS);
  }

  @Test
  public void testPullRequestCodeInsights_addedOnly_SourceLocationUnknown() throws Exception {
    setupTestData();
    createTestData_Policies(false);

    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured(), null, policyDAO, organizationDAO, false);

    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.PASS);
    List<CodeInsightAnnotation> annotations = details.getAnnotations();
    assertThat(annotations).hasSize(39);
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.HIGH,
        "10 - Unlikely Test Policy - org.springframework.security : spring-security-web : 4.2.3.RELEASE",
        "Nonsensical Constraint: Found licenses in the 'Liberal' license threat group ('Apache-2.0')",
        0);
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.HIGH,
        "10 - Unlikely Test Policy - com.h2database : h2 : 1.4.190",
        "Nonsensical Constraint: Found 1 security vulnerability: CVE-2018-14335",
        null, null,
        22);
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.LOW,
        "2 - Component-Unknown - webgoat-server-8.0.0.M1.jar",
        "Unknown 3rd party component: Match state was 'Unknown', Component does not contain proprietary packages",
        34);
  }

  @Test
  public void testPullRequestCodeInsights_clearedOnly() throws Exception {
    //setup test data (reversed)
    setupTestData("/PullRequestCodeInsightsDetailsTest/to-report", "/PullRequestCodeInsightsDetailsTest/from-report");

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false);

    //then assert that created contents match expected
    assertThat(details.getReportDetails()).isEqualTo("Sonatype Lifecycle found no new policy violations on " +
        bomTimestamp + ". 39 outstanding policy violations fixed, affecting 4 components");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.PASS);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(0, 0, 0));
    assertThat(details.getAnnotations()).isEmpty();
  }

  @Test
  public void testPullRequestCodeInsights_addedAndCleared() throws Exception {
    setupTestData();
    createTestData_Policies(true);

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
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(componentDetails, diff.getCleared());

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false);

    //then assert that created contents match expected
    assertThat(details.getReportDetails()).isEqualTo("On " + bomTimestamp +
        ", Sonatype Lifecycle found 39 new policy violations affecting 4 components. " +
        "1 outstanding policy violation fixed, affecting 1 component");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.FAIL);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(32, 3, 4));

    List<CodeInsightAnnotation> annotations = details.getAnnotations();
    assertThat(annotations).hasSize(39);
    // assert a couple of annotations
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.HIGH,
        "10 - Unlikely Test Policy - org.springframework.security : spring-security-web : 4.2.3.RELEASE",
        "Nonsensical Constraint: Found licenses in the 'Liberal' license threat group ('Apache-2.0')",
        0);
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.HIGH,
        "10 - Unlikely Test Policy - com.h2database : h2 : 1.4.190",
        "Nonsensical Constraint: Found 1 security vulnerability: CVE-2018-14335",
        22);
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.LOW,
        "2 - Component-Unknown - webgoat-server-8.0.0.M1.jar",
        "Unknown 3rd party component: Match state was 'Unknown', Component does not contain proprietary packages",
        34);
  }

  @Test
  public void testPullRequestCodeInsights_noAddedOrCleared() throws Exception {
    //setup test data
    setupTestData();
    diff.getAppeared().clear();

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false);

    //then assert that created contents match expected
    assertThat(details.getReportDetails())
        .isEqualTo("Sonatype Lifecycle found no new policy violations on " + bomTimestamp + ".");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.PASS);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(0, 0, 0));
    assertThat(details.getAnnotations()).isEmpty();
  }

  @Test
  public void testPullRequestCodeInsights_singlePolicyViolationPlurality() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();
    createTestData_Policies(true);

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getAppeared().add(first);

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false);

    //then assert that created contents has singular violation in heading
    assertThat(details.getReportDetails())
        .startsWith("On " + bomTimestamp + ", Sonatype Lifecycle found 1 new policy violation affecting 1 component.");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.FAIL);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(1, 0, 0));
    List<CodeInsightAnnotation> annotations = details.getAnnotations();
    assertThat(annotations).hasSize(1);
    assertAnnotation(annotations, BitbucketCodeInsightSeverity.HIGH,
        "10 - Unlikely Test Policy - org.springframework.security : spring-security-web : 4.2.3.RELEASE",
        "Nonsensical Constraint: Found licenses in the 'Liberal' license threat group ('Apache-2.0')",
        0);
  }

  @Test
  public void testPullRequestCodeInsights_singleClearedViolationPlurality() throws IOException, URISyntaxException {
    //setup test data
    setupTestData();

    final PolicyViolation first = diff.getAppeared().get(0);
    diff.getAppeared().clear();
    diff.getCleared().add(first);
    sourceControlComponentLoader.enhanceSourceControlComponentDetails(componentDetails, diff.getCleared());

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false);

    //then assert that created contents has singular violation in heading
    assertThat(details.getReportDetails()).contains("Sonatype Lifecycle found no new policy violations on " +
        bomTimestamp + ". 1 outstanding policy violation fixed, affecting 1 component");
    assertThat(details.getReportOutcome()).isEqualTo(BitbucketCodeInsightReportOutcome.PASS);
    assertThat(details.getReportUri()).isEqualTo(EXPECTED_REPORT_URI);
    assertThat(details.getReportData()).containsAllEntriesOf(expectedReportData(0, 0, 0));
    assertThat(details.getAnnotations()).isEmpty();
  }

  @Test
  public void testPullRequestCodeInsights_emptyBomData() throws IOException, URISyntaxException {
    //setup test data
    setupTestData("/PullRequestCodeInsightsDetailsTest/from-report",
        "/PullRequestCodeInsightsDetailsTest/to-report-empty-bom");

    //when
    PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
        bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation, diff,
        lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false);

    //then assert that created contents is not available
    String contents = details.getReportDetails();
    assertThat(contents).isEqualTo("Sonatype Lifecycle found no new policy violations on " + bomTimestamp + ".");
  }

  @Test
  public void testPullRequestCodeInsights_nullBom() throws IOException, URISyntaxException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, null, featureBranchPolicyEvaluation, diff,
            lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false))
        .withMessage("sourceControlComponentDetails is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullDiff() throws IOException, URISyntaxException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation,
            null, lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false))
        .withMessage("policyViolationDiff is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullApp() throws URISyntaxException, IOException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.normalizedRepositoryUrl, null, componentDetails, featureBranchPolicyEvaluation,
            diff, lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false))
        .withMessage("app is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullRepoUrl() throws URISyntaxException, IOException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            null, app, componentDetails, featureBranchPolicyEvaluation, diff,
            lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false))
        .withMessage("repositoryUrl is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullFeatureBranchEvaluation() throws IOException, URISyntaxException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, null, diff,
            lookup(BaseUrl.class).getConfigured(), locationDiscoveryResult, policyDAO, organizationDAO, false))
        .withMessage("featureBranchEvaluation is required and cannot be null");
  }

  @Test
  public void testPullRequestCodeInsights_nullBaseUrl() throws IOException, URISyntaxException {
    setupTestData();
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
        new PullRequestCodeInsightsDetails(
            bitbucketGitRepositoryInfo.normalizedRepositoryUrl, app, componentDetails, featureBranchPolicyEvaluation,
            diff, null, locationDiscoveryResult, policyDAO, organizationDAO, false))
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
    featureBranchPolicyEvaluation.setTime(new GregorianCalendar(2020, Calendar.JUNE, 21, 9, 15, 32).getTime());

    //setup diff
    diff = policyEvaluationDiffService.createPolicyViolationDiff(defaultBranchPolicyEvaluation,
        featureBranchPolicyEvaluation).get();

    //setup remediationVersionMap
    remediationVersionMap = new HashMap<>();
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.190", "", "jar");
    remediationVersionMap.put(ci, "1.4.200");

    //setup pullRequestLineComments
    pullRequestLineComments = new ArrayList<>();
    PullRequestLineCommentDTO lineCommentDTO = new PullRequestLineCommentDTO(ci, new DiffPosition("path",
            1, 0, 1, "456", 1));
    lineCommentDTO.setScmId(12345L);
    pullRequestLineComments.add(lineCommentDTO);

    //setup gitRepositoryInfo
    bitbucketGitRepositoryInfo = new GitRepositoryInfo(repositoryUrl, null, "user", "token",
        SourceControlProvider.BITBUCKET, "master", true, true,true, true, true, true, false, null);

    //setup source control component details
    componentDetails = sourceControlComponentLoader.getSourceControlComponentDetails(
        featureBranchPolicyEvaluation.getApplicationId(), featureBranchPolicyEvaluation.getScanId());

    bomTimestamp = DATE_TIME_FORMATTER.format(
        ZonedDateTime.ofInstant(featureBranchPolicyEvaluation.getTime().toInstant(), ZoneId.systemDefault()));

    Mockito.lenient().when(pullRequestLocationDiscoveryService.doLocationDiscovery(anyList(),
        any(GitRepositoryInfo.class), anyString(), anyString())).thenReturn(locationDiscoveryResult);
  }

  private void createTestData_Policies(boolean failInRelease) {
    // add policies - these are the ids in test data we have
    List<String> policyIds = Arrays.asList("041a17c5f04944178eb8cdfa0880d81b", "05f79154757d4fcfa3d1f7b0b539b7fb",
        "2aadd95bd58345e69723815413dd4a97", "f48231a74eb943c1ad866d60e4073099", "703d23823ddb40f9a3207011f315e37c");

    tempEntity.createSamplePolicyData(policyIds, failInRelease);
  }

  private Map<String, Object> expectedReportData(
      final Object criticalCount,
      final Object moderateCount,
      final Object severeCount)
  {
    return ImmutableMap.<String, Object>builder()
        .put("Critical", criticalCount)
        .put("Details",
            new BitbucketLinkDataParameter(bitbucketGitRepositoryInfo.normalizedRepositoryUrl, "Application Report",
                EXPECTED_REPORT_URI))
        .put("Moderate", moderateCount)
        .put("Organization", "TEST ORG")
        .put("Severe", severeCount)
        .put("Stage", "release")
        .build();
  }

  private void assertAnnotation(
      final List<CodeInsightAnnotation> annotations,
      final BitbucketCodeInsightSeverity severity,
      final String message, final String detail,
      final String path, final Integer lineNumber,
      final int index)
  {
    CodeInsightAnnotation annotation;
    if (BitbucketApiClientUtils.isCloudHosted(repositoryUrl)) {
      annotation = new BitbucketV2CodeInsightAnnotation(message, detail, severity,
          BitbucketCodeInsightAnnotationType.CODE_SMELL, null, path, lineNumber);
    }
    else {
      BitbucketV1CodeInsightAnnotation v1Annotation = new BitbucketV1CodeInsightAnnotation();
      v1Annotation.setSeverity(severity);
      v1Annotation.setType(BitbucketCodeInsightAnnotationType.CODE_SMELL);
      v1Annotation.setMessage(message + " - " + detail);
      v1Annotation.setLine(lineNumber);
      v1Annotation.setPath(path);
      annotation = v1Annotation;
    }
    assertThat(annotations).contains(annotation, Index.atIndex(index));
  }

  private void assertAnnotation(
      final List<CodeInsightAnnotation> annotations,
      final BitbucketCodeInsightSeverity severity,
      final String message, final String detail,
      final int index)
  {
    assertAnnotation(annotations, severity, message, detail, null, null, index);
  }
}
