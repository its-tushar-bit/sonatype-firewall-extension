/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Objects.isNull;

public class ReferenceIdParser
{
  private static final List<String> SECURITY_CONDITIONS = ImmutableList
      .of(SecurityVulnerabilitySeverityConditionType.ID, SecurityVulnerabilityStatusConditionType.ID);

  private static final Pattern REF_ID_REGEX_PATTERN = Pattern.compile("((CVE|SONATYPE|sonatype)-\\d+-\\d+)");

  public static Set<String> parseReferenceIds(final PolicyViolation policyViolation) {
    if (isNull(policyViolation.getConstraintFacts())) {
      return ImmutableSet.of();
    }
    return policyViolation
        .getConstraintFacts()
        .stream()
        .flatMap(ReferenceIdParser::streamSecurityConditionFacts)
        .map(ReferenceIdParser::extractTriggerReferenceValue)
        .filter(Objects::nonNull)
        .map(ReferenceIdParser::parseReferenceId)
        .flatMap(Collection::stream)
        .collect(toImmutableSet());
  }

  private static String extractTriggerReferenceValue(final ConditionFact conditionFact) {
    if (isNull(conditionFact)
        || isNull(conditionFact.getReference())
        || isNull(conditionFact.getReference().getValue()))
    {
      return null;
    }
    return conditionFact.getReference().getValue();
  }

  private static Stream<ConditionFact> streamSecurityConditionFacts(final ConstraintFact c) {
    return c.getConditionFacts()
        .stream()
        .filter(conditionFact -> SECURITY_CONDITIONS.contains(conditionFact.getConditionTypeId()));
  }

  private static Set<String> parseReferenceId(final String input) {
    final Matcher matcher = REF_ID_REGEX_PATTERN.matcher(input);
    final ImmutableSet.Builder<String> refIds = ImmutableSet.builder();
    while (matcher.find()) {
      refIds.add(matcher.group(1));
    }
    return refIds.build();
  }
}
