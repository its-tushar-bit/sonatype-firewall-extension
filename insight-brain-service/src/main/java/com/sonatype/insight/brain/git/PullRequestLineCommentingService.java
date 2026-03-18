/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.git.dto.PullRequestLineCommentCreationResult;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.iq.location.discovery.PositionDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.PositionDiscoveryResult;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.DiffPosition;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestLineCommentingService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestLineCommentingService.class);

  private final GitClientFactory gitClientFactory;

  private final SourceControlPullRequestCommentDAO pullRequestCommentDAO;

  private final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService;

  private final PositionDiscoveryExecutor positionDiscoveryExecutor;

  private final PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator;

  private final ProductLicense productLicense;

  @Inject
  public PullRequestLineCommentingService(
      final GitClientFactory gitClientFactory,
      final SourceControlPullRequestCommentDAO pullRequestCommentDAO,
      final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService,
      final PositionDiscoveryExecutor positionDiscoveryExecutor,
      final PullRequestCommentingEligibilityValidator pullRequestCommentingEligibilityValidator,
      final ProductLicense productLicense)
  {
    this.gitClientFactory = gitClientFactory;
    this.pullRequestCommentDAO = pullRequestCommentDAO;
    this.pullRequestFeedbackMarkupService = pullRequestFeedbackMarkupService;
    this.positionDiscoveryExecutor = positionDiscoveryExecutor;
    this.pullRequestCommentingEligibilityValidator = pullRequestCommentingEligibilityValidator;
    this.productLicense = productLicense;
  }

  /**
   * This method encapsulates the complete flow for pull request line commenting of policy violation diffs between the
   * development branch commit that triggered the policy evaluation (which then issued this event) and the most recently
   * available policy evaluation for the source control configured base branch for the associated application.
   *
   * @return PR line comments creation result
   */
  public PullRequestLineCommentCreationResult createPullRequestLineComments(
      final List<PolicyViolation> violationList,
      final GitRepositoryInfo gitRepositoryInfo,
      final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      final int pullRequestId,
      final String commitHash,
      final String applicationId,
      final String sourcePolicyEvaluationId,
      final String basePolicyEvaluationId,
      final LocationDiscoveryResult locationDiscoveryResult,
      final String featureBranchScanId)
  {
    final PullRequestLineCommentCreationResult lineCommentCreationResult =
        new PullRequestLineCommentCreationResult();

    if (!pullRequestCommentingEligibilityValidator.isPullRequestLineCommentingEnabled(gitRepositoryInfo)) {
      return lineCommentCreationResult;
    }

    try {
      deleteExistingLineCommentsIfExists(lineCommentCreationResult, applicationId, gitRepositoryInfo, pullRequestId);
      if (!CollectionUtils.isEmpty(violationList)) {
        if (locationDiscoveryResult != null && !locationDiscoveryResult.getLocationMap().isEmpty()) {
          GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);

          // Find the best positions to comment on available in the PR diff
          PositionDiscoveryResult positionDiscoveryResult =
              positionDiscoveryExecutor.execute(locationDiscoveryResult.getLocationMap(), pullRequestId, gitApiClient);

          if (positionDiscoveryResult != null && !positionDiscoveryResult.getDiffPositionsByComponent().isEmpty()) {

            Map<ComponentIdentifier, List<DiffPosition>> diffPositionMap =
                positionDiscoveryResult.getDiffPositionsByComponent();

            // Build a list of line comments to be created
            buildLineCommentList(lineCommentCreationResult, diffPositionMap, violationList);

            addMarkupToLineComments(
                lineCommentCreationResult.getPullRequestLineCommentDtoList(),
                remediationVersionMap,
                gitRepositoryInfo.getProvider(),
                gitRepositoryInfo.getRepositoryUrl(),
                applicationId,
                featureBranchScanId);

            createLineComments(lineCommentCreationResult, gitApiClient, pullRequestId, commitHash,
                sourcePolicyEvaluationId, basePolicyEvaluationId, applicationId);

            // Filter out unsuccessful comment attempts
            lineCommentCreationResult.setPullRequestLineCommentDtoList(
                lineCommentCreationResult.getPullRequestLineCommentDtoList()
                    .stream()
                    .filter(i -> i.getScmId() != null)
                    .collect(Collectors.toList()));
          }
        }
      }
    }
    catch (Exception e) {
      throw new SourceControlException("Cannot create PullRequest line comments - reason: " + e.getMessage(), e);
    }

    return lineCommentCreationResult;
  }

  /**
   * Creates the line comments in the target SCM and records them in the database.
   */
  private void createLineComments(
      final PullRequestLineCommentCreationResult lineCommentCreationResult,
      final GitApiClient gitApiClient,
      final int pullRequestId,
      final String commitHash,
      final String sourcePolicyEvaluationId,
      final String basePolicyEvaluationId,
      final String applicationId)
  {
    int totalCount = 0;
    int successfulCount = 0;
    for (PullRequestLineCommentDTO lineCommentDTO : lineCommentCreationResult.getPullRequestLineCommentDtoList()) {
      if (lineCommentDTO.hasMarkup()) {
        totalCount++;
        try {
          if (createLineComment(applicationId, gitApiClient, commitHash, pullRequestId, sourcePolicyEvaluationId,
              basePolicyEvaluationId, lineCommentDTO))
          {
            successfulCount++;
          }
        }
        catch (IOException e) {
          String message = String.format("Cannot create PR line comment for pr %s, at line number: %s", pullRequestId,
              lineCommentDTO.getDiffPosition().getNewLineNumber());
          lineCommentCreationResult.addException(new IOException(message, e));
        }
      }
    }
    log.info("Pull request line comments created {} out of {} attempted for application '{}' and pull request '{}'",
        successfulCount, totalCount, applicationId, pullRequestId);
  }

  private boolean createLineComment(
      final String applicationId,
      final GitApiClient gitApiClient,
      final String commitHash,
      final int pullRequestId,
      final String sourcePolicyEvaluationId,
      final String basePolicyEvaluationId,
      final PullRequestLineCommentDTO lineCommentDTO) throws IOException
  {
    boolean wasCreated = false;

    CommentResponse response = gitApiClient
        .createPullRequestLineComment(pullRequestId, lineCommentDTO.getMarkup(), commitHash,
            lineCommentDTO.getDiffPosition());

    if (response.getId() != null) {
      lineCommentDTO.setScmId(response.getId());
      lineCommentDTO.setScmVersion(response.getVersion());

      // Add the line comment details to the database
      SourceControlPullRequestComment pullRequestComment = new SourceControlPullRequestComment(
          applicationId, //
          lineCommentDTO.getHash(), //
          lineCommentDTO.getDiffPosition().getFilePath(), //
          pullRequestId, //
          response.getId(), //
          response.getVersion(), //
          sourcePolicyEvaluationId, //
          basePolicyEvaluationId);
      pullRequestCommentDAO.insert(pullRequestComment);
      wasCreated = true;
    }
    return wasCreated;
  }

  /**
   * Adds content to the provided line comment list
   */
  private void addMarkupToLineComments(
      final List<PullRequestLineCommentDTO> lineCommentList,
      final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      final SourceControlProvider provider,
      final String scmBaseUrl,
      final String applicationId,
      final String featureBranchScanId)
  {
    for (PullRequestLineCommentDTO lineCommentDTO : lineCommentList) {
      ComponentIdentifier componentIdentifier = lineCommentDTO.getComponentIdentifier();
      RemediationVersionDTO remediationVersion = remediationVersionMap.get(componentIdentifier);
      Optional<String> codeSuggestion = createCodeSuggestion(
          provider,
          componentIdentifier,
          remediationVersion,
          lineCommentDTO.getDiffPosition().getNewLineContent(),
          lineCommentDTO.getDiffPosition().getFilePath());
      // Create the line comment body, if possible
      Optional<String> markupOptional = pullRequestFeedbackMarkupService.createLineMarkup(
          lineCommentDTO.getPolicyViolations(), ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString(),
          remediationVersion, codeSuggestion, provider, scmBaseUrl, applicationId, featureBranchScanId,
          productLicense.hasFeature(LicensedFeature.SCM_UX_IMPROVEMENTS));
      markupOptional.ifPresent(lineCommentDTO::setMarkup);
    }
  }

  private Optional<String> createCodeSuggestion(
      SourceControlProvider provider,
      ComponentIdentifier componentIdentifier,
      RemediationVersionDTO remediationVersion,
      String existingContent,
      String filePath)
  {
    String codeSuggestion = null;
    String componentVersion = componentIdentifier.get("version");
    // Temporarily suppress code suggestions on root-level pom.xml files in GitHub repos due to a bug in GitHub
    // See https://sonatype.atlassian.net/browse/SDEV-538
    if (provider == SourceControlProvider.GITHUB && filePath.equals("pom.xml")) {
      return Optional.empty();
    }
    else if (remediationVersion != null && existingContent.contains(componentVersion)) {
      // TODO: handling for non-specific dependency versions: https://sonatype.atlassian.net/browse/SDEV-282.
      codeSuggestion = existingContent.replace(componentVersion, remediationVersion.getVersion());
    }
    return Optional.ofNullable(codeSuggestion);
  }

  /**
   * Builds the list of line comments to be created. The list items are enhanced in subsequent steps
   */
  private void buildLineCommentList(
      final PullRequestLineCommentCreationResult lineCommentCreationResult,
      final Map<ComponentIdentifier, List<DiffPosition>> diffPositionMap,
      final List<PolicyViolation> violationList)
  {
    List<PullRequestLineCommentDTO> lineCommentDTOList = diffPositionMap.entrySet()
        .stream()
        .flatMap(e -> {
          List<PullRequestLineCommentDTO> list = new LinkedList<>();
          for (DiffPosition diffPosition : e.getValue()) {
            list.add(new PullRequestLineCommentDTO(e.getKey(), diffPosition));
          }
          return list.stream();
        })
        .collect(Collectors.toList());

    // add policy violations and component hashes to individual comments
    for (PolicyViolation policyViolation : violationList) {
      for (PullRequestLineCommentDTO lineCommentDTO : lineCommentDTOList) {
        if (lineCommentDTO.getComponentIdentifier().equals(policyViolation.getComponentIdentifier())) {
          lineCommentDTO.addPolicyViolations(policyViolation);
          lineCommentDTO.setHash(policyViolation.getHash());
        }
      }
    }
    lineCommentCreationResult.setPullRequestLineCommentDtoList(lineCommentDTOList);
  }

  /**
   * Deletes all existing line comments for a given PR from the DB and SCM if they exists
   *
   * @param lineCommentCreationResult Collects operation result details
   * @param applicationId The application the PR relates to
   * @param gitRepositoryInfo The repository info the PR relates to
   * @param pullRequestId The pull request id
   */
  private void deleteExistingLineCommentsIfExists(
      final PullRequestLineCommentCreationResult lineCommentCreationResult,
      final String applicationId,
      final GitRepositoryInfo gitRepositoryInfo,
      final int pullRequestId)
  {
    List<SourceControlPullRequestComment> existingLineComments =
        pullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithComponents(applicationId, pullRequestId);

    if (!existingLineComments.isEmpty()) {
      GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
      for (SourceControlPullRequestComment comment : existingLineComments) {
        // Users can delete comments, which will result in 404 on delete, we should handle this and continue
        try {
          pullRequestCommentDAO.delete(comment);
          gitApiClient.deletePullRequestLineComment(comment.getPullRequestCommentId(), pullRequestId,
              comment.getPullRequestCommentVersion());
        }
        catch (IOException e) {
          if (e instanceof HttpResponseException &&
              HttpStatus.SC_NOT_FOUND == ((HttpResponseException) e).getStatusCode())
          {
            log.debug("Deleting pull request line comment with id {} on application {} returned 404, skipping",
                comment.getPullRequestCommentId(), applicationId);
          }
          else {
            String message = String.format("Cannot delete pull request line comment with id %s on application %s",
                comment.getPullRequestCommentId(), applicationId);
            lineCommentCreationResult.addException(new IOException(message, e));
          }
        }
      }
    }
  }
}
