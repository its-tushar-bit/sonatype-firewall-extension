/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.hds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.sonatype.insight.brain.hds.AggregateScoring.computeAggregateScore;
import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class AggregateScoringTest
    extends AbstractComponentH2Test
{
  public static Stream<Arguments> testParameters() {
    final List<Arguments> testIterations = new ArrayList<>();

    testIterations.add(Arguments.of(
        "should not include null cwes in cwe count",
        6.3F,
        Arrays.asList(
            generateSecurityVulnerability(6.3F, "cwe-180"),
            generateSecurityVulnerability(0F, null),
            generateSecurityVulnerability(0F, null),
            generateSecurityVulnerability(0F, null)),
        6.300000190734863D));

    testIterations.add(Arguments.of(
        "should handle multiple unique cwes and vulns",
        9.3F,
        Arrays.asList(
            generateSecurityVulnerability(9.3F, "cwe-180"),
            generateSecurityVulnerability(4.0F, "cwe-181"),
            generateSecurityVulnerability(9.0F, "cwe-182"),
            generateSecurityVulnerability(1.1F, "cwe-183"),
            // cwe count should be based in unique cwe
            generateSecurityVulnerability(9.2F, "cwe-184"),
            generateSecurityVulnerability(7.2F, "cwe-184"),
            generateSecurityVulnerability(7.2F, "cwe-184")),
        9.568571617262704D));

    testIterations.add(Arguments.of(
        "should max out at 10",
        10.0F,
        Arrays.asList(
            generateSecurityVulnerability(10.0F, "cwe-180"),
            generateSecurityVulnerability(10.0F, "cwe-181"),
            generateSecurityVulnerability(10.0F, "cwe-182"),
            generateSecurityVulnerability(10.0F, "cwe-183")),
        10.0D));

    return testIterations.stream();
  }

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("testParameters")
  public void testComputeAggregateScore_shouldComputeCorrectValueFromComponentDetailsDto(
      final String testDescription,
      final float givenHighestSeverity,
      final List<SecurityVulnerability> givenVulnerabilities,
      final double expectedScore)
  {
    final ComponentDetailsDTO givenComponentDetails = generateComponentDetailsDto(givenHighestSeverity,
        givenVulnerabilities);

    final double result = computeAggregateScore(givenComponentDetails);

    assertThat(result).isEqualTo(expectedScore);
  }

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("testParameters")
  public void testComputeAggregateScore_shouldComputeCorrectValueFromSecurityVulnerabilities(
      final String testDescription,
      final float givenHighestSeverity,
      final List<SecurityVulnerability> givenVulnerabilities,
      final double expectedScore)
  {
    final double result = computeAggregateScore(givenVulnerabilities);

    assertThat(result).isEqualTo(expectedScore);
  }

  private static ComponentDetailsDTO generateComponentDetailsDto(
      final float highestSecurityVulnerabilitySeverity,
      final List<SecurityVulnerability> securityVulnerabilities)
  {
    final ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.highestSecurityVulnerabilitySeverity = highestSecurityVulnerabilitySeverity;
    componentDetailsDTO.securityVulnerabilities = securityVulnerabilities;

    return componentDetailsDTO;
  }

  private static SecurityVulnerability generateSecurityVulnerability(final float severity, final String cwe) {
    final SecurityVulnerability securityVulnerability = new SecurityVulnerability(
        getAnyValue(), getAnyValue(), severity);

    securityVulnerability.setCwe(cwe);

    return securityVulnerability;
  }

  private static String getAnyValue() {
    return UUID.randomUUID().toString();
  }
}
