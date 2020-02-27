/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
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

  private final PolicyEvaluation toEvaluation;

  private final PolicyViolationDiff<PolicyViolation> diff;

  private final String contents;

  public PullRequestFeedbackDetails(
      final ReportEntry bomReportEntry,
      final PolicyEvaluation toEvaluation,
      final PolicyViolationDiff<PolicyViolation> diff,
      final Application app,
      final String baseUrl) throws IOException
  {
    Preconditions.checkNotNull(bomReportEntry, "bomReportEntry is required and cannot be null");
    this.bomReportEntry = bomReportEntry;
    Preconditions.checkNotNull(toEvaluation, "toEvaluation is required and cannot be null");
    this.toEvaluation = toEvaluation;
    Preconditions.checkNotNull(diff, "diff is required and cannot be null");
    this.diff = diff;
    Preconditions.checkNotNull(app, "app is required and cannot be null");
    this.app = app;
    contents = constructContents(baseUrl);
  }

  private static synchronized Template getPolicyViolationDiffTemplate() throws IOException {
    if (policyViolationDiffTemplate == null) {
      policyViolationDiffTemplate =
          TemplateUtils.createFreemarkerConfig().getTemplate("pullrequest-feedback-violations.ftl");
    }
    return policyViolationDiffTemplate;
  }

  /**
   * The Markdown-formatted contents of the Pull Request Comment, will be empty if no new violations or no components
   * available
   */
  public Optional<String> getContents() {
    return contents.equals("") ? Optional.empty() : Optional.of(contents);
  }

  /**
   * Constructs the contents for the PR feedback, if no new violation are available or no components in the bom, it will
   * be an empty string
   *
   * @param baseUrl The baseUrl of the IQ server
   * @return An optional variable containing the PR feedback contents
   * @throws IOException
   */
  private String constructContents(final String baseUrl) throws IOException {
    if (diff.getAppeared().isEmpty()) {
      return "";
    }

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
    final Map<String, Object> modelMap = getModelMap(componentPolicyViolationsMap, componentFeedbackList, baseUrl);

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
   * Gets the highest threat level from a list of policy violations.
   *
   * @param policyViolations The list of policy violations for which the highest threat level needs to be found
   * @return The highest threat level from the given policy violations, or 0 if there are none.
   */
  @VisibleForTesting
  static int getHighestThreatLevel(final List<PolicyViolation> policyViolations) {
    return policyViolations
        .stream()
        .map(PolicyViolation::getThreatLevel)
        .max(Comparator.comparingInt(Integer::intValue))
        .orElse(0);
  }

  /**
   * Gets the details for each of the violated policies
   *
   * @param policyViolations A list of all policy violations, the list can contain multiple violations for the same
   *                         policy
   * @param baseUrl          The baseUrl of the IQ server
   * @return A list of maps, each map in the list contains the details for violations on a specific policy
   */
  @VisibleForTesting
  static List<Map<String, Object>> getPoliciesViolatedMap(
      final List<PolicyViolation> policyViolations,
      final String baseUrl)
  {
    return policyViolations
        .stream()
        .collect(Collectors.groupingBy(
            AbstractPolicyViolation::getPolicyId
        ))
        .values()
        .stream()
        .sorted((o1, o2) -> Integer.compare(o2.get(0).getThreatLevel(), o1.get(0).getThreatLevel()))
        .map(groupedPolicyViolations -> ImmutableMap.<String, Object>builder()
            .put("threatLevel", groupedPolicyViolations.get(0).getThreatLevel())
            .put("name", groupedPolicyViolations.get(0).getPolicyName())
            .put("constraints", getConstraintsForPolicyViolationsPerPolicy(groupedPolicyViolations, baseUrl))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Gets the constraint details for each of the specified policy violations
   *
   * @param policyViolations A list of policy violations, these should all be for the same policy id
   * @param baseUrl          The baseUrl of the IQ server
   * @return A list of maps, each map in the list contains the details for a specific constraint
   */
  @VisibleForTesting
  static List<Map<String, Object>> getConstraintsForPolicyViolationsPerPolicy(
      final List<PolicyViolation> policyViolations,
      final String baseUrl)
  {
    return getConstraintDetailsForConstraints(policyViolations
        .stream()
        .sorted((o1, o2) -> Integer.compare(o2.getThreatLevel(), o1.getThreatLevel()))
        .map(PolicyViolation::getConstraintFacts)
        .flatMap(Collection::stream)
        .collect(Collectors.toList()), baseUrl);
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
      final String baseUrl)
  {
    return ImmutableMap.<String, Object>builder()
        .put("applicationName", app.getName())
        .put("organizationName", getOrganizationName(app))
        .put("componentList", componentFeedbackList)
        .put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(toEvaluation.getTime()))
        .put("stage", toEvaluation.getStageTypeId())
        .put("detailedReportUrl", baseUrl +
            UserInterfaceLinksResource.getReportUrl(app.getPublicId(), toEvaluation.getScanId()))
        .put("baseIqUrl", baseUrl)
        .put("policiesViolatedCount",
            componentPolicyViolationsMap.entrySet()
                .stream()
                .flatMap(entry ->
                    entry.getValue()
                    .stream()
                        .map(policyViolation -> String.format("%s|%s", entry.getKey(), policyViolation.getId())))
                .collect(Collectors.toSet()).size())
        .build();
  }
}
