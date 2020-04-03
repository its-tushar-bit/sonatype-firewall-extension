/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import freemarker.template.Template;

/**
 * Constructs a pull request comment given a policy violation diff
 */
public class PullRequestFeedbackDetails
    extends PullRequestDetailsBase
{
  private static Template policyViolationDiffTemplate;

  private final Application app;

  private final ReportEntry bomReportEntry;

  private final PolicyEvaluation featureBranchEvaluation;

  private final PolicyEvaluation defaultBranchEvaluation;

  private final PolicyViolationDiff<PolicyViolation> diff;

  private final String baseUrl;

  public PullRequestFeedbackDetails(
      final ReportEntry bomReportEntry,
      final PolicyEvaluation featureBranchEvaluation,
      final PolicyEvaluation defaultBranchEvaluation,
      final PolicyViolationDiff<PolicyViolation> diff,
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
    Preconditions.checkNotNull(app, "app is required and cannot be null");
    this.app = app;
    this.baseUrl = baseUrl;
  }

  private static synchronized Template getPolicyViolationDiffTemplate() throws IOException {
    if (policyViolationDiffTemplate == null) {
      policyViolationDiffTemplate =
          TemplateUtils.createFreemarkerConfig().getTemplate("pullrequest-feedback-violations.ftl");
    }
    return policyViolationDiffTemplate;
  }

  /**
   * Renders the template and returns the content
   *
   * @return An optional variable containing the Markdown-formatted contents of the Pull Request Comment, will be empty
   * if no new violations or no components available
   * @throws IOException
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
   * @throws IOException
   */
  private String constructContents() throws IOException {
    //Create a map from component hash to display name
    final Map<String, String> componentDisplayNamesMap = getDisplayNamesMapFromBom();
    if (componentDisplayNamesMap.isEmpty()) {
      return "";
    }

    //Policy violations need to be grouped by component, any component not in the bom will not be considered
    Preconditions.checkNotNull(diff.getAppeared(), "new violations data is required, and cannot be null");
    final Map<String, List<PolicyViolation>> componentPolicyViolationsMap =
        diff.getAppeared()
            .stream()
            .filter(policyViolation -> componentDisplayNamesMap.containsKey(policyViolation.getHash()))
            .collect(
                Collectors.groupingBy(
                    policyViolation -> componentDisplayNamesMap.get(policyViolation.getHash()))
            );
    //Get a map containing the PR feedback for each of the components
    final List<Map<String, Object>> componentFeedbackList = getComponentFeedbackList(componentPolicyViolationsMap,
        baseUrl);
    //Get a map containing all model values to be used in the template
    final Map<String, Object> modelMap =
        getModelMap(componentPolicyViolationsMap, componentFeedbackList, baseUrl, diff.getCleared());

    return TemplateUtils.render(getPolicyViolationDiffTemplate(), modelMap);
  }

  /**
   * Gets the display names for all components in the BOM
   *
   * @return Returns a map with the component hash as the key and the component display name as the value
   */
  Map<String, String> getDisplayNamesMapFromBom() {
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
   * Gets a list of feedback items for each of the components
   *
   * @param componentPolicyViolationsMap A map containing policy violations for each component
   * @param baseUrl                      The baseUrl of the IQ server
   * @return A list of maps, each containing the feedback for a specific component, the components are sorted according
   * to highest threat level on the component
   */
  @VisibleForTesting
  static List<Map<String, Object>> getComponentFeedbackList(
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
            .put("policiesViolated", getPoliciesViolatedMap(componentEntry.getValue(), baseUrl))
            .build())
        .sorted(
            (o1, o2) -> Integer.compare((Integer) o2.get("highestThreatLevel"), (Integer) o1.get("highestThreatLevel")))
        .collect(Collectors.toList());
  }

  /**
   * Gets the main model map needed to render the template
   * @param componentPolicyViolationsMap The mapping of components to policy violations
   * @param componentFeedbackList        The list containing mappings of feedback for each component
   * @param baseUrl                      The baseUrl of the IQ server
   */
  private Map<String, Object> getModelMap(
      final Map<String, List<PolicyViolation>> componentPolicyViolationsMap,
      final List<Map<String, Object>> componentFeedbackList,
      final String baseUrl,
      final List<PolicyViolation> cleared)
  {
    return ImmutableMap.<String, Object>builder()
        .put("applicationName", app.getName())
        .put("organizationName", getOrganizationName(app))
        .put("componentList", componentFeedbackList)
        .put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(featureBranchEvaluation.getTime()))
        .put("stage", featureBranchEvaluation.getStageTypeId())
        .put("detailedFeatureBranchReportUrl", baseUrl +
            UserInterfaceLinksResource.getReportUrl(app.getPublicId(), featureBranchEvaluation.getScanId()))
        .put("detailedDefaultBranchReportUrl", baseUrl +
            UserInterfaceLinksResource.getReportUrl(app.getPublicId(), defaultBranchEvaluation.getScanId()))
        .put("baseIqUrl", baseUrl)
        .put("policiesViolatedCount",
            componentPolicyViolationsMap.entrySet()
                .stream()
                .flatMap(entry ->
                    entry.getValue()
                        .stream()
                        .map(policyViolation -> String.format("%s|%s", entry.getKey(), policyViolation.getId())))
                .collect(Collectors.toSet()).size())
        .put("fixedPolicyViolationsCount", cleared == null ? 0 : cleared.size())
        .build();
  }
}
