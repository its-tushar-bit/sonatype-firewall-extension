/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.discovery.PositionDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.DiffPosition;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.PositionDiscoveryResult;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestLineCommentingService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestLineCommentingService.class);

  static final String LINE_COMMENT_FEATURE = "prLineCommenting";

  private static final String VERSION_KEY = "version";

  private final GitClientFactory gitClientFactory;

  private final SourceControlPullRequestCommentDAO pullRequestCommentDAO;

  private final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService;

  private final ApiComponentRemediationService remediationService;

  private final SourceControlTaskRunner sourceControlTaskRunner;

  private final PositionDiscoveryExecutor positionDiscoveryExecutor;

  private final InsightConfig insightConfig;

  @Inject
  public PullRequestLineCommentingService(
      final GitClientFactory gitClientFactory,
      final SourceControlPullRequestCommentDAO pullRequestCommentDAO,
      final PullRequestFeedbackMarkupService pullRequestFeedbackMarkupService,
      final ApiComponentRemediationService remediationService,
      final SourceControlTaskRunner sourceControlTaskRunner,
      final PositionDiscoveryExecutor positionDiscoveryExecutor,
      final InsightConfig insightConfig)
  {
    this.gitClientFactory = gitClientFactory;
    this.pullRequestCommentDAO = pullRequestCommentDAO;
    this.pullRequestFeedbackMarkupService = pullRequestFeedbackMarkupService;
    this.remediationService = remediationService;
    this.sourceControlTaskRunner = sourceControlTaskRunner;
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
      final int pullRequestId,
      final String branch,
      final String commitHash,
      final String applicationId,
      final String sourcePolicyEvaluationId,
      final String basePolicyEvaluationId)
  {
    List<PullRequestLineCommentDTO> lineCommentList = Collections.emptyList();

    if (insightConfig.isExperimentalFeatureEnabled(LINE_COMMENT_FEATURE) &&
        violationList != null && !violationList.isEmpty()) {

      try {
        // Find all potential source locations to comment on
        LocationDiscoveryResult locationDiscoveryResult =
            doLocationDiscovery(violationList, gitRepositoryInfo, branch, applicationId);

        if (locationDiscoveryResult != null && !locationDiscoveryResult.getLocationMap().isEmpty()) {
          GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);

          // Find the best positions to comment on available in the PR diff
          PositionDiscoveryResult positionDiscoveryResult =
              positionDiscoveryExecutor.execute(locationDiscoveryResult.getLocationMap(), pullRequestId, gitApiClient);

          if (positionDiscoveryResult != null && !positionDiscoveryResult.getDiffPositionsByComponent().isEmpty()) {

            Map<ComponentIdentifier, List<DiffPosition>> diffPositionMap =
                positionDiscoveryResult.getDiffPositionsByComponent();

            //Fetch remediation information for the target component, if available
            Map<ComponentIdentifier, String> remediationVersionMap =
                getRemediationVersionMap(diffPositionMap.keySet(), applicationId);

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
      catch (Exception e) {
        log.error("Cannot create PullRequest line comments", e);
      }
    }
    return lineCommentList;
  }

  /**
   * Given a repository, branch name and a policy violation list retrieve all potential location to comment on.
   * <p>
   * The ecosystem specific location collection steps are executed only if there is at least one component in
   * policy evaluation diff that matches that ecosystem.
   * <p>
   * The output of this step is a map between components (ComponentIdentifier) and a list of
   * potential locations to comment on (RankedSourceLocation).
   */
  private LocationDiscoveryResult doLocationDiscovery(
      final List<PolicyViolation> violationList,
      final GitRepositoryInfo gitRepositoryInfo,
      final String branch,
      final String applicationId)
      throws ExecutionException, InterruptedException
  {
    List<ComponentIdentifier> componentIdentifierSet = violationList.stream()
        .filter(pv -> pv.getComponentIdentifier() != null)
        .map(PolicyViolation::getComponentIdentifier)
        .filter(ci -> ci.getFormat().equalsIgnoreCase(ComponentIdentifier.FORMAT_MAVEN))
        .distinct()
        .collect(Collectors.toList());
    return sourceControlTaskRunner
        .doPullRequestLocationDiscovery(componentIdentifierSet, gitRepositoryInfo, branch, applicationId);
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

          //Add the line comment details to the database
          SourceControlPullRequestComment pullRequestComment = new SourceControlPullRequestComment(
              applicationId, lineCommentDTO.getHash(), pullRequestId, response.getId(),
              sourcePolicyEvaluationId, basePolicyEvaluationId);
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
          lineCommentDTO.getPolicyViolations(), createNameAndVersion(componentIdentifier),
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
   * Returns a map of component identifier and remediation versions for a given set of component identifiers.
   * The map will contains entries only for the components for which a remediation version is found.
   */
  private Map<ComponentIdentifier, String> getRemediationVersionMap(
      final Set<ComponentIdentifier> componentIdentifiers,
      final String ownerId)
  {
    Map<ComponentIdentifier, String> remediationVersionMap = new HashMap<>();
    for (ComponentIdentifier componentIdentifier : componentIdentifiers) {
      Optional<String> suggestedVersion = getRemediationVersion(componentIdentifier, ownerId);
      suggestedVersion.ifPresent(s -> remediationVersionMap.put(componentIdentifier, s));
    }
    return remediationVersionMap;
  }

  /**
   * Gets remediation versions, if any,  for a given component identifier
   */
  private Optional<String> getRemediationVersion(final ComponentIdentifier componentIdentifier, final String ownerId) {
    String nextVersion = null;
    final ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    ApiComponentRemediationDTO remediationDTO = remediationService.getSuggestedRemediationForComponentNoAuth(
        componentDto, OwnerType.APPLICATION, ownerId, null, null, null);
    if (remediationDTO != null) {
      List<ApiVersionChangeOptionDTO> versionChanges = remediationDTO.remediation.versionChanges;
      if (!versionChanges.isEmpty()) {
        nextVersion =
            versionChanges.get(0).getData().getComponent().componentIdentifier.getCoordinates().get(VERSION_KEY);
      }
    }
    return Optional.ofNullable(nextVersion);
  }

  /**
   * Creates a display name for a component identifier
   */
  private String createNameAndVersion(final ComponentIdentifier componentIdentifier) {
    String name;
    switch (componentIdentifier.getFormat()) {
      case ComponentIdentifier.FORMAT_MAVEN:
        name = componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID) + " : " +
            componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) + " : " +
            componentIdentifier.get(ComponentIdentifier.VERSION);
        break;
      case ComponentIdentifier.FORMAT_NPM:
        name = componentIdentifier.get(ComponentIdentifier.NPM_PACKAGE_ID) + " : " +
            componentIdentifier.get(ComponentIdentifier.VERSION);
        break;
      case ComponentIdentifier.FORMAT_ANAME:
        name = componentIdentifier.get(ComponentIdentifier.ANAME_NAME) + " : " +
            componentIdentifier.get(ComponentIdentifier.VERSION);
        break;
      default:
        name = componentIdentifier.toString();
    }
    return name;
  }
}
