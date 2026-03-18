/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentRiskDTOComparatorTest
{
  private ComponentRiskDTO newDTO(
      String derivedComponentName,
      int affectedApplications,
      int total,
      int critical,
      int severe,
      int moderate,
      int low)
  {
    ComponentRiskDTO dto = new ComponentRiskDTO();
    dto.derivedComponentName = derivedComponentName;
    dto.affectedApplications = affectedApplications;
    dto.score = total;
    dto.scoreCritical = critical;
    dto.scoreSevere = severe;
    dto.scoreModerate = moderate;
    dto.scoreLow = low;
    return dto;
  }

  private ComponentRiskDTO newDTO(
      String derivedComponentName,
      String hash,
      int affectedApplications,
      int total,
      int critical,
      int severe,
      int moderate,
      int low)
  {
    ComponentRiskDTO dto = newDTO(derivedComponentName, affectedApplications, total, critical, severe, moderate, low);
    dto.hash = hash;
    return dto;
  }

  private void assertComparison(
      Comparator<ComponentRiskDTO> comparator,
      int expected,
      ComponentRiskDTO dto1,
      ComponentRiskDTO dto2)
  {
    assertThat(comparator.compare(dto1, dto2)).isEqualTo(expected);
    assertThat(comparator.compare(dto2, dto1)).isEqualTo(-expected);
  }

  @Test
  public void testCompare_Name_ASC_SmallerFirst() {
    assertComparison(new ComponentRiskDTOComparator("NAME"), -1, newDTO("Name", 1, 5, 0, 0, 0, 0),
        newDTO("Name1", 1, 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_Name_DESC_SmallerLast() {
    assertComparison(new ComponentRiskDTOComparator("-NAME"), 1, newDTO("Name", 1, 5, 0, 0, 0, 0),
        newDTO("Name1", 1, 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_AffectedApplications_ASC_GreaterLast() {
    assertComparison(new ComponentRiskDTOComparator("NUMBER_OF_AFFECTED_APPS"), 1, newDTO("Name", 2, 5, 0, 0, 0, 0),
        newDTO("Name", 1, 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_AffectedApplications_DESC_GreaterFirst() {
    assertComparison(new ComponentRiskDTOComparator("-NUMBER_OF_AFFECTED_APPS"), -1, newDTO("Name", 2, 5, 0, 0, 0, 0),
        newDTO("Name", 1, 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_TotalScore_ASC_GreaterLast() {
    assertComparison(new ComponentRiskDTOComparator("TOTAL_RISK"), 1, newDTO("Name", 1, 5, 0, 0, 0, 0),
        newDTO("Name", 1, 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_TotalScore_DESC_GreaterFirst() {
    assertComparison(new ComponentRiskDTOComparator("-TOTAL_RISK"), -1, newDTO("Name", 1, 5, 0, 0, 0, 0),
        newDTO("Name", 1, 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_CriticalScore_ASC_GreaterLast() {
    assertComparison(new ComponentRiskDTOComparator("CRITICAL_RISK"), 1, newDTO("Name", 1, 0, 5, 0, 0, 0),
        newDTO("Name", 1, 0, 4, 0, 0, 0));
  }

  @Test
  public void testCompare_CriticalScore_DESC_GreaterFirst() {
    assertComparison(new ComponentRiskDTOComparator("-CRITICAL_RISK"), -1, newDTO("Name", 1, 0, 5, 0, 0, 0),
        newDTO("Name", 1, 0, 4, 0, 0, 0));
  }

  @Test
  public void testCompare_SevereScore_ASC_GreaterLast() {
    assertComparison(new ComponentRiskDTOComparator("SEVERE_RISK"), 1, newDTO("Name", 1, 0, 0, 5, 0, 0),
        newDTO("Name", 1, 0, 0, 4, 0, 0));
  }

  @Test
  public void testCompare_SevereScore_DESC_GreaterFirst() {
    assertComparison(new ComponentRiskDTOComparator("-SEVERE_RISK"), -1, newDTO("Name", 1, 0, 0, 5, 0, 0),
        newDTO("Name", 1, 0, 0, 4, 0, 0));
  }

  @Test
  public void testCompare_ModerateScore_ASC_GreaterLast() {
    assertComparison(new ComponentRiskDTOComparator("MODERATE_RISK"), 1, newDTO("Name", 1, 0, 0, 0, 5, 0),
        newDTO("Name", 1, 0, 0, 0, 4, 0));
  }

  @Test
  public void testCompare_ModerateScore_DESC_GreaterFirst() {
    assertComparison(new ComponentRiskDTOComparator("-MODERATE_RISK"), -1, newDTO("Name", 1, 0, 0, 0, 5, 0),
        newDTO("Name", 1, 0, 0, 0, 4, 0));
  }

  @Test
  public void testCompare_LowScore_ASC_GreaterLast() {
    assertComparison(new ComponentRiskDTOComparator("LOW_RISK"), 1, newDTO("Name", 1, 0, 0, 0, 0, 5),
        newDTO("Name", 1, 0, 0, 0, 0, 4));
  }

  @Test
  public void testCompare_LowScore_DESC_GreaterFirst() {
    assertComparison(new ComponentRiskDTOComparator("-LOW_RISK"), -1, newDTO("Name", 1, 0, 0, 0, 0, 5),
        newDTO("Name", 1, 0, 0, 0, 0, 4));
  }

  @Test
  public void testCompare_InvalidOrderBy() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> assertComparison(new ComponentRiskDTOComparator("Invalid"), -1, newDTO("Name", 1, 0, 0, 0, 0, 5),
            newDTO("Name", 1, 0, 0, 0, 0, 4)))
        .withMessage("Invalid orderBy property.");
  }

  @Test
  public void testCompare_NullOrderBy_NoChange() {
    assertComparison(new ComponentRiskDTOComparator(null), 0, newDTO("Name", 1, 0, 0, 0, 0, 5),
        newDTO("Name1", 0, 1, 1, 1, 1, 4));
  }

  @Test
  public void testCompare_EmptyOrderBy() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> assertComparison(new ComponentRiskDTOComparator(""), 0, newDTO("Name", 1, 0, 0, 0, 0, 5),
            newDTO("Name1", 0, 1, 1, 1, 1, 4)))
        .withMessage("Invalid orderBy property.");
  }

  @Test
  public void testCompare_SameOrdersByHash_BothHashesNull() {
    assertComparison(new ComponentRiskDTOComparator("NAME"), 0, newDTO("Name", null /* hash */, 0, 0, 0, 0, 0, 0),
        newDTO("Name", null /* hash */, 0, 0, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_SameOrdersByHash_SameHash() {
    assertComparison(new ComponentRiskDTOComparator("NAME"), 0, newDTO("Name", "hash", 0, 0, 0, 0, 0, 0),
        newDTO("Name", "hash", 0, 0, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_SameOrdersByHash_OneHashNull() {
    assertComparison(new ComponentRiskDTOComparator("NAME"), 1, newDTO("Name", null /* hash */, 0, 0, 0, 0, 0, 0),
        newDTO("Name", "hash", 0, 0, 0, 0, 0, 0));
    assertComparison(new ComponentRiskDTOComparator("NAME"), -1, newDTO("Name", "hash", 0, 0, 0, 0, 0, 0),
        newDTO("Name", null /* hash */, 0, 0, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_SameOrdersByHash_DifferentHashes() {
    assertComparison(new ComponentRiskDTOComparator("NAME"), -1, newDTO("Name", "hash1", 0, 0, 0, 0, 0, 0),
        newDTO("Name", "hash2", 0, 0, 0, 0, 0, 0));
    assertComparison(new ComponentRiskDTOComparator("NAME"), 1, newDTO("Name", "hash2", 0, 0, 0, 0, 0, 0),
        newDTO("Name", "hash1", 0, 0, 0, 0, 0, 0));
  }
}
