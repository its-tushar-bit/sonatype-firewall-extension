/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ApplicationRiskScoreDTOComparatorTest
{
  private ApplicationRiskScoreDTO newDTO(String applicationName,
                                         int total,
                                         int critical,
                                         int severe,
                                         int moderate,
                                         int low)
  {
    ApplicationRiskScoreDTO dto = new ApplicationRiskScoreDTO("orgName", applicationName, "test");
    dto.totalApplicationRisk.totalRisk = total;
    dto.totalApplicationRisk.criticalRisk = critical;
    dto.totalApplicationRisk.severeRisk = severe;
    dto.totalApplicationRisk.moderateRisk = moderate;
    dto.totalApplicationRisk.lowRisk = low;
    return dto;
  }

  private void assertComparison(Comparator<ApplicationRiskScoreDTO> comparator,
                                int expected,
                                ApplicationRiskScoreDTO dto1,
                                ApplicationRiskScoreDTO dto2)
  {
    assertThat(comparator.compare(dto1, dto2), is(expected));
    assertThat(comparator.compare(dto2, dto1), is(-expected));
  }

  @Test
  public void testCompare_Name_ASC_SmallerFirst() {
    assertComparison(new ApplicationRiskScoreDTOComparator("NAME"), -1, newDTO("Name", 5, 0, 0, 0, 0),
        newDTO("Name1", 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_Name_DESC_SmallerLast() {
    assertComparison(new ApplicationRiskScoreDTOComparator("-NAME"), 1, newDTO("Name", 5, 0, 0, 0, 0),
        newDTO("Name1", 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_TotalScore_ASC_GreaterLast() {
    assertComparison(new ApplicationRiskScoreDTOComparator("TOTAL_RISK"), 1, newDTO("Name", 5, 0, 0, 0, 0),
        newDTO("Name", 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_TotalScore_DESC_GreaterFirst() {
    assertComparison(new ApplicationRiskScoreDTOComparator("-TOTAL_RISK"), -1, newDTO("Name", 5, 0, 0, 0, 0),
        newDTO("Name", 4, 0, 0, 0, 0));
  }

  @Test
  public void testCompare_CriticalScore_ASC_GreaterLast() {
    assertComparison(new ApplicationRiskScoreDTOComparator("CRITICAL_RISK"), 1, newDTO("Name", 0, 5, 0, 0, 0),
        newDTO("Name", 0, 4, 0, 0, 0));
  }

  @Test
  public void testCompare_CriticalScore_DESC_GreaterFirst() {
    assertComparison(new ApplicationRiskScoreDTOComparator("-CRITICAL_RISK"), -1, newDTO("Name", 0, 5, 0, 0, 0),
        newDTO("Name", 0, 4, 0, 0, 0));
  }

  @Test
  public void testCompare_SevereScore_ASC_GreaterLast() {
    assertComparison(new ApplicationRiskScoreDTOComparator("SEVERE_RISK"), 1, newDTO("Name", 0, 0, 5, 0, 0),
        newDTO("Name", 0, 0, 4, 0, 0));
  }

  @Test
  public void testCompare_SevereScore_DESC_GreaterFirst() {
    assertComparison(new ApplicationRiskScoreDTOComparator("-SEVERE_RISK"), -1, newDTO("Name", 0, 0, 5, 0, 0),
        newDTO("Name", 0, 0, 4, 0, 0));
  }

  @Test
  public void testCompare_ModerateScore_ASC_GreaterLast() {
    assertComparison(new ApplicationRiskScoreDTOComparator("MODERATE_RISK"), 1, newDTO("Name", 0, 0, 0, 5, 0),
        newDTO("Name", 0, 0, 0, 4, 0));
  }

  @Test
  public void testCompare_ModerateScore_DESC_GreaterFirst() {
    assertComparison(new ApplicationRiskScoreDTOComparator("-MODERATE_RISK"), -1, newDTO("Name", 0, 0, 0, 5, 0),
        newDTO("Name", 0, 0, 0, 4, 0));
  }

  @Test
  public void testCompare_LowScore_ASC_GreaterLast() {
    assertComparison(new ApplicationRiskScoreDTOComparator("LOW_RISK"), 1, newDTO("Name", 0, 0, 0, 0, 5),
        newDTO("Name", 0, 0, 0, 0, 4));
  }

  @Test
  public void testCompare_LowScore_DESC_GreaterFirst() {
    assertComparison(new ApplicationRiskScoreDTOComparator("-LOW_RISK"), -1, newDTO("Name", 0, 0, 0, 0, 5),
        newDTO("Name", 0, 0, 0, 0, 4));
  }

  @Test
  public void testCompare_InvalidOrderBy() {
    try {
      assertComparison(new ApplicationRiskScoreDTOComparator("Invalid"), -1, newDTO("Name", 0, 0, 0, 0, 5),
          newDTO("Name", 0, 0, 0, 0, 4));
      fail("Expected BadRequestException when invalid orderBy is used");
    }
    catch (BadRequestException e) {
      Assert.assertThat(e.getMessage(),
          is("Invalid orderBy property."));
    }
  }

  @Test
  public void testCompare_NullOrderBy_NoChange() {
    assertComparison(new ApplicationRiskScoreDTOComparator(null), 0, newDTO("Name", 0, 0, 0, 0, 5),
        newDTO("Name1", 1, 1, 1, 1, 4));
  }

  @Test
  public void testCompare_EmptyOrderBy() {
    try {
      assertComparison(new ApplicationRiskScoreDTOComparator(""), 0, newDTO("Name", 0, 0, 0, 0, 5),
          newDTO("Name1", 1, 1, 1, 1, 4));
      fail("Expected BadRequestException when invalid orderBy is used");
    }
    catch (BadRequestException e) {
      Assert.assertThat(e.getMessage(),
          is("Invalid orderBy property."));
    }
  }
}
