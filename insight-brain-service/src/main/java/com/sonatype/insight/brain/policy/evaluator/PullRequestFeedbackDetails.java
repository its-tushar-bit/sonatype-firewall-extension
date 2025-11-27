/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesUtilsService;
import com.sonatype.insight.brain.git.PullRequestLineCommentDTO;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.SourceControlComponentDetails;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.dto.BaseProjectUrl;
import com.sonatype.nexus.scm.bitbucket.dto.BitbucketServerProjectUrl;
import com.sonatype.nexus.scm.github.dto.GitHubProjectUrl;
import com.sonatype.nexus.scm.gitlab.dto.GitlabProjectUrl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import freemarker.template.Template;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static java.util.stream.Collectors.toList;

/**
 * Constructs a pull request comment given a policy violation diff
 */
public class PullRequestFeedbackDetails
    extends PullRequestDetailsBase
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestFeedbackDetails.class);

  private static final String LIGHT_BLUE_BAR = "light-blue-bar.png";

  private static final String DARK_BLUE_BAR = "dark-blue-bar.png";

  private static final String YELLOW_BAR = "yellow-bar.png";

  private static final String ORANGE_BAR = "orange-bar.png";

  private static final String RED_BAR = "red-bar.png";

  private static final String BLANK_LOGO = "blank.png";

  private static final String D_LOGO = "d-logo.png";

  private static final String T_LOGO = "t-logo.png";

  private static final String[] THREAT_IMAGE_ARRAY = new String[]{
      LIGHT_BLUE_BAR, // 0
      DARK_BLUE_BAR, // 1
      YELLOW_BAR, YELLOW_BAR, // 2 - 3
      ORANGE_BAR, ORANGE_BAR, ORANGE_BAR, ORANGE_BAR, // 4 - 7
      RED_BAR, RED_BAR, RED_BAR // 8 - 10
  };

  private static final int MAX_BITBUCKET_DESCRIPTION_COMPONENTS = 10;

  private static Template policyViolationDiffMDEmbeddedHtmlTemplate;

  private static Template policyViolationDiffMDMinimalHtmlTemplate;

  private final Application app;

  private final SourceControlComponentDetails sourceControlComponentDetails;

  private final PolicyEvaluation featureBranchEvaluation;

  private final PolicyEvaluation baseBranchEvaluation;

  private final PolicyViolationDiff<PolicyViolation> diff;

  private final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap;

  private final List<PullRequestLineCommentDTO> pullRequestLineComments;

  private final GitRepositoryInfo gitRepositoryInfo;

  private final int pullRequestNumber;

  private final String iqBaseUrl;

  private int newViolationsComponentCount;

  private int clearedViolationsComponentCount;

  private final boolean scmImprovementsEnabled;

  private final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  static {
    try {
      policyViolationDiffMDEmbeddedHtmlTemplate =
          TemplateUtils.createFreemarkerConfig().getTemplate("pullrequest-feedback-violations.ftl");
      policyViolationDiffMDMinimalHtmlTemplate =
          TemplateUtils.createFreemarkerConfig().getTemplate("pullrequest-feedback-minimal-markdown-violations.ftl");
    }
    catch (IOException e) {
      log.error("Error loading threats template: {}", e.getMessage(), e);
    }
  }

  public PullRequestFeedbackDetails(
      final SourceControlComponentDetails sourceControlComponentDetails,
      final PolicyEvaluation featureBranchEvaluation,
      final PolicyEvaluation baseBranchEvaluation,
      final PolicyViolationDiff<PolicyViolation> diff,
      final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      final List<PullRequestLineCommentDTO> pullRequestLineComments,
      final GitRepositoryInfo gitRepositoryInfo,
      final int pullRequestNumber,
      final Application app,
      final String iqBaseUrl,
      final boolean scmImprovementsEnabled,
      final OrganizationDAO organizationDAO,
      final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService,
      final boolean reducedSecurityData)
  {
    super(organizationDAO, reducedSecurityData);
    Preconditions
        .checkNotNull(sourceControlComponentDetails, "sourceControlComponentDetails is required and cannot be null");
    this.sourceControlComponentDetails = sourceControlComponentDetails;
    Preconditions.checkNotNull(featureBranchEvaluation, "featureBranchEvaluation is required and cannot be null");
    this.featureBranchEvaluation = featureBranchEvaluation;
    Preconditions.checkNotNull(baseBranchEvaluation, "baseBranchEvaluation is required and cannot be null");
    this.baseBranchEvaluation = baseBranchEvaluation;
    Preconditions.checkNotNull(diff, "diff is required and cannot be null");
    this.diff = diff;
    Preconditions.checkNotNull(remediationVersionMap, "remediationVersionMap is required and cannot be null");
    this.remediationVersionMap = remediationVersionMap;
    Preconditions.checkNotNull(pullRequestLineComments, "pullRequestLineComments is required and cannot be null");
    this.pullRequestLineComments = pullRequestLineComments;
    Preconditions.checkNotNull(gitRepositoryInfo, "gitRepositoryInfo is required and cannot be null");
    this.gitRepositoryInfo = gitRepositoryInfo;
    Preconditions.checkNotNull(app, "app is required and cannot be null");
    this.app = app;
    this.pullRequestNumber = pullRequestNumber;
    this.iqBaseUrl = iqBaseUrl;
    this.scmImprovementsEnabled = scmImprovementsEnabled;
    this.developmentPrioritiesUtilsService = developmentPrioritiesUtilsService;
    Preconditions.checkNotNull(gitRepositoryInfo.provider, "provider is required and cannot be null");
  }

  /**
   * Renders the template and returns the content
   *
   * @return An optional variable containing the Markdown-formatted contents of the Pull Request Comment, will be empty
   * if no new violations or no components available
   */
  public Optional<String> renderTemplateAndGetContents() throws IOException {
    final String contents = constructContents();
    return contents.equals("") ? Optional.empty() : Optional.of(contents);
  }

  /**
   * Constructs the contents for the PR feedback, if no new violation are available or no components in the bom, it will
   * be an empty string
   *
   * @return An optional variable containing the PR feedback contents
   */
  private String constructContents() throws IOException {
    final List<PolicyViolation> introducedPolicyViolations = getIntroducedPolicyViolations();
    final List<PolicyViolation> fixedPolicyViolations = getFixedPolicyViolations();

    //Policy violations grouped by component hash, any component not in the bom will not be considered
    final Map<String, List<PolicyViolation>> componentPolicyViolationsMap = !introducedPolicyViolations.isEmpty() ?
        getComponentPolicyViolationsMap(introducedPolicyViolations) : Collections.emptyMap();

    //Get a map containing the PR feedback for each of the components
    final List<Map<String, Object>> newComponentFeedbackList = getNewComponentFeedbackList(componentPolicyViolationsMap,
        remediationVersionMap, pullRequestLineComments, gitRepositoryInfo, pullRequestNumber, iqBaseUrl);
    newViolationsComponentCount = newComponentFeedbackList.size();

    final Map<String, List<PolicyViolation>> fixedComponentPolicyViolationsMap = !fixedPolicyViolations.isEmpty() ?
        getComponentPolicyViolationsMap(fixedPolicyViolations) : Collections.emptyMap();
    //Get a map containing the PR feedback for each of the components
    final List<Map<String, Object>> fixedComponentFeedbackList =
        getFixedComponentFeedbackList(fixedComponentPolicyViolationsMap, iqBaseUrl);
    clearedViolationsComponentCount = fixedComponentFeedbackList.size();

    final boolean hasNoViolationsInPR = CollectionUtils.isEmpty(diff.getAppeared());

    //Get a map containing all model values to be used in the template
    final Map<String, Object> modelMap =
        getModelMap(
            newComponentFeedbackList,
            fixedComponentFeedbackList,
            gitRepositoryInfo.provider,
            iqBaseUrl,
            hasNoViolationsInPR);

    return TemplateUtils.render(getPolicyTemplate(), modelMap);
  }

  private List<PolicyViolation> getFixedPolicyViolations() {
    // all violations that were cleared and are not in the new version of the component
    return diff.getCleared()
        .stream()
        .filter(policyViolationA -> diff
              .getAppeared()
              .stream()
              .noneMatch(policyViolationB -> policyViolationsTheSame(policyViolationA, policyViolationB))
        )
        .collect(toList());
  }

  private List<PolicyViolation> getIntroducedPolicyViolations() {
    return diff.getAppeared()
        .stream()
        .filter(policyViolationA -> diff
              .getCleared()
              .stream()
              .noneMatch(policyViolationB -> policyViolationsTheSame(policyViolationA, policyViolationB))
        )
        .collect(toList());
  }

  private boolean policyViolationsTheSame(PolicyViolation policyViolationA, PolicyViolation policyViolationB) {
    if (policyViolationA.getComponentIdentifier() == null || policyViolationB.getComponentIdentifier() == null) {
      return false;
    }

    final ComponentIdentifier versionlessIdentifierA = policyViolationA
        .getComponentIdentifier()
        .createAlternativeVersion(null);
    final ComponentIdentifier versionlessIdentifierB = policyViolationB
        .getComponentIdentifier()
        .createAlternativeVersion(null);

    if (policyViolationA.getConstraintFactsId() != null) {
      return versionlessIdentifierA.equals(versionlessIdentifierB) &&
          policyViolationA.getConstraintFactsId().equals(policyViolationB.getConstraintFactsId());
    }
    else {
      return versionlessIdentifierA.equals(versionlessIdentifierB) &&
          policyViolationA.getConstraintFactsJson().equals(policyViolationB.getConstraintFactsJson());
    }
  }

  private Template getPolicyTemplate() {
    if (gitRepositoryInfo.provider.supportsEmbeddedHtmlInMarkdown(gitRepositoryInfo.getRepositoryUrl())) {
      return policyViolationDiffMDEmbeddedHtmlTemplate;
    }
    return policyViolationDiffMDMinimalHtmlTemplate;
  }

  /**
   * Returns a map between component hashes and associated lists of policy violations
   */
  private Map<String, List<PolicyViolation>> getComponentPolicyViolationsMap(
      final List<PolicyViolation> violations)
  {
    return violations.stream()
        .filter(policyViolation -> sourceControlComponentDetails.getComponentInfo(policyViolation.getHash()) != null)
        .collect(Collectors.groupingBy(AbstractPolicyViolation::getHash));
  }

  /**
   * Gets a list of feedback items for each of the components with fixed violations
   *
   * @param componentPolicyViolationsMap A map containing policy violations for each component
   * @param baseUrl The baseUrl of the IQ server
   * @return A list of maps, each containing the feedback for a specific component, the components are sorted according
   * to highest threat level on the component
   */
  @VisibleForTesting
  List<Map<String, Object>> getFixedComponentFeedbackList(
      final Map<String, List<PolicyViolation>> componentPolicyViolationsMap,
      final String baseUrl)
  {
    return componentPolicyViolationsMap
        .entrySet()
        .stream()
        .map(componentEntry -> ImmutableMap.<String, Object>builder()
            .put("componentNameAndVersion",
                sourceControlComponentDetails.getComponentInfo(componentEntry.getKey()).getDisplayName())
            .put("highestThreatLevel",
                getHighestThreatLevel(componentEntry.getValue()))
            .put("policiesViolated",
                getPoliciesViolatedMap(componentEntry.getValue(), baseUrl, true, reducedSecurityData, app.getPublicId(),
                    featureBranchEvaluation.getScanId()))
            .build())
        .sorted(
            (o1, o2) -> Integer.compare((Integer) o2.get("highestThreatLevel"), (Integer) o1.get("highestThreatLevel")))
        .collect(toList());
  }

  /**
   * Gets a list of feedback items for each of the components that introduced new policy violations
   *
   * @param componentPolicyViolationsMap A map containing policy violations for each component grouped by hash
   * @param remediationVersionMap A map containing suggested remediation version (if one exists) for each component
   * @param pullRequestLineComments A list of newly created line comment details
   * @param baseUrl The baseUrl of the IQ server
   * @return A list of maps, each containing the feedback for a specific component, the components are sorted according
   * to highest threat level on the component
   */
  @VisibleForTesting
  List<Map<String, Object>> getNewComponentFeedbackList(
      final Map<String, List<PolicyViolation>> componentPolicyViolationsMap,
      final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      final List<PullRequestLineCommentDTO> pullRequestLineComments,
      final GitRepositoryInfo gitRepositoryInfo,
      final int prNumber,
      final String baseUrl)
  {
    return componentPolicyViolationsMap
        .entrySet()
        .stream()
        .map(componentEntry -> {
          RemediationVersionDTO remediationVersionDTO =
              getSuggestedVersion(remediationVersionMap, componentEntry.getValue());
          String suggestedVersion = remediationVersionDTO != null ? remediationVersionDTO.getVersion() : "";
          int breakingChangesCount = -1;
          String remediationTypeDisplayName = "";
          boolean remediationForDependencies = false;
          if (remediationVersionDTO != null) {
            if (remediationVersionDTO.getBreakingChangesCount() != null) {
              breakingChangesCount = remediationVersionDTO.getBreakingChangesCount();
            }
            ApiVersionChangeOptionType remediationType = remediationVersionDTO.getRemediationType();
            if (remediationType != null) {
              remediationTypeDisplayName = remediationType.getDisplayName();
              remediationForDependencies = ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES
                  .equals(remediationType);
            }
          }

          final ImmutableMap.Builder<String, Object> modelMapBuilder = ImmutableMap.<String, Object>builder()
              .put("componentNameAndVersion",
                  sourceControlComponentDetails.getComponentInfo(componentEntry.getKey()).getDisplayName())
              .put("dependencyLogo",
                  sourceControlComponentDetails.getComponentInfo(componentEntry.getKey()).getDirectDependency() ==
                      null ? BLANK_LOGO :
                      sourceControlComponentDetails.getComponentInfo(componentEntry.getKey()).getDirectDependency() ?
                          D_LOGO : T_LOGO
              )
              .put("highestThreatLevel",
                  getHighestThreatLevel(componentEntry.getValue()))
              .put("suggestedVersion", suggestedVersion)
              .put("remediationForDependencies", remediationForDependencies)
              .put("breakingChangesCount", breakingChangesCount)
              .put("remediationTypeDisplayName", remediationTypeDisplayName)
              .put("lineCommentLink",
                  getLineCommentLink(pullRequestLineComments, componentEntry.getValue(), gitRepositoryInfo, prNumber))
              .put("policiesViolated",
                  getPoliciesViolatedMap(componentEntry.getValue(), baseUrl, true, reducedSecurityData,
                      app.getPublicId(),
                      featureBranchEvaluation.getScanId()));

          maybePutComponentHash(modelMapBuilder, componentEntry.getValue());
          return modelMapBuilder.build();
        })
        .sorted(
            (o1, o2) -> Integer.compare((Integer) o2.get("highestThreatLevel"), (Integer) o1.get("highestThreatLevel")))
        .collect(toList());
  }

  private static String getLineCommentLink(
      final List<PullRequestLineCommentDTO> pullRequestLineComments,
      final List<PolicyViolation> violationList,
      final GitRepositoryInfo gitRepositoryInfo,
      final Integer prNumber)
  {
    String link = "";
    ComponentIdentifier identifier = getComponentIdentifier(violationList);
    if (identifier != null) {
      Long scmId = null;
      for (PullRequestLineCommentDTO lineComment : pullRequestLineComments) {
        if (lineComment.getComponentIdentifier().equals(identifier)) {
          scmId = lineComment.getScmId();
          break;
        }
      }
      link = createLink(gitRepositoryInfo, prNumber, scmId);
    }
    return link;
  }

  private static String createLink(
      final GitRepositoryInfo gitRepositoryInfo,
      final Integer prNumber,
      final Long scmId)
  {
    String linkUrl = "";
    if (scmId != null) {
      BaseProjectUrl projectUrl;
      String repositoryUrl = gitRepositoryInfo.normalizedRepositoryUrl;
      switch (gitRepositoryInfo.provider) {
        case GITHUB:
          projectUrl = new GitHubProjectUrl(repositoryUrl);
          linkUrl = projectUrl.getCanonicalUrl().toString() + "pull/" + prNumber + "#discussion_r" + scmId;
          break;
        case GITLAB:
          projectUrl = new GitlabProjectUrl(repositoryUrl);
          linkUrl = projectUrl.getCanonicalUrl().toString() + "-/merge_requests/" + prNumber + "#note_" + scmId;
          break;
        case BITBUCKET:
          projectUrl = new BitbucketServerProjectUrl(repositoryUrl);
          String context = StringUtils.isBlank(projectUrl.getContext()) ? "" : "/" + projectUrl.getContext();
          linkUrl = context + "/projects/" + projectUrl.getNamespace() + "/repos/" + projectUrl.getProject() +
              "/pull-requests/" + prNumber + "/overview?commentId=" + scmId;
          break;
        default:
          linkUrl = "";
      }
    }
    return linkUrl;
  }

  private static RemediationVersionDTO getSuggestedVersion(
      final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      final List<PolicyViolation> violationList)
  {
    ComponentIdentifier identifier = getComponentIdentifier(violationList);
    if (identifier != null) {
      return remediationVersionMap.get(identifier);
    }
    return null;
  }

  private static ComponentIdentifier getComponentIdentifier(final List<PolicyViolation> violationList) {
    ComponentIdentifier identifier = null;
    if (violationList != null && !violationList.isEmpty()) {
      identifier = violationList.get(0).getComponentIdentifier();
    }
    return identifier;
  }

  /**
   * Gets the main model map needed to render the template
   * @param newComponentFeedbackList     The list containing mappings of feedback for components introducing violations
   * @param fixedComponentFeedbackList   The list containing mappings of feedback for components fixing violations
   * @param baseUrl                      The baseUrl of the IQ server
   */
  private Map<String, Object> getModelMap(
      final List<Map<String, Object>> newComponentFeedbackList,
      final List<Map<String, Object>> fixedComponentFeedbackList,
      final SourceControlProvider provider,
      final String baseUrl,
      final boolean hasNoViolationsInPR)
  {
    return ImmutableMap.<String, Object>builder()
        .put("applicationName", app.getName())
        .put("organizationName", getOrganizationName(app))
        .put("componentList", newComponentFeedbackList)
        .put("maxComponents", provider == BITBUCKET ? MAX_BITBUCKET_DESCRIPTION_COMPONENTS : Integer.MAX_VALUE)
        .put("maxFixedComponents", provider == BITBUCKET ? MAX_BITBUCKET_DESCRIPTION_COMPONENTS : Integer.MAX_VALUE)
        .put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(featureBranchEvaluation.getTime()))
        .put("featureBranchStage", StringUtils.capitalize(featureBranchEvaluation.getStageTypeId()))
        .put("baseBranchStage", StringUtils.capitalize(baseBranchEvaluation.getStageTypeId()))
        .put("baseFeatureBranchURL", baseUrl +
            UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), featureBranchEvaluation.getScanId()))
        .put("detailedFeatureBranchReportUrl", baseUrl +
            UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), featureBranchEvaluation.getScanId()) +
            "?source=pr-commenting")
        .put("detailedBaseBranchReportUrl", baseUrl +
            UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), baseBranchEvaluation.getScanId()) +
            "?source=pr-commenting")
        .put("featureBranchPrioritiesUrl", UrlUtils.appendUrlPaths(baseUrl,
            UserInterfaceLinksHelper.getPrioritiesUrl(app.getPublicId(), featureBranchEvaluation.getScanId())))
        .put("shouldIncludePrioritiesReport", shouldIncludePrioritiesReport())
        .put("baseIqUrl", baseUrl)
        .put("policiesViolatedCount",
            newComponentFeedbackList.stream().mapToInt(item -> ((List<?>) item.get("policiesViolated")).size()).sum()
        )
        .put("fixedComponentList", fixedComponentFeedbackList)
        .put("fixedPolicyViolationsCount",
            fixedComponentFeedbackList.stream().mapToInt(item -> ((List<?>) item.get("policiesViolated")).size()).sum()
        )
        .put("threatImageArray", THREAT_IMAGE_ARRAY)
        .put("provider", provider)
        .put("scmChangesEnabled", scmImprovementsEnabled)
        .put("hasNoViolationsInPR", hasNoViolationsInPR)
        .build();
  }

  public int getNewViolationsComponentCount() {
    return newViolationsComponentCount;
  }

  public int getClearedViolationsComponentCount() {
    return clearedViolationsComponentCount;
  }

  // package visibility for PR Line Feedback access
  static String getImageForThreatLevel(final int threatLevel) {
    if (threatLevel < 0 || threatLevel > 10) {
      return THREAT_IMAGE_ARRAY[0];
    }
    return THREAT_IMAGE_ARRAY[threatLevel];
  }

  private static void maybePutComponentHash(
      final ImmutableMap.Builder<String, Object> modelMapBuilder,
      final List<PolicyViolation> policyViolations)
  {
    extractFirstComponentHash(policyViolations)
        .ifPresent(hash -> modelMapBuilder.put( "componentScanHash" , hash));
  }

  private static Optional<String> extractFirstComponentHash(final List<PolicyViolation> violations) {
    return violations.stream().map(AbstractPolicyViolation::getHash).findFirst();
  }

  private boolean shouldIncludePrioritiesReport() {
    return developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled();
  }
}
