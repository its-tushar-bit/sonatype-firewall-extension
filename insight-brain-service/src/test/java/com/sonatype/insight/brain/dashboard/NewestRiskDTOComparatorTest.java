/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NewestRiskDTOComparatorTest
{
  private Comparator<NewestRiskDTO> comparator = NewestRiskDTOComparator.INSTANCE;

  private NewestRiskDTO newDTO(int threatLevel, long time, String policyName, String applicationName, String hash) {
    NewestRiskDTO dto = new NewestRiskDTO();
    dto.threatLevel = threatLevel;
    dto.time = time;
    dto.policyName = policyName;
    dto.applicationName = applicationName;
    dto.hash = hash;
    return dto;
  }

  private void assertComparison(int expected, NewestRiskDTO dto1, NewestRiskDTO dto2) {
    assertThat(comparator.compare(dto1, dto2), is(expected));
    assertThat(comparator.compare(dto2, dto1), is(-expected));
  }

  @Test
  public void testCompare_GreaterThreatLevelComesFirst() {
    assertComparison(-1, newDTO(5, 0, "MyPolicy", "MyApp", "ababababab"),
        newDTO(4, 0, "MyPolicy", "MyApp", "ababababab"));
  }

  @Test
  public void testCompare_GreaterTimeComesFirst() {
    assertComparison(-1, newDTO(5, 1, "MyPolicy", "MyApp", "ababababab"),
        newDTO(5, 0, "MyPolicy", "MyApp", "ababababab"));
  }

  @Test
  public void testCompare_SmallerPolicyNameComesFirst() {
    assertComparison(-1, newDTO(5, 0, "MyPolicy", "MyApp", "ababababab"),
        newDTO(5, 0, "MyPolicy1", "MyApp", "ababababab"));
  }

  @Test
  public void testCompare_SmallerAppNameComesFirst() {
    assertComparison(-1, newDTO(5, 0, "MyPolicy", "MyApp", "ababababab"),
        newDTO(5, 0, "MyPolicy", "MyApp1", "ababababab"));
  }

  @Test
  public void testCompare_SmallerComponentNameComesFirst() {
    assertComparison(-1, newDTO(5, 0, "MyPolicy", "MyApp", "ababababab"),
        newDTO(5, 0, "MyPolicy", "MyApp", "ababababab1"));
  }
}
