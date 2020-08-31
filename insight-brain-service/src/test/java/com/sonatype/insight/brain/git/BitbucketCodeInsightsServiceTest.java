/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.scm.bitbucket.BitbucketApiClient;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightReportOutcome;
import com.sonatype.nexus.scm.bitbucket.BitbucketLinkDataParameter;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.sonatype.insight.brain.git.BitbucketCodeInsightsService.CODE_INSIGHT_LOGO_URL;
import static com.sonatype.insight.brain.git.BitbucketCodeInsightsService.CODE_INSIGHT_REPORTER;
import static com.sonatype.insight.brain.git.BitbucketCodeInsightsService.CODE_INSIGHT_REPORT_KEY;
import static com.sonatype.insight.brain.git.BitbucketCodeInsightsService.CODE_INSIGHT_REPORT_TITLE;
import static com.sonatype.insight.brain.git.BitbucketCodeInsightsService.CODE_INSIGHT_REPORT_TYPE;
import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class BitbucketCodeInsightsServiceTest
    extends AbstractComponentTest
{
  private static final String FROM_SCAN_ID = "fromScanId";

  private static final String TO_SCAN_ID = "toScanId";

  private static final String BRANCH = "branch";

  @Inject
  private InsightWork insightWork;

  @Inject
  private InsightConfig config;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private ReportService reportService;

  @Inject
  private PolicyEvaluationDiffService policyEvaluationDiffService;

  @Inject
  private BaseUrl baseUrl;

  @Mock
  private GitClientFactory gitClientFactory;

  @Mock
  private BitbucketApiClient<?, ?> bitbucketApiClient;

  private Application application;

  private GitRepositoryInfo gitRepositoryInfo;

  private PolicyEvaluation defaultBranchPolicyEvaluation;

  private PolicyEvaluation featureBranchPolicyEvaluation;

  private PolicyViolationDiff<PolicyViolation> policyViolationDiff;

  private BitbucketCodeInsightsService service;

  private LocationDiscoveryResult locationDiscoveryResult;

  @Before
  public void before() throws URISyntaxException, IOException {
    MockitoAnnotations.openMocks(this);

    config.setExperimentalFeatures(ImmutableMap.of(Feature.CODE_INSIGHTS.getFlag(), Boolean.TRUE));
    config.setBaseUrl("http://localhost:1122");
    application = tempEntity.newApplicationWithParent();
    service =
        new BitbucketCodeInsightsService(applicationDAO, reportService, config, baseUrl);

    createReportFile(application.getId(), FROM_SCAN_ID,
        zipReportDir("/BitbucketCodeInsightsServiceTest/from-report", tempDir), insightWork);
    createReportFile(application.getId(), TO_SCAN_ID,
        zipReportDir("/BitbucketCodeInsightsServiceTest/to-report", tempDir), insightWork);

    gitRepositoryInfo = new GitRepositoryInfo("https://foo.com", "username", "token", BITBUCKET, "baseBranch", true,
        true);

    //setup evaluations
    defaultBranchPolicyEvaluation = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, FROM_SCAN_ID);
    featureBranchPolicyEvaluation = tempEntity
        .newPolicyEvaluation(application.getId(), ReleaseStageType.ID, TO_SCAN_ID);
    featureBranchPolicyEvaluation.setTime(new GregorianCalendar(2020, 5, 21, 9, 15, 32).getTime());
    featureBranchPolicyEvaluation.setCommitHash("12345678abcd12345678");

    //setup diff
    policyViolationDiff = policyEvaluationDiffService
        .createPolicyViolationDiff(defaultBranchPolicyEvaluation, featureBranchPolicyEvaluation).get();

    lenient().when(gitClientFactory.createApiClient(gitRepositoryInfo)).thenReturn(bitbucketApiClient);

    locationDiscoveryResult = new LocationDiscoveryResult();
  }

  @Test
  public void testCodeInsightFeatureFlag() throws IOException {
    // verify when disabled that the feature is not interacted with
    config.setFeatures(ImmutableMap.of(Feature.CODE_INSIGHTS.getFlag(), Boolean.FALSE));
    service.invokeAction(gitClientFactory, gitRepositoryInfo, policyViolationDiff, featureBranchPolicyEvaluation,
        defaultBranchPolicyEvaluation, BRANCH, locationDiscoveryResult);
    verifyNoInteractions(bitbucketApiClient);

    // verify when enabled that the feature is interacted with
    config.setFeatures(ImmutableMap.of(Feature.CODE_INSIGHTS.getFlag(), Boolean.TRUE));
    service.invokeAction(gitClientFactory, gitRepositoryInfo, policyViolationDiff, featureBranchPolicyEvaluation,
        defaultBranchPolicyEvaluation, BRANCH, locationDiscoveryResult);
    verify(bitbucketApiClient).deleteCodeInsightReport(anyString(), anyString()); //interaction itself doesn't matter
  }

  @Test
  public void testCodeInsightFlow() throws IOException {
    service.invokeAction(gitClientFactory, gitRepositoryInfo, policyViolationDiff, featureBranchPolicyEvaluation,
        defaultBranchPolicyEvaluation, BRANCH, locationDiscoveryResult);

    URI reportUri = URI.create(String.format("http://localhost:1122/ui/links/application/%s/report/toScanId",
        application.getPublicId()));
    Map<String, Object> dataMap = new ImmutableMap.Builder<String, Object>()
        .put("Critical", 32)
        .put("Organization", organizationDAO.getByIdNotNull(application.getOrganizationId()).getName())
        .put("Severe", 4)
        .put("Stage", "release")
        .put("Moderate", 3)
        .put("Details",
            new BitbucketLinkDataParameter(gitRepositoryInfo.repositoryUrl, "Application Report", reportUri))
        .build();

    // verify the proper API calls are made
    verify(bitbucketApiClient)
        .deleteCodeInsightReport(featureBranchPolicyEvaluation.getCommitHash(), CODE_INSIGHT_REPORT_KEY);
    verify(bitbucketApiClient).createCodeInsightReport(eq(featureBranchPolicyEvaluation.getCommitHash()), anyString(),
        eq(BitbucketCodeInsightReportOutcome.FAIL), eq(CODE_INSIGHT_REPORT_TYPE), eq(CODE_INSIGHT_REPORT_TITLE),
        eq(CODE_INSIGHT_REPORTER), eq(reportUri), eq(CODE_INSIGHT_LOGO_URL), eq(CODE_INSIGHT_REPORT_KEY), eq(dataMap));
    verify(bitbucketApiClient)
        .createCodeInsightAnnotations(eq(featureBranchPolicyEvaluation.getCommitHash()), anyString(), anyList());
  }

  @Test
  public void testWrongProvider() {
    gitRepositoryInfo.provider = GITHUB;

    service.invokeAction(gitClientFactory, gitRepositoryInfo, policyViolationDiff, featureBranchPolicyEvaluation,
        defaultBranchPolicyEvaluation, BRANCH, locationDiscoveryResult);

    verifyNoInteractions(bitbucketApiClient);
  }
}
