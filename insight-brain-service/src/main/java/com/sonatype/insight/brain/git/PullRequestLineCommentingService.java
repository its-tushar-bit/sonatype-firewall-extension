/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.discovery.PositionDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.DiffPosition;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.PositionDiscoveryResult;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;

import org.apache.commons.collections.CollectionUtils;
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

  private final PullRequestLocationDiscoveryService locationDiscoveryService;

  private final PositionDiscoveryExecutor positionDiscoveryExecutor;

  private final InsightConfig insightConfig;

  @Inject
  public PullRequestLineCommentingService(
      final GitClientFactory gitClientFactory,
      final SourceControlPullRequestCommentDAO pullRequestCommentDAO,
      final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService,
      final PullRequestLocationDiscoveryService locationDiscoveryService,
      final PositionDiscoveryExecutor positionDiscoveryExecutor,
      final InsightConfig insightConfig)
  {
    this.gitClientFactory = gitClientFactory;
    this.pullRequestCommentDAO = pullRequestCommentDAO;
    this.pullRequestFeedbackMarkupService = pullRequestFeedbackMarkupService;
    this.locationDiscoveryService = locationDiscoveryService;
    this.positionDiscoveryExecutor = positionDiscoveryExecutor;
    this.insightConfig = insightConfig;
  }

  /**
   * This method encapsulates the complete flow for pull request line commenting of policy violation diffs between the
   * development branch commit that triggered the policy evaluation (which then issued this event) and the most recently
   * available policy evaluation for the source control configured base branch for the associated application.
   * @return list of PR line comments successfully created
   */
  public List<PullRequestLineCommentDTO> createPullRequestLineComments(
      final List<PolicyViolation> violationList,
      final GitRepositoryInfo gitRepositoryInfo,
      final Map<ComponentIdentifier, String> remediationVersionMap,
      final int pullRequestId,
      final String branch,
      final String commitHash,
      final String applicationId,
      final String sourcePolicyEvaluationId,
      final String basePolicyEvaluationId)
  {
    if (!insightConfig.isFeatureEnabled(Feature.PR_LINE_COMMENTING) ||
        !gitRepositoryInfo.getProvider().supportsPullRequestLineCommenting()) {
      return Collections.emptyList();
    }

    List<PullRequestLineCommentDTO> lineCommentList = Collections.emptyList();
    try {
      deleteExistingLineCommentsIfExists(applicationId, gitRepositoryInfo, pullRequestId);
      if (!CollectionUtils.isEmpty(violationList)) {
        // Find all potential source locations to comment on
        LocationDiscoveryResult locationDiscoveryResult = locationDiscoveryService.doLocationDiscovery(
            violationList, gitRepositoryInfo, branch, applicationId);

        if (locationDiscoveryResult != null && !locationDiscoveryResult.getLocationMap().isEmpty()) {
          GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);

          // Find the best positions to comment on available in the PR diff
          PositionDiscoveryResult positionDiscoveryResult =
              positionDiscoveryExecutor.execute(locationDiscoveryResult.getLocationMap(), pullRequestId, gitApiClient);

          if (positionDiscoveryResult != null && !positionDiscoveryResult.getDiffPositionsByComponent().isEmpty()) {

            Map<ComponentIdentifier, List<DiffPosition>> diffPositionMap =
                positionDiscoveryResult.getDiffPositionsByComponent();

            // Build a list of line comments to be created
            lineCommentList = buildLineCommentList(diffPositionMap, violationList);

            addMarkupToLineComments(lineCommentList, remediationVersionMap);

            createLineComments(lineCommentList, gitApiClient, pullRequestId, commitHash,
                sourcePolicyEvaluationId, basePolicyEvaluationId, applicationId);

            // Filter out unsuccessful comment attempts
            lineCommentList = lineCommentList.stream().filter(i -> i.getScmId() != null).collect(Collectors.toList());
          }
        }
      }
    }
    catch (Exception e) {
      log.error("Cannot create PullRequest line comments", e);
    }

    return lineCommentList;
  }

  /**
   * Creates the line comments in the target SCM and records them in the database.
   */
  private void createLineComments(
      final List<PullRequestLineCommentDTO> lineCommentList,
      final GitApiClient gitApiClient,
      final int pullRequestId,
      final String commitHash,
      final String sourcePolicyEvaluationId,
      final String basePolicyEvaluationId,
      final String applicationId)
  {
    int totalCount = 0;
    int successfulCount = 0;
    for (PullRequestLineCommentDTO lineCommentDTO : lineCommentList) {
      if (lineCommentDTO.hasMarkup()) {
        totalCount++;
        try {
          //Create the line comment in GitHub
          CommentResponse response = gitApiClient
              .createPullRequestLineComment(pullRequestId, lineCommentDTO.getMarkup(), commitHash,
                  lineCommentDTO.getDiffPosition().getFilePath(), lineCommentDTO.getDiffPosition().getDiffPosition());
          lineCommentDTO.setScmId(response.getId());
          lineCommentDTO.setScmVersion(response.getVersion());

          //Add the line comment details to the database
          SourceControlPullRequestComment pullRequestComment = new SourceControlPullRequestComment(
              applicationId, lineCommentDTO.getHash(), pullRequestId, response.getId(),
              response.getVersion(), sourcePolicyEvaluationId, basePolicyEvaluationId);
          pullRequestCommentDAO.insert(pullRequestComment);

          successfulCount++;
        }
        catch (IOException e) {
          log.error("Cannot create PR line comment", e);
        }
      }
    }
    log.info("Pull request line comments created {} out of {} attempted for application '{}' and pull request '{}'",
        successfulCount, totalCount, applicationId, pullRequestId);
  }

  /**
   * Adds content to the provided line comment list
   */
  private void addMarkupToLineComments(
      final List<PullRequestLineCommentDTO> lineCommentList,
      final Map<ComponentIdentifier, String> remediationVersionMap)
  {
    for (PullRequestLineCommentDTO lineCommentDTO : lineCommentList) {
      ComponentIdentifier componentIdentifier = lineCommentDTO.getComponentIdentifier();
      //Create the line comment body, if possible
      Optional<String> markupOptional = pullRequestFeedbackMarkupService.createLineMarkup(
          lineCommentDTO.getPolicyViolations(), ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString(),
          remediationVersionMap.get(componentIdentifier));
      markupOptional.ifPresent(lineCommentDTO::setMarkup);
    }
  }

  /**
   * Builds the list of line comments to be created. The list items are enhanced in subsequent steps
   */
  private List<PullRequestLineCommentDTO> buildLineCommentList(
      final Map<ComponentIdentifier, List<DiffPosition>> diffPositionMap,
      final List<PolicyViolation> violationList)
  {
    List<PullRequestLineCommentDTO> lineCommentDTOList = diffPositionMap.entrySet().stream()
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
    return lineCommentDTOList;
  }

  /**
   * Deletes all existing line comments for a given PR from the DB and SCM if they exists
   *
   * @param applicationId     The application the PR relates to
   * @param gitRepositoryInfo The repository info the PR relates to
   * @param pullRequestId     The pull request id
   */
  private void deleteExistingLineCommentsIfExists(
      final String applicationId,
      final GitRepositoryInfo gitRepositoryInfo,
      final int pullRequestId) throws IOException
  {
    List<SourceControlPullRequestComment> existingLineComments =
        pullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithComponents(applicationId, pullRequestId);

    if (!existingLineComments.isEmpty()) {
      GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
      for (SourceControlPullRequestComment comment : existingLineComments) {
        //Users can delete comments, which will result in 404 on delete, we should handle this and continue
        try {
          gitApiClient.deletePullRequestLineComment(comment.getPullRequestCommentId());
          pullRequestCommentDAO.delete(comment);
        }
        catch (HttpResponseException e) {
          if (HttpStatus.SC_NOT_FOUND == e.getStatusCode()) {
            log.debug("Deleting pull request with id {} on application {} returned 404, skipping", pullRequestId,
                applicationId);
          }
          else {
            throw e;
          }
        }
      }
    }
  }
}
