/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesUtilsService;
import com.sonatype.insight.brain.git.dto.PullRequestLineCommentCreationResult;
import com.sonatype.insight.brain.scm.event.PullRequestCommentingLogger;
import com.sonatype.insight.brain.scm.event.SourceControlEventLoggerFactory;
import com.sonatype.insight.brain.scm.event.SourceControlEventType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.CommentResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SourceControlEventData.forComment;
import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SourceControlEventData.forError;
import static com.sonatype.insight.brain.scm.event.SourceControlEventType.API_ERROR;

@Named
@Singleton
public class PullRequestCommentCreator
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentCreator.class);

  private final GitClientFactory gitClientFactory;

  private final SourceControlPullRequestCommentDAO pullRequestCommentDAO;

  private final PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator;

  private final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService;

  private final PullRequestCommentingClient pullRequestCommentingClient;

  private final PullRequestCommentingMetricsService prCommentingMetricsService;

  private final PullRequestLineCommentingService pullRequestLineCommentingService;

  // note: the framework will inject an empty list in the event there are no post-comment action classes defined
  private final Set<PullRequestPostCommentAction> pullRequestPostCommentActionList;

  private final PullRequestLocationDiscoveryService locationDiscoveryService;

  private final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  private final SourceControlComponentLoader sourceControlComponentLoader;

  private final ProductLicense productLicense;

  private final TelemetryUtils telemetryUtils;

  private final SourceControlEventLoggerFactory scmEventLoggerFactory;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  @Inject
  public PullRequestCommentCreator(
      final GitClientFactory gitClientFactory,
      final SourceControlPullRequestCommentDAO pullRequestCommentDAO,
      final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService,
      final PullRequestCommentingClient pullRequestCommentingClient,
      final PullRequestCommentingMetricsService prCommentingMetricsService,
      final PullRequestLineCommentingService pullRequestLineCommentingService,
      final Set<PullRequestPostCommentAction> pullRequestPostCommentActionList,
      final PullRequestLocationDiscoveryService locationDiscoveryService,
      final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService,
      final PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator,
      final SourceControlComponentLoader sourceControlComponentLoader,
      final ProductLicense productLicense,
      final TelemetryUtils telemetryUtils,
      final SourceControlEventLoggerFactory scmEventLoggerFactory,
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO)
  {
    this.gitClientFactory = gitClientFactory;
    this.pullRequestCommentDAO = pullRequestCommentDAO;
    this.pullRequestFeedbackMarkupService = pullRequestFeedbackMarkupService;
    this.pullRequestCommentingClient = pullRequestCommentingClient;
    this.prCommentingMetricsService = prCommentingMetricsService;
    this.pullRequestLineCommentingService = pullRequestLineCommentingService;
    this.pullRequestPostCommentActionList = pullRequestPostCommentActionList;
    this.locationDiscoveryService = locationDiscoveryService;
    this.developmentPrioritiesUtilsService = developmentPrioritiesUtilsService;
    this.pullRequestCommentingEligibilityValidator = pullRequestCommentingEligibilityValidator;
    this.sourceControlComponentLoader = sourceControlComponentLoader;
    this.productLicense = productLicense;
    this.telemetryUtils = telemetryUtils;
    this.scmEventLoggerFactory = scmEventLoggerFactory;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
  }

  public void createPullRequestComment(
      PullRequestPolicyEvaluationsDTO prPolicyEvaluationsDTO,
      PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      String contentHash)
  {
    doCreateOrUpdateComments(prPolicyEvaluationsDTO, null, policyViolationDiff, remediationVersionMap, contentHash);
  }

  public void updatePullRequestComment(
      PullRequestPolicyEvaluationsDTO prPolicyEvaluationsDTO,
      SourceControlPullRequestComment existingPullRequestComment,
      PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      String contentHash)
  {
    doCreateOrUpdateComments(prPolicyEvaluationsDTO, existingPullRequestComment, policyViolationDiff,
        remediationVersionMap, contentHash);
  }

  /**
   * Ability to handle invoke post-comment actions (like Code Insights) without updating PR comments.
   * This is useful when policy evaluations haven't changed, but we still want to post Code Insights
   * for a new commit.
   *
   * @param prPolicyEvaluationsDTO the PR and policy evaluation context
   * @param policyViolationDiff the diff between source and target evaluations
   * @param sourceControlComponentDetails component details needed for Code Insights
   * @param locationDiscoveryResult location discovery result (can be null if not available)
   * @see <a href="https://sonatype.atlassian.net/browse/CLM-35694">CLM-35694</a>
   */
  public void handlePostCommentActions(
      PullRequestPolicyEvaluationsDTO prPolicyEvaluationsDTO,
      PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      SourceControlComponentDetails sourceControlComponentDetails,
      LocationDiscoveryResult locationDiscoveryResult)
  {
    PolicyEvaluation featureBranchPolicyEvaluation = prPolicyEvaluationsDTO.getFeatureBranchPolicyEvaluation();
    PolicyEvaluation targetPolicyEvaluation = prPolicyEvaluationsDTO.getTargetPolicyEvaluation();

    invokePostCommentActions(
        prPolicyEvaluationsDTO.getGitRepositoryInfo(),
        policyViolationDiff,
        sourceControlComponentDetails,
        featureBranchPolicyEvaluation,
        targetPolicyEvaluation,
        prPolicyEvaluationsDTO.getFeatureBranchName(),
        locationDiscoveryResult);
  }

  private void doCreateOrUpdateComments(
      PullRequestPolicyEvaluationsDTO prPolicyEvaluationsDTO,
      final SourceControlPullRequestComment existingPullRequestComment,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      final String contentHash)
  {
    Application application = applicationDAO.getById(prPolicyEvaluationsDTO.getApplicationId());
    Organization organization = application != null ? organizationDAO.getById(application.getOrganizationId()) : null;

    PullRequestCommentingLogger scmEventLogger = scmEventLoggerFactory.newLogger(
        new Date(), application, organization, prPolicyEvaluationsDTO.getGitRepositoryInfo());

    PullRequestCommentTelemetry telemetry = new PullRequestCommentTelemetry(
        prPolicyEvaluationsDTO.getApplicationId(), prPolicyEvaluationsDTO.getPullRequestNumber(),
        telemetryUtils.obfuscateIfAdvancedReportingDisabled(prPolicyEvaluationsDTO.getApplicationId()));

    PolicyEvaluation featureBranchPolicyEvaluation = prPolicyEvaluationsDTO.getFeatureBranchPolicyEvaluation();
    PolicyEvaluation targetPolicyEvaluation = prPolicyEvaluationsDTO.getTargetPolicyEvaluation();

    LocationDiscoveryResult locationDiscoveryResult = getLocationDiscovery(prPolicyEvaluationsDTO, policyViolationDiff);

    // line comment sub-flow
    PullRequestLineCommentCreationResult lineCommentsCreationResult = pullRequestLineCommentingService
        .createPullRequestLineComments(
            policyViolationDiff.getAppeared(),
            prPolicyEvaluationsDTO.getGitRepositoryInfo(),
            remediationVersionMap,
            prPolicyEvaluationsDTO.getPullRequestNumber(),
            featureBranchPolicyEvaluation.getCommitHash(),
            prPolicyEvaluationsDTO.getApplicationId(),
            featureBranchPolicyEvaluation.getId(),
            targetPolicyEvaluation.getId(),
            locationDiscoveryResult,
            featureBranchPolicyEvaluation.getScanId());
    try {
      List<PullRequestLineCommentDTO> pullRequestLineComments =
          lineCommentsCreationResult.getPullRequestLineCommentDtoList();
      telemetry.lineCommentCount = pullRequestLineComments.size();

      SourceControlComponentDetails sourceControlComponentDetails =
          getSourceControlComponentDetails(prPolicyEvaluationsDTO, policyViolationDiff, pullRequestLineComments);

      Optional<String> policyEvaluationDiffMarkup = pullRequestFeedbackMarkupService.createMarkup(
          policyViolationDiff, remediationVersionMap, pullRequestLineComments,
          prPolicyEvaluationsDTO.getGitRepositoryInfo(), prPolicyEvaluationsDTO.getPullRequestNumber(),
          featureBranchPolicyEvaluation, targetPolicyEvaluation, sourceControlComponentDetails, telemetry,
          productLicense.hasFeature(LicensedFeature.SCM_UX_IMPROVEMENTS), developmentPrioritiesUtilsService);

      if (policyEvaluationDiffMarkup.isPresent()) {
        Optional<CommentResponse> response = Optional.empty();
        if (existingPullRequestComment != null || policyViolationDiff.hasAppeared() ||
            policyViolationDiff.hasCleared()) {
          response = pullRequestCommentingClient.createOrUpdateCommentInGitSCM(
              prPolicyEvaluationsDTO.getApplicationId(),
              prPolicyEvaluationsDTO.getGitRepositoryInfo(),
              prPolicyEvaluationsDTO.getPullRequestNumber(),
              policyEvaluationDiffMarkup.get(),
              existingPullRequestComment,
              telemetry);
        }

        if (response.isPresent() ||
            SourceControlProvider.BITBUCKET == prPolicyEvaluationsDTO.getGitRepositoryInfo().getProvider()) {
          if (response.isPresent()) {
            CommentResponse commentResponse = response.get();
            recordCommentInDatabase(
                prPolicyEvaluationsDTO,
                commentResponse.getId(),
                commentResponse.getVersion(),
                contentHash,
                existingPullRequestComment);
          }

          SourceControlEventType eventType = existingPullRequestComment == null
              ? SourceControlEventType.PR_COMMENT_CREATED
              : SourceControlEventType.PR_COMMENT_UPDATED;

          scmEventLogger.add(eventType, forComment(
              String.valueOf(prPolicyEvaluationsDTO.getPullRequestNumber()),
              policyViolationDiff.getAppeared().size(),
              policyViolationDiff.getCleared().size()));

          invokePostCommentActions(
              prPolicyEvaluationsDTO.getGitRepositoryInfo(),
              policyViolationDiff,
              sourceControlComponentDetails,
              featureBranchPolicyEvaluation,
              targetPolicyEvaluation,
              prPolicyEvaluationsDTO.getFeatureBranchName(),
              locationDiscoveryResult);

          prCommentingMetricsService.sendTelemetry(telemetry);

          AuditEvent auditEvent = existingPullRequestComment == null
              ? AuditEvent.CREATE_PULL_REQUEST_COMMENT : AuditEvent.UPDATE_PULL_REQUEST_COMMENT;
          prCommentingMetricsService.addAuditRecord(
              auditEvent, prPolicyEvaluationsDTO.getApplicationId(),
              prPolicyEvaluationsDTO.getGitRepositoryInfo().normalizedRepositoryUrl,
              prPolicyEvaluationsDTO.getPullRequestNumber());

          scmEventLogger.log();
        }
      }
      else {
        log.info("generated feedback markup was empty for application '{}' pull request '{}'",
            prPolicyEvaluationsDTO.getApplicationId(), prPolicyEvaluationsDTO.getPullRequestNumber());
      }
    }
    catch (Exception e) {
      scmEventLogger.add(API_ERROR, forError("Failed to create or update PR comments: " + e.getMessage()));
      scmEventLogger.log();

      SourceControlException sourceControlException =
          new SourceControlException("Failed to create or update PR comments: " + e.getMessage(), e);
      addLineCommentsCreationException(lineCommentsCreationResult, sourceControlException);
      throw sourceControlException;
    }

    if (lineCommentsCreationResult.hasExceptions()) {
      SourceControlException sourceControlException =
          new SourceControlException("Failed to delete/create some PR line comments", true);
      addLineCommentsCreationException(lineCommentsCreationResult, sourceControlException);
      throw sourceControlException;
    }
  }

  private void addLineCommentsCreationException(
      PullRequestLineCommentCreationResult pullRequestLineCommentCreationResult,
      Exception targetException)
  {
    if (pullRequestLineCommentCreationResult.hasExceptions()) {
      for (Exception exception : pullRequestLineCommentCreationResult.getExceptionList()) {
        targetException.addSuppressed(exception);
      }
    }
  }

  private SourceControlComponentDetails getSourceControlComponentDetails(
      PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO,
      PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      List<PullRequestLineCommentDTO> pullRequestLineComments) throws IOException
  {
    SourceControlComponentDetails sourceControlComponentDetails = sourceControlComponentLoader
        .getSourceControlComponentDetails(
            pullRequestPolicyEvaluationsDTO.getApplicationId(),
            pullRequestPolicyEvaluationsDTO.getFeatureBranchPolicyEvaluation().getScanId());

    sourceControlComponentLoader
        .enhanceSourceControlComponentDetails(sourceControlComponentDetails, policyViolationDiff.getCleared());

    sourceControlComponentLoader
        .enhanceSourceControlComponentDetailsWithDirectDependencyInformation(sourceControlComponentDetails,
            pullRequestLineComments);

    return sourceControlComponentDetails;
  }

  private LocationDiscoveryResult getLocationDiscovery(
      PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO,
      PolicyViolationDiff<PolicyViolation> policyViolationDiff)
  {
    LocationDiscoveryResult locationDiscoveryResult = new LocationDiscoveryResult();
    if (pullRequestCommentingEligibilityValidator.isLocationDiscoveryNeededAndAllowed(
        pullRequestPolicyEvaluationsDTO.getSourceControlProvider(), policyViolationDiff)) {
      locationDiscoveryResult = locationDiscoveryService.doLocationDiscovery(
          policyViolationDiff.getAppeared(),
          pullRequestPolicyEvaluationsDTO.getGitRepositoryInfo(),
          pullRequestPolicyEvaluationsDTO.getFeatureBranchName(),
          pullRequestPolicyEvaluationsDTO.getApplicationId());
    }
    return locationDiscoveryResult;
  }

  /**
   * record in the DB that we've created or updated a comment for the given pull request for the given app
   */
  private void recordCommentInDatabase(
      PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO,
      Long commentId,
      Integer commentVersion,
      String contentHash,
      SourceControlPullRequestComment existingPullRequestComment)
  {
    if (existingPullRequestComment == null) {
      SourceControlPullRequestComment pullRequestComment =
          new SourceControlPullRequestComment(
              pullRequestPolicyEvaluationsDTO.getApplicationId(),
              pullRequestPolicyEvaluationsDTO.getPullRequestNumber(),
              commentId,
              commentVersion,
              contentHash,
              pullRequestPolicyEvaluationsDTO.getFeatureBranchPolicyEvaluationId(),
              pullRequestPolicyEvaluationsDTO.getTargetPolicyEvaluationId()
          );
      pullRequestCommentDAO.insert(pullRequestComment);
    }
    else {
      existingPullRequestComment.setPullRequestCommentId(commentId);
      existingPullRequestComment.setPullRequestCommentVersion(commentVersion);
      existingPullRequestComment.setContentHash(contentHash);
      existingPullRequestComment
          .setSourcePolicyEvaluationId(pullRequestPolicyEvaluationsDTO.getFeatureBranchPolicyEvaluationId());
      existingPullRequestComment
          .setTargetPolicyEvaluationId(pullRequestPolicyEvaluationsDTO.getTargetPolicyEvaluationId());
      pullRequestCommentDAO.update(existingPullRequestComment);
    }
    log.debug("pull request comment '{}' for application '{}' pull request '{}' recorded in database", commentId,
        pullRequestPolicyEvaluationsDTO.getApplicationId(), pullRequestPolicyEvaluationsDTO.getPullRequestNumber());
  }

  /**
   * Invoked after a comment has been created or updated
   */
  private void invokePostCommentActions(
      final GitRepositoryInfo gitRepositoryInfo,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      final SourceControlComponentDetails sourceControlComponentDetails,
      final PolicyEvaluation sourceCommitPolicyEvaluation,
      final PolicyEvaluation baseBranchPolicyEvaluation,
      final String branch,
      final LocationDiscoveryResult locationDiscoveryResult)
  {
    pullRequestPostCommentActionList.forEach(pullRequestPostCommentAction -> pullRequestPostCommentAction
        .invokeAction(gitClientFactory, gitRepositoryInfo, policyViolationDiff, sourceControlComponentDetails,
            sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation, branch, locationDiscoveryResult));
  }
}
