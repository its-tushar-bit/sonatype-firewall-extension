/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.experimental.sast;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastFindingDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastPullRequestCommentDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.PullRequestCommentingEligibilityValidator;
import com.sonatype.insight.brain.git.PullRequestInfoClient;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
import com.sonatype.insight.brain.git.SourceControlException;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastPullRequestComment;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.SastPullRequestCommentTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;
import com.sonatype.nexus.scm.api.model.CommitInformation;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.api.model.PullRequestState;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.telemetry.SastPullRequestCommentTelemetry.SAST_PULL_REQUEST_COMMENT_TELEMETRY;

// since 1.174
@Named
public class SastPullRequestCommentingService
{
  private static final Logger log = LoggerFactory.getLogger(SastPullRequestCommentingService.class);

  private final PullRequestInfoClient pullRequestInfoClient;

  private final GitClientFactory gitClientFactory;

  private final SourceControlUtils sourceControlUtils;

  private final SastPullRequestCommentDAO sastPullRequestCommentDAO;

  private final SastFindingDAO sastFindingDAO;

  private final ApplicationDAO applicationDAO;

  private final BaseUrl baseUrl;

  private final TelemetrySender telemetrySender;

  private final ScmRepoVisibilityService scmRepoVisibilityService;

  private final PullRequestCommentingEligibilityValidator prCommentingEligibilityValidator;

  private final FeaturesService featuresService;

  @Inject
  public SastPullRequestCommentingService(
      final PullRequestInfoClient pullRequestInfoClient,
      final GitClientFactory gitClientFactory,
      final SourceControlUtils sourceControlUtils,
      final SastPullRequestCommentDAO sastPullRequestCommentDAO,
      final SastFindingDAO sastFindingDAO,
      final ApplicationDAO applicationDAO,
      final BaseUrl baseUrl,
      final TelemetrySender telemetrySender,
      final ScmRepoVisibilityService scmRepoVisibilityService,
      final PullRequestCommentingEligibilityValidator prCommentingEligibilityValidator,
      final FeaturesService featuresService)
  {
    this.pullRequestInfoClient = pullRequestInfoClient;
    this.gitClientFactory = gitClientFactory;
    this.sourceControlUtils = sourceControlUtils;
    this.sastPullRequestCommentDAO = sastPullRequestCommentDAO;
    this.sastFindingDAO = sastFindingDAO;
    this.applicationDAO = applicationDAO;
    this.baseUrl = baseUrl;
    this.telemetrySender = telemetrySender;
    this.scmRepoVisibilityService = scmRepoVisibilityService;
    this.prCommentingEligibilityValidator = prCommentingEligibilityValidator;
    this.featuresService = featuresService;
  }

  public void createOrUpdateSastPullRequestComment(final SastScan sastScan, final String commitHash) {
    final GitRepositoryInfo gitRepoInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(sastScan.getApplicationId());
    if (gitRepoInfo == null) {
      log.warn("Could not fetch git repository information for application [{}], skipping SAST PR commenting.",
          sastScan.getApplicationId());
      return;
    }

    final CommitInformation commitInfo;
    try {
      commitInfo = pullRequestInfoClient.getCommitInfoFromScm(gitRepoInfo, commitHash);
    }
    catch (final SourceControlException e) {
      log.warn("Could not create SAST PR comment: {}", e.getMessage());
      return;
    }

    final PullRequest pullRequest = commitInfo.getPullRequests()
        .stream()
        .filter(pr -> PullRequestState.OPEN == pr.getState())
        .max(Comparator.comparing(PullRequest::getUpdated))
        .orElse(null);
    final GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepoInfo);
    final List<SastFinding> sastFindings = sastFindingDAO.getBySastScanIdOrderBySeverityDesc(sastScan.getId());

    SastPullRequestComment preexistingSastPullRequestComment = null;
    if (pullRequest != null) {
      preexistingSastPullRequestComment = sastPullRequestCommentDAO.getByPullRequestUrl(pullRequest.getUrl());
    }

