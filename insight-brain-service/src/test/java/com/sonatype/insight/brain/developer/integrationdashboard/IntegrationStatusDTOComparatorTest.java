/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.Comparator;

import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusDTO;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IntegrationStatusDTOComparatorTest
{
  @Test
  public void testCompare_Name_ASC_SmallerFirst() {
    assertComparison(new IntegrationStatusDTOComparator("NAME"), -1, newDTO("Name", 0L, 0L, 0),
        newDTO("Name1", 0L, 0L, 0));
  }

  @Test
  public void testCompare_Name_DESC_SmallerLast() {
    assertComparison(new IntegrationStatusDTOComparator("-NAME"), 1, newDTO("Name", 0L, 0L, 0),
        newDTO("Name1", 0L, 0L, 0));
  }

  @Test
  public void testCompare_Commit_ASC_GreaterLast() {
    assertComparison(new IntegrationStatusDTOComparator("COMMIT"), -1, newDTO("Name", 20_000L, 0L, 0),
        newDTO("Name1", 10_0000L, 0L, 0));
  }

  @Test
  public void testCompare_Commit_DESC_GreaterFirst() {
    assertComparison(new IntegrationStatusDTOComparator("-COMMIT"), 1, newDTO("Name", 20_000L, 0L, 0),
        newDTO("Name1", 10_0000L, 0L, 0));
  }

  @Test
  public void testCompare_Evaluation_ASC_GreaterLast() {
    assertComparison(new IntegrationStatusDTOComparator("EVALUATION"), -1, newDTO("Name", 0L, 10_000L, 0),
        newDTO("Name1", 0L, 5_0000L, 0));
  }

  @Test
  public void testCompare_Evaluation_DESC_GreaterFirst() {
    assertComparison(new IntegrationStatusDTOComparator("-EVALUATION"), 1, newDTO("Name", 0L, 10_000L, 0),
        newDTO("Name1", 0L, 5_0000L, 0));
  }

  @Test
  public void testCompare_TotalScore_ASC_GreaterLast() {
    assertComparison(new IntegrationStatusDTOComparator("TOTAL_RISK"), 1, newDTO("Name", 0L, 0L, 6),
        newDTO("Name1", 0L, 0L, 5));
  }

  @Test
  public void testCompare_TotalScore_DESC_GreaterFirst() {
    assertComparison(new IntegrationStatusDTOComparator("-TOTAL_RISK"), -1, newDTO("Name", 0L, 0L, 6),
        newDTO("Name1", 0L, 0L, 5));
  }

  @Test
  public void testCompare_UnsupportedOrderBy() {
    assertThatThrownBy(() -> new IntegrationStatusDTOComparator("-UNSUPPORTED"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid orderBy property -UNSUPPORTED");
  }

  private IntegrationStatusDTO newDTO(
      final String applicationName,
      final long lastCommitTimestamp,
      final long lastEvaluationTimestamp,
      final int totalRiskScore)
  {
    return new IntegrationStatusDTO()
        .setApplicationName(applicationName)
        .setLastCommitTimestamp(lastCommitTimestamp)
        .setLastEvaluationTimestamp(lastEvaluationTimestamp)
        .setTotalRiskScore(totalRiskScore);
  }

  private void assertComparison(
      final Comparator<IntegrationStatusDTO> comparator,
      final int expected,
      final IntegrationStatusDTO dto1,
      final IntegrationStatusDTO dto2)
  {
    assertThat(comparator.compare(dto1, dto2)).isEqualTo(expected);
    assertThat(comparator.compare(dto2, dto1)).isEqualTo(-expected);
  }
}
