/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.SecurityVulnerability;

class AggregateScoring
{
  public static double computeAggregateScore(final ComponentDetailsDTO componentDetailsDTO) {
    List<SecurityVulnerability> vulnerabilities =
        componentDetailsDTO.securityVulnerabilities != null
            ? componentDetailsDTO.securityVulnerabilities
            : Collections.emptyList();

    final float maxScore = componentDetailsDTO.highestSecurityVulnerabilitySeverity != null
        ? componentDetailsDTO.highestSecurityVulnerabilitySeverity
        : 0.0F;

    return computeAggregateScore(vulnerabilities, maxScore);
  }

  public static double computeAggregateScore(final List<SecurityVulnerability> vulnerabilities) {
    if (vulnerabilities == null) {
      return computeAggregateScore(Collections.emptyList(), 0.0F);
    }

    final float maxScore = findMaxSeverity(vulnerabilities);

    return computeAggregateScore(vulnerabilities, maxScore);
  }

  private static double computeAggregateScore(
      final List<SecurityVulnerability> vulnerabilities,
      final float maxScore)
  {
    final double averageScore = getAverageSeverityScore(vulnerabilities);
    final int uniqueCWEs = getNumberOfUniqueCwes(vulnerabilities);

    final double score = maxScore + (uniqueCWEs - 1) * averageScore * 0.01;

    return Math.min(score, 10.0);
  }

  private static float findMaxSeverity(final Collection<SecurityVulnerability> vulnerabilities) {
    return (float) vulnerabilities.stream()
        .mapToDouble(SecurityVulnerability::getSeverity)
        .max()
        .orElse(0.0F);
  }

  private static int getNumberOfUniqueCwes(List<com.sonatype.clm.dto.model.SecurityVulnerability> vulnerabilities) {
    return vulnerabilities.stream()
        .map(SecurityVulnerability::getCwe)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet())
        .size();
  }

  private static double getAverageSeverityScore(
      List<com.sonatype.clm.dto.model.SecurityVulnerability> vulnerabilities)
  {
    return vulnerabilities.stream()
        .mapToDouble(SecurityVulnerability::getSeverity)
        .average()
        .orElse(0.0D);
  }
}
