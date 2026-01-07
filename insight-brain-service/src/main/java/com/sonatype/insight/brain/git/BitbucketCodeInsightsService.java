/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PullRequestCodeInsightsDetails;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.scm.bitbucket.BitbucketApiClient;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightReportType;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that creates/maintains Bitbucket code insight reports for a policy evaluation
 */
@Named
@Singleton
public class BitbucketCodeInsightsService
    implements PullRequestPostCommentAction
{
  private static final Logger log = LoggerFactory.getLogger(BitbucketCodeInsightsService.class);

  @VisibleForTesting
  static final String CODE_INSIGHT_REPORT_TITLE = "Sonatype Lifecycle for SCM";

  @VisibleForTesting
  static final String CODE_INSIGHT_REPORT_KEY = "nexus-iq-for-scm";

  @VisibleForTesting
  static final String CODE_INSIGHT_REPORTER = "Sonatype Lifecycle";

  @VisibleForTesting
  static final URI CODE_INSIGHT_LOGO_URL = URI
      .create("https://cdn.sonatype.com/iq-for-scm/1.0/bitbucket-code-insights.png");

  @VisibleForTesting
  static final BitbucketCodeInsightReportType CODE_INSIGHT_REPORT_TYPE = BitbucketCodeInsightReportType.BUG;

  private final ApplicationDAO applicationDAO;

  private final BaseUrl baseUrl;

  private final PolicyDAO policyDAO;

  private final OrganizationDAO organizationDAO;

  private final ScmReducedSecurityService scmReducedSecurityService;

  @Inject
  public BitbucketCodeInsightsService(
      final ApplicationDAO applicationDAO,
      final BaseUrl baseUrl,
      final PolicyDAO policyDAO,
      final OrganizationDAO organizationDAO,
      final ScmReducedSecurityService scmReducedSecurityService)
  {
    this.applicationDAO = applicationDAO;
    this.baseUrl = baseUrl;
    this.policyDAO = policyDAO;
    this.organizationDAO = organizationDAO;
    this.scmReducedSecurityService = scmReducedSecurityService;
  }

  @Override
  public void invokeAction(
      final GitClientFactory gitClientFactory,
      final GitRepositoryInfo gitRepositoryInfo,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      final SourceControlComponentDetails sourceControlComponentDetails,
      final PolicyEvaluation sourceCommitPolicyEvaluation,
      final PolicyEvaluation baseBranchPolicyEvaluation,
      final String branch,
      final LocationDiscoveryResult locationDiscoveryResult)
  {
    // Early return if the SCM provider doesn't support Code Insights.
    // Code Insights is a Bitbucket Server/Data Center specific feature that provides enhanced
    // reporting with inline annotations. Other SCM providers (GitHub, GitLab, Azure DevOps, etc.)
    // do not support this feature and will gracefully skip this step.
    if (!gitRepositoryInfo.provider.supportsCodeInsights()) {
      return;
    }

    // Early return if the CODE_INSIGHTS feature flag is disabled. When disabled, Bitbucket operations continue to
    // work normally - this only affects the creation of Code Insight reports. Other SCM features like PR commenting,
    // line commenting, and branch monitoring are independent and unaffected by this flag.
    // See SystemConfigurationPropertyFeature.CODE_INSIGHTS for more details.
    if (!SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled()) {
      return;
    }

    try {
      Application application = applicationDAO.getById(sourceCommitPolicyEvaluation.getApplicationId());
      boolean reducedSecurityData = scmReducedSecurityService.isReducedSecurityData(application.getId());
      PullRequestCodeInsightsDetails details = new PullRequestCodeInsightsDetails(
          gitRepositoryInfo.normalizedRepositoryUrl,
          application,
          sourceControlComponentDetails,
          sourceCommitPolicyEvaluation,
          policyViolationDiff,
          baseUrl.getConfigured(),
          locationDiscoveryResult,
          policyDAO,
          organizationDAO,
          reducedSecurityData);

      BitbucketApiClient<?, ?> bitbucketApiClient = getBitbucketApiClient(gitClientFactory, gitRepositoryInfo);

      // first delete any existing report (with annotations)
      bitbucketApiClient.deleteCodeInsightReport(sourceCommitPolicyEvaluation.getCommitHash(), CODE_INSIGHT_REPORT_KEY);

      bitbucketApiClient.createCodeInsightReport(sourceCommitPolicyEvaluation.getCommitHash(),
          details.getReportDetails(),
          details.getReportOutcome(),
          CODE_INSIGHT_REPORT_TYPE,
          CODE_INSIGHT_REPORT_TITLE,
          CODE_INSIGHT_REPORTER,
          details.getReportUri(),
          CODE_INSIGHT_LOGO_URL,
          CODE_INSIGHT_REPORT_KEY,
          details.getReportData());

      try {
        bitbucketApiClient
            .createCodeInsightAnnotations(sourceCommitPolicyEvaluation.getCommitHash(), CODE_INSIGHT_REPORT_KEY,
                details.getAnnotations());
      }
      catch (HttpResponseException e) {
        // Known issue by Bitbucket and it is harmless
        if (e.getMessage() == null ||
            !e.getMessage().contains("The field 'annotations' must be present and have at least 1 annotation")) {
          throw e;
        }
      }
    }
    catch (IOException e) {
      log.error("Error creating Bitbucket Code Insight", e);
    }
  }

  private BitbucketApiClient<?, ?> getBitbucketApiClient(
      final GitClientFactory gitClientFactory,
      final GitRepositoryInfo gitRepositoryInfo)
  {
    return (BitbucketApiClient<?, ?>) gitClientFactory.createApiClient(gitRepositoryInfo);
  }
}
