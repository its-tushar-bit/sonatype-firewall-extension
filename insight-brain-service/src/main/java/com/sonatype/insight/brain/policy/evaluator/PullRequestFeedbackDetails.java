/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.git.PullRequestLineCommentDTO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.common.SimpleProjectUri;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final String[] THREAT_IMAGE_ARRAY = new String[]{
      LIGHT_BLUE_BAR, // 0
      DARK_BLUE_BAR, // 1
      YELLOW_BAR, YELLOW_BAR, // 2 - 3
      ORANGE_BAR, ORANGE_BAR, ORANGE_BAR, ORANGE_BAR, // 4 - 7
      RED_BAR, RED_BAR, RED_BAR // 8 - 10
  };

  private static Template policyViolationDiffMDEmbeddedHtmlTemplate;

  private static Template policyViolationDiffMDMinimalHtmlTemplate;

  private final Application app;

  private final ReportEntry bomReportEntry;

  private final PolicyEvaluation featureBranchEvaluation;

  private final PolicyEvaluation defaultBranchEvaluation;

  private final PolicyViolationDiff<PolicyViolation> diff;
  
  private final Map<ComponentIdentifier, String> remediationVersionMap;
  
  private final List<PullRequestLineCommentDTO> pullRequestLineComments;
  
  private final GitRepositoryInfo gitRepositoryInfo;
  
  private final int pullRequestNumber;

  private final String baseUrl;

  private int newViolationsComponentCount;

  private int clearedViolationsComponentCount;

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
      final ReportEntry bomReportEntry,
      final PolicyEvaluation featureBranchEvaluation,
      final PolicyEvaluation defaultBranchEvaluation,
      final PolicyViolationDiff<PolicyViolation> diff,
      final Map<ComponentIdentifier, String> remediationVersionMap,
      final List<PullRequestLineCommentDTO> pullRequestLineComments,
      final GitRepositoryInfo gitRepositoryInfo,
      final int pullRequestNumber, 
      final Application app,
      final String baseUrl)
  {
    Preconditions.checkNotNull(bomReportEntry, "bomReportEntry is required and cannot be null");
    this.bomReportEntry = bomReportEntry;
    Preconditions.checkNotNull(featureBranchEvaluation, "featureBranchEvaluation is required and cannot be null");
    this.featureBranchEvaluation = featureBranchEvaluation;
    Preconditions.checkNotNull(defaultBranchEvaluation, "defaultBranchEvaluation is required and cannot be null");
    this.defaultBranchEvaluation = defaultBranchEvaluation;
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
    this.baseUrl = baseUrl;
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
    //Create a map from component hash to display name
    final Map<String, String> componentDisplayNamesMap = createDisplayNamesMap();
    if (componentDisplayNamesMap.isEmpty()) {
      return "";
    }

    //Policy violations need to be grouped by component, any component not in the bom will not be considered
    final Map<String, List<PolicyViolation>> componentPolicyViolationsMap = diff.hasAppeared() ? 
        getComponentPolicyViolationsMap(diff.getAppeared(), componentDisplayNamesMap) : Collections.emptyMap();
    //Get a map containing the PR feedback for each of the components
    final List<Map<String, Object>> newComponentFeedbackList = getNewComponentFeedbackList(componentPolicyViolationsMap,
        remediationVersionMap, pullRequestLineComments, gitRepositoryInfo, pullRequestNumber, baseUrl);
    newViolationsComponentCount = newComponentFeedbackList.size();

    final Map<String, List<PolicyViolation>> fixedComponentPolicyViolationsMap = diff.hasCleared() ? 
        getComponentPolicyViolationsMap(diff.getCleared(), componentDisplayNamesMap) : Collections.emptyMap();
    //Get a map containing the PR feedback for each of the components
    final List<Map<String, Object>> fixedComponentFeedbackList = 
        getFixedComponentFeedbackList(fixedComponentPolicyViolationsMap, baseUrl);
    clearedViolationsComponentCount = fixedComponentFeedbackList.size();

    //Get a map containing all model values to be used in the template
    final Map<String, Object> modelMap =
        getModelMap(newComponentFeedbackList, fixedComponentFeedbackList, gitRepositoryInfo.provider, baseUrl);

    return TemplateUtils.render(getPolicyTemplate(), modelMap);
  }

  private Template getPolicyTemplate() {
    if (gitRepositoryInfo.provider.supportsEmbeddedHtmlInMarkdown()) {
      return policyViolationDiffMDEmbeddedHtmlTemplate;
    }
    return policyViolationDiffMDMinimalHtmlTemplate;
  }
  
  private Map<String, List<PolicyViolation>> getComponentPolicyViolationsMap(
      final List<PolicyViolation> violations,
      final Map<String, String> componentDisplayNamesMap)
  {
    return violations.stream()
        .filter(policyViolation -> componentDisplayNamesMap.containsKey(policyViolation.getHash()))
        .collect(
            Collectors.groupingBy(
                policyViolation -> componentDisplayNamesMap.get(policyViolation.getHash()))
        );
  }

  /**
   * Gets the display names for all components in the BOM and components in the cleared policy violations section
   * (some of them may not be included in the BOM).
   *
   * @return Returns a map with the component hash as the key and the component display name as the value
   */
  Map<String, String> createDisplayNamesMap() {
    final Map<String, String> componentDisplayNamesMap = new HashMap<>();
    JsonNode bomJson = loadJson();
    if (bomJson != null) {
      bomJson = bomJson.get("aaData");
      if (bomJson != null) {
        final ArrayNode bomJsonArray = (ArrayNode) bomJson;
        bomJsonArray.forEach(jsonNode -> {
          final String hash = JsonUtils.getNullableString(jsonNode.get("hash"));
          componentDisplayNamesMap.put(hash, ComponentDisplayNameUtil.fromJsonNode((ObjectNode) jsonNode).toString());
        });
      }
    }
    if (diff.hasCleared()) {
      // add mappings for the components from the cleared violations section; some may not be included in the bom file
      List<PolicyViolation> cleared = diff.getCleared();
      for (PolicyViolation violation : cleared) {
        String hash = violation.getHash();
        if (violation.getComponentIdentifier() != null && !componentDisplayNamesMap.containsKey(hash)) {
          componentDisplayNamesMap.put(hash,
              ComponentDisplayNameUtil.fromIdentifier(violation.getComponentIdentifier()).toString());
        }
      }
    }
    return componentDisplayNamesMap;
  }

  private JsonNode loadJson() {
    Preconditions.checkNotNull(bomReportEntry.buf, "bom data is required, and cannot be null");
    try {
      return JsonUtils.parse(bomReportEntry.buf);
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
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
  static List<Map<String, Object>> getFixedComponentFeedbackList(
      final Map<String, List<PolicyViolation>> componentPolicyViolationsMap,
      final String baseUrl)
  {
    return componentPolicyViolationsMap
        .entrySet()
        .stream()
        .map(componentEntry -> ImmutableMap.<String, Object>builder()
            .put("componentNameAndVersion", componentEntry.getKey())
            .put("highestThreatLevel",
                getHighestThreatLevel(componentEntry.getValue()))
            .put("policiesViolated", getPoliciesViolatedMap(componentEntry.getValue(), baseUrl, true))
            .build())
        .sorted(
            (o1, o2) -> Integer.compare((Integer) o2.get("highestThreatLevel"), (Integer) o1.get("highestThreatLevel")))
        .collect(Collectors.toList());
  }

  /**
   * Gets a list of feedback items for each of the components that introduced new policy violations
   *
   * @param componentPolicyViolationsMap A map containing policy violations for each component
   * @param remediationVersionMap A map containing suggested remediation version (if one exists) for each component
   * @param pullRequestLineComments A list of newly created line comment details
   * @param baseUrl The baseUrl of the IQ server
   * @return A list of maps, each containing the feedback for a specific component, the components are sorted according
   * to highest threat level on the component
   */
  @VisibleForTesting
  static List<Map<String, Object>> getNewComponentFeedbackList(
      final Map<String, List<PolicyViolation>> componentPolicyViolationsMap,
      final Map<ComponentIdentifier, String> remediationVersionMap,
      final List<PullRequestLineCommentDTO> pullRequestLineComments,
      final GitRepositoryInfo gitRepositoryInfo,
      final int prNumber,
      final String baseUrl)
  {
    return componentPolicyViolationsMap
        .entrySet()
        .stream()
        .map(componentEntry -> ImmutableMap.<String, Object>builder()
            .put("componentNameAndVersion", componentEntry.getKey())
            .put("highestThreatLevel",
                getHighestThreatLevel(componentEntry.getValue()))
            .put("suggestedVersion", getSuggestedVersion(remediationVersionMap, componentEntry.getValue()))
            .put("lineCommentLink",
                getLineCommentLink(pullRequestLineComments, componentEntry.getValue(), gitRepositoryInfo, prNumber))
            .put("policiesViolated", getPoliciesViolatedMap(componentEntry.getValue(), baseUrl, true))
            .build())
        .sorted(
            (o1, o2) -> Integer.compare((Integer) o2.get("highestThreatLevel"), (Integer) o1.get("highestThreatLevel")))
        .collect(Collectors.toList());
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
      Integer scmId = null;
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
      final Integer scmId)
  {
    if (scmId != null && gitRepositoryInfo.getRepositoryUrl().startsWith("http")) {
      if (gitRepositoryInfo.provider == SourceControlProvider.GITHUB) {
        // normalize repository URL
        SimpleProjectUri projectUri = new SimpleProjectUri(gitRepositoryInfo.getRepositoryUrl());
        String repoUrl = projectUri.getCanonicalUri().toString();
        return repoUrl + "pull/" + prNumber + "#discussion_r" + scmId;
      }
    }
    return "";
  }

  private static String getSuggestedVersion(
      final Map<ComponentIdentifier, String> remediationVersionMap,
      final List<PolicyViolation> violationList)
  {
    String version = "";
    ComponentIdentifier identifier = getComponentIdentifier(violationList);
    if (identifier != null) {
      String remediationVersion = remediationVersionMap.get(identifier);
      if (StringUtils.isNotEmpty(remediationVersion)) {
        version = remediationVersion;
      }
    }
    return version;
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
      final String baseUrl)
  {
    return ImmutableMap.<String, Object>builder()
        .put("applicationName", app.getName())
        .put("organizationName", getOrganizationName(app))
        .put("componentList", newComponentFeedbackList)
        .put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(featureBranchEvaluation.getTime()))
        .put("featureBranchStage", StringUtils.capitalize(featureBranchEvaluation.getStageTypeId()))
        .put("defaultBranchStage", StringUtils.capitalize(defaultBranchEvaluation.getStageTypeId()))
        .put("detailedFeatureBranchReportUrl", baseUrl +
            UserInterfaceLinksResource.getReportUrl(app.getPublicId(), featureBranchEvaluation.getScanId()))
        .put("detailedDefaultBranchReportUrl", baseUrl +
            UserInterfaceLinksResource.getReportUrl(app.getPublicId(), defaultBranchEvaluation.getScanId()))
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
}