    if (canComment(pullRequest, gitRepoInfo, sastFindings.size(), commitHash, sastScan,
        preexistingSastPullRequestComment))
    {
      doCreateOrUpdateComment(gitRepoInfo, gitApiClient, commitHash, sastScan, sastFindings, pullRequest,
          preexistingSastPullRequestComment);
    }
  }

  private void doCreateOrUpdateComment(
      final GitRepositoryInfo gitRepoInfo,
      final GitApiClient gitApiClient,
      final String commitHash,
      final SastScan sastScan,
      final List<SastFinding> sastFindings,
      final PullRequest pullRequest,
      final SastPullRequestComment preexistingSastPullRequestComment)
  {
    final String baseUrlString = getBaseUrl();
    if (StringUtils.isEmpty(baseUrlString)) {
      log.warn("IQ base URL is not configured, skipping SAST PR commenting.");
      return;
    }

    final String commentText = getCommentContent(baseUrlString, sastScan, sastFindings);
    if (StringUtils.isEmpty(commentText)) {
      // Error logged when attempting to construct report URL
      return;
    }
    final SastPullRequestCommentTelemetry telemetry =
        new SastPullRequestCommentTelemetry(sastScan.getApplicationId(), pullRequest.getNumber(),
            gitRepoInfo.provider.name());
    final CommentResponse commentResponse;
    if (preexistingSastPullRequestComment == null) {
      telemetry.action = SastPullRequestCommentTelemetry.ACTION_CREATED;
      commentResponse = createComment(gitApiClient, commitHash, sastScan, commentText, pullRequest);
    }
    else {
      telemetry.action = SastPullRequestCommentTelemetry.ACTION_UPDATED;
      commentResponse =
          updateComment(gitApiClient, commitHash, preexistingSastPullRequestComment, sastScan, commentText,
              pullRequest);
    }

    if (commentResponse != null) {
      telemetry.commentId = commentResponse.getId();
      sendTelemetry(telemetry);
    }
  }

  private CommentResponse createComment(
      final GitApiClient gitApiClient,
      final String commitHash,
      final SastScan sastScan,
      final String commentText,
      final PullRequest pullRequest)
  {
    final CommentResponse commentResponse;
    try {
      commentResponse = gitApiClient.createPullRequestComment(pullRequest.getNumber(), commentText);

      if (commentResponse == null) {
        log.warn("Comment could not be created for pull request [{}] for application [{}]", pullRequest.getNumber(),
            sastScan.getApplicationId());
        return null;
      }
    }
    catch (final IOException e) {
      log.error("Comment could not be created for pull request [{}] for application [{}]", pullRequest.getNumber(),
          sastScan.getApplicationId(), e);
      return null;
    }

    log.info("Pull request comment [{}] created for application [{}] pull request [{}]",
        commentResponse.getId(), sastScan.getApplicationId(), pullRequest.getNumber());

    sastPullRequestCommentDAO.insert(new SastPullRequestComment(sastScan.getId(),
        pullRequest.getUrl(), commitHash, getCommentTextHash(commentText),
        String.valueOf(commentResponse.getId()), 0));

    return commentResponse;
  }

  private CommentResponse updateComment(
      final GitApiClient gitApiClient,
      final String commitHash,
      final SastPullRequestComment preexistingSastPrComment,
      final SastScan sastScan,
      final String commentText,
      final PullRequest pullRequest)
  {
    final CommentResponse commentResponse;
    try {
      commentResponse =
          gitApiClient.updatePullRequestComment(Long.parseLong(preexistingSastPrComment.getPullRequestCommentId()),
              pullRequest.getNumber(), preexistingSastPrComment.getPullRequestCommentVersion(), commentText);

      if (commentResponse == null) {
        log.warn("Pull request [{}] could not be updated for application [{}]", pullRequest.getNumber(),
            sastScan.getApplicationId());
        return null;
      }
    }
    catch (final IOException e) {
      log.error("Pull request [{}] could not be updated for application [{}]", pullRequest.getNumber(),
          sastScan.getApplicationId(), e);
      return null;
    }

    log.info("Pull request comment [{}] updated for application [{}] pull request [{}]",
        commentResponse.getId(), sastScan.getApplicationId(), pullRequest.getNumber());

    preexistingSastPrComment.setCommitHash(commitHash);
    preexistingSastPrComment.setContentHash(getCommentTextHash(commentText));
    preexistingSastPrComment.setPullRequestCommentVersion(preexistingSastPrComment.getPullRequestCommentVersion() + 1);
    preexistingSastPrComment.setLastUpdatedAt(new Date());

    sastPullRequestCommentDAO.update(preexistingSastPrComment);

    return commentResponse;
  }

  private boolean canComment(
      final PullRequest pullRequest,
      final GitRepositoryInfo gitRepoInfo,
      final int sastFindingsCount,
      final String commitHash,
      final SastScan sastScan,
      final SastPullRequestComment preexistingSastPullRequestComment)
  {
    if (!hasDeveloperFeature()) {
      log.warn("Sonatype Developer is not a feature of this license, skipping SAST PR commenting");
      return false;
    }
    if (pullRequest == null) {
      log.warn("No open pull requests for commit [{}] available, skipping SAST PR commenting.", commitHash);
      return false;
    }
    if (!prCommentingEligibilityValidator.isPullRequestCommentingEnabled(gitRepoInfo)) {
      log.warn("PR commenting is not enabled for application [{}], skipping SAST PR commenting.",
          sastScan.getApplicationId());
      return false;
    }
    // Only allow a comment about 0 findings if a comment already exists
    if (sastFindingsCount == 0 && preexistingSastPullRequestComment == null) {
      log.warn("No SAST findings for SAST scan [{}], skipping SAST PR commenting.", sastScan.getId());
      return false;
    }
    if (!scmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(gitRepoInfo)) {
      log.warn("Repository [{}] is not private or internal, skipping SAST PR commenting.", gitRepoInfo.repositoryUrl);
      return false;
    }
    return true;
  }

  private boolean hasDeveloperFeature() {
    final Set<Feature> features = featuresService.getFeatures();
    return features.contains(LicensedFeature.DEVELOPER_DASHBOARD);
  }

  private String getBaseUrl() {
    try {
      return baseUrl.getConfigured();
    }
    catch (final IllegalStateException e) {
      return null;
    }
  }

  private String getCommentContent(
      final String baseUrlString,
      final SastScan sastScan,
      final List<SastFinding> sastFindings)
  {
    final String sastReportUrl = getSastReportUrl(baseUrlString, sastScan);
    if (StringUtils.isEmpty(sastReportUrl)) {
      return null;
    }
    final long numCriticalFindings = sastFindings.stream()
        // Severity ID 4 == SastFindingSeverity.CRITICAL
        .filter(sastFinding -> sastFinding.getSeverityId() == 4)
        .count();

    return getCommentText(numCriticalFindings, sastFindings.size(), sastReportUrl);
  }

  private String getSastReportUrl(final String baseUrlString, final SastScan sastScan) {
    final Application application = applicationDAO.getById(sastScan.getApplicationId());
    try {
      final URL baseUrlFormat = new URL(baseUrlString);
      return new URL(baseUrlFormat,
          String.format("assets/index.html#/application/%s/sastScan/%s", application.getPublicId(), sastScan.getId()))
          .toString();
    }
    catch (final MalformedURLException e) {
      // This should theoretically never be thrown because IQ does not allow you to set an invalid base URL
      log.error("A valid report URL from base URL [{}] could not be constructed, skipping SAST PR commenting",
          baseUrlString, e);
      return null;
    }
  }

  private static String getCommentText(
      final long numCriticalFindings,
      final int numTotalFindings,
      final String reportUrl)
  {
    final String viewReport = String.format("[Click here](%s) to view the full SAST report.", reportUrl);
    final String issue = "issue";
    final String issues = "issues";

    final StringBuilder commentText = new StringBuilder("Sonatype found ");
    if (numTotalFindings == 0) {
      commentText.append("**0 issues**");
    }
    else if (numCriticalFindings > 0) {
      commentText.append(String.format("**%d critical** %s out of **%d total** %s", numCriticalFindings,
          numCriticalFindings == 1 ? issue : issues, numTotalFindings,
          numTotalFindings == 1 ? issue : issues));
    }
    else {
      commentText.append(String.format("**%d total** %s", numTotalFindings, numTotalFindings == 1 ? issue : issues));
    }
    commentText.append(" active in this branch. ")
        .append(viewReport);

    return commentText.toString();
  }

  private static String getCommentTextHash(final String commentText) {
    return DigestUtils.sha256Hex(commentText);
  }

  private void sendTelemetry(final SastPullRequestCommentTelemetry telemetry) {
    telemetry.applicationId = HdsClientAnalytics.obfuscate(telemetry.applicationId);
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SAST_SOURCE_CONTROL_PULL_REQUEST_COMMENT);
    telemetryData.put(SAST_PULL_REQUEST_COMMENT_TELEMETRY, telemetry);
    telemetrySender.send(telemetryData);
  }
}
