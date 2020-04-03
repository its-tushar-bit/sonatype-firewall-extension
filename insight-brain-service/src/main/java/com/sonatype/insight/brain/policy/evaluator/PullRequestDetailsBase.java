/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

public class PullRequestDetailsBase
{
  private static final Pattern CVE_REGEX_PATTERN = Pattern.compile("((CVE|SONATYPE|sonatype)-\\d+-\\d+)");

  private static final List<String> SECURITY_CONDITIONS = ImmutableList
      .of(SecurityVulnerabilitySeverityConditionType.ID, SecurityVulnerabilityStatusConditionType.ID);

  /**
   * Gets the constraint details for the given list of constraints
   *
   * @param constraintFactsInput A list of constraint facts for a specific policy violation. These should all be
   *                             relevant to the same policy
   * @param baseUrl             The baseUrl of the IQ server
   * @return A list of maps, each map in the list contains the details for a specific constraint
   */
  @VisibleForTesting
  static List<Map<String, Object>> getConstraintDetailsForConstraints(
      final List<ConstraintFact> constraintFactsInput,
      final String baseUrl)
  {
    return constraintFactsInput
        .stream()
        .collect(groupingBy(ConstraintFact::getConstraintId, LinkedHashMap::new, toList()))
        .values()
        .stream()
        .filter(constraintFacts -> !constraintFacts.isEmpty())
        .map(constraintFacts -> ImmutableMap.<String, Object>builder()
            .put("constraintName", constraintFacts.get(0).getConstraintName())
            .put("conditions", getConstraintConditionSummaries(constraintFacts, baseUrl))
            .build())
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Gets a list of condition summaries for the given list of constraints. All security conditions will be summarised
   * into a single string, all other conditions will each have it's own string
   *
   * @param constraintFacts The list of constraints that needs to be processed to get the condition summaries
   * @param baseUrl         The baseUrl of the IQ server
   * @return The list of condition summaries for the given constraints
   */
  @VisibleForTesting
  static List<String> getConstraintConditionSummaries(
      final List<ConstraintFact> constraintFacts,
      final String baseUrl)
  {
    final Map<Boolean, List<ConditionFact>> conditionFactsByType = constraintFacts
        .stream()
        .map(ConstraintFact::getConditionFacts)
        .flatMap(Collection::stream)
        .collect(partitioningBy(conditionFact -> SECURITY_CONDITIONS.contains(conditionFact.getConditionTypeId())));

    final List<String> conditionReasons = new ArrayList<>();
    getViolationSummaryForSecurityConditions(conditionFactsByType.get(Boolean.TRUE), baseUrl)
        .ifPresent(conditionReasons::add);
    conditionReasons.addAll(getViolationSummariesForNonSecurityConditions(conditionFactsByType.get(Boolean.FALSE)));

    return conditionReasons;
  }

  /**
   * Gets the summarised security condition string for the given conditions
   *
   * @param securityConditionFacts An list of security conditions that were violated that needs to be processed,
   *                               the optional will be empty of no security conditions are present
   * @param baseUrl                The baseUrl of the IQ Server
   * @return A single string, with comma delimited values for all security threats, prefixed with the required string
   */
  @VisibleForTesting
  static Optional<String> getViolationSummaryForSecurityConditions(
      final List<ConditionFact> securityConditionFacts,
      final String baseUrl)
  {
    final List<String> securityConditionDescriptions = securityConditionFacts
        .stream()
        .map(conditionFact -> applyCVEUrl(conditionFact, baseUrl))
        .distinct()
        .collect(Collectors.toList());

    if (securityConditionDescriptions.size() == 0) {
      return Optional.empty();
    }
    return Optional.of(String.format("%s %s", getSecurityPrefix(securityConditionDescriptions),
        String.join(", ", securityConditionDescriptions)));
  }

  @VisibleForTesting
  static String getSecurityPrefix(final List<String> securityConditionDescriptions) {
    if (securityConditionDescriptions.isEmpty()) {
      return "";
    }
    return String.format("Found security %s:",
        securityConditionDescriptions.size() == 1 ? "vulnerability" : "vulnerabilities");
  }

  /**
   * Gets a list of descriptions for each of the matches conditions specified
   *
   * @param nonSecurityConditionFacts The matched conditions that require summaries
   * @return Returns a list of descriptions, one for each specified non security condition
   */
  @VisibleForTesting
  static List<String> getViolationSummariesForNonSecurityConditions(
      final List<ConditionFact> nonSecurityConditionFacts)
  {
    return nonSecurityConditionFacts
        .stream()
        .map(ConditionFact::getReason)
        .distinct()
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  static String applyCVEUrl(final ConditionFact conditionFact, final String baseUrl) {
    if (conditionFact.getReference() == null || conditionFact.getReference().getValue() == null) {
      return null;
    }

    final Matcher matcher = CVE_REGEX_PATTERN.matcher(conditionFact.getReference().getValue());
    final StringBuffer stringBuffer = new StringBuffer();
    while (matcher.find()) {
      final String cveCode = matcher.group(1);
      matcher.appendReplacement(stringBuffer,
          MessageFormat.format("[{0}]({1}ui/links/vln/{0})", cveCode, baseUrl));
    }
    matcher.appendTail(stringBuffer);
    return stringBuffer.toString();
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
        .collect(groupingBy(
            AbstractPolicyViolation::getPolicyId
        ))
        .values()
        .stream()
        .sorted((o1, o2) -> Integer.compare(o2.get(0).getThreatLevel(), o1.get(0).getThreatLevel()))
        .map(groupedPolicyViolations -> ImmutableMap.<String, Object>builder()
            .put("threatLevel", groupedPolicyViolations.get(0).getThreatLevel())
            .put("name", groupedPolicyViolations.get(0).getPolicyName())
            .put("constraints", PullRequestDetailsBase
                .getConstraintsForPolicyViolationsPerPolicy(groupedPolicyViolations, baseUrl))
            .build())
        .collect(toList());
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
        .collect(toList()), baseUrl);
  }

  protected Object getOrganizationName(final Application app) {
    return new OrganizationDAO().getByIdNotNull(app.getOrganizationId()).getName();
  }
}
