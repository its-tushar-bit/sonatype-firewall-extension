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

public class ComponentRiskDTOComparatorTest
{
  private Comparator<ComponentRiskDTO> comparator = ComponentRiskDTOComparator.INSTANCE;

  private ComponentRiskDTO newDTO(int total, int critical, int severe, int moderate, int low, String hash) {
    ComponentRiskDTO dto = new ComponentRiskDTO();
    dto.score = total;
    dto.scoreCritical = critical;
    dto.scoreSevere = severe;
    dto.scoreModerate = moderate;
    dto.scoreLow = low;
    dto.hash = hash;
    return dto;
  }

  private void assertComparison(int expected, ComponentRiskDTO dto1, ComponentRiskDTO dto2) {
    assertThat(comparator.compare(dto1, dto2), is(expected));
    assertThat(comparator.compare(dto2, dto1), is(-expected));
  }

  @Test
  public void testCompare_GreaterTotalScoreComesFirst() {
    assertComparison(-1, newDTO(5, 0, 0, 0, 0, "hash"), newDTO(4, 0, 0, 0, 0, "hash"));
  }

  @Test
  public void testCompare_GreaterCriticalScoreComesFirst() {
    assertComparison(-1, newDTO(0, 5, 0, 0, 0, "hash"), newDTO(0, 4, 0, 0, 0, "hash"));
  }

  @Test
  public void testCompare_GreaterSevereScoreComesFirst() {
    assertComparison(-1, newDTO(0, 0, 5, 0, 0, "hash"), newDTO(0, 0, 4, 0, 0, "hash"));
  }

  @Test
  public void testCompare_GreaterModerateScoreComesFirst() {
    assertComparison(-1, newDTO(0, 0, 0, 5, 0, "hash"), newDTO(0, 0, 0, 4, 0, "hash"));
  }

  @Test
  public void testCompare_GreaterLowScoreComesFirst() {
    assertComparison(-1, newDTO(0, 0, 0, 0, 5, "hash"), newDTO(0, 0, 0, 0, 4, "hash"));
  }

  @Test
  public void testCompare_SmallerHashComesFirst() {
    assertComparison(-1, newDTO(0, 0, 0, 0, 0, "a"), newDTO(0, 0, 0, 0, 0, "b"));
  }

  @Test
  public void testCompare_NullHashComesLast() {
    assertComparison(-1, newDTO(0, 0, 0, 0, 0, "a"), newDTO(0, 0, 0, 0, 0, null));
  }
}
