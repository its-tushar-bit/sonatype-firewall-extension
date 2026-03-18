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

public class DashboardViolationRiskDTOComparatorTest
{
  private DashboardViolationRiskDTO newDTO(
      int threatLevel,
      long time,
      String policyName,
      String applicationName,
      String derivedComponentName)
  {
    DashboardViolationRiskDTO dto = new DashboardViolationRiskDTO();
    dto.threatLevel = threatLevel;
    dto.firstOccurrenceTime = time;
    dto.policyName = policyName;
    dto.applicationName = applicationName;
    dto.derivedComponentName = derivedComponentName;
    return dto;
  }

  private void assertComparison(
      Comparator<DashboardViolationRiskDTO> comparator,
      int expected,
      DashboardViolationRiskDTO dto1,
      DashboardViolationRiskDTO dto2)
  {
    assertThat(comparator.compare(dto1, dto2)).isEqualTo(expected);
    assertThat(comparator.compare(dto2, dto1)).isEqualTo(-expected);
  }

  @Test
  public void testCompare_ThreatLevel_ASC_GreaterLast() {
    assertComparison(new DashboardViolationRiskDTOComparator("THREAT_LEVEL"), 1,
        newDTO(5, 0, "MyPolicy", "MyApp", null), newDTO(4, 0, "MyPolicy", "MyApp", null));
  }

  @Test
  public void testCompare_ThreatLevel_DESC_GreaterFirst() {
    assertComparison(new DashboardViolationRiskDTOComparator("-THREAT_LEVEL"), -1,
        newDTO(5, 0, "MyPolicy", "MyApp", null), newDTO(4, 0, "MyPolicy", "MyApp", null));
  }

  @Test
  public void testCompare_Time_ASC_GreaterLast() {
    assertComparison(new DashboardViolationRiskDTOComparator("AGE"), 1, newDTO(5, 1, "MyPolicy", "MyApp", null),
        newDTO(5, 0, "MyPolicy", "MyApp", null));
  }

  @Test
  public void testCompare_Time_DESC_GreaterFirst() {
    assertComparison(new DashboardViolationRiskDTOComparator("-AGE"), -1, newDTO(5, 1, "MyPolicy", "MyApp", null),
        newDTO(5, 0, "MyPolicy", "MyApp", null));
  }

  @Test
  public void testCompare_PolicyName_ASC_SmallerFirst() {
    assertComparison(new DashboardViolationRiskDTOComparator("POLICY_NAME"), -1,
        newDTO(5, 0, "MyPolicy", "MyApp", null), newDTO(5, 0, "MyPolicy1", "MyApp", null));
  }

  @Test
  public void testCompare_PolicyName_DESC_SmallerLast() {
    assertComparison(new DashboardViolationRiskDTOComparator("-POLICY_NAME"), 1,
        newDTO(5, 0, "MyPolicy", "MyApp", null), newDTO(5, 0, "MyPolicy1", "MyApp", null));
  }

  @Test
  public void testCompare_AppName_ASC_SmallerFirst() {
    assertComparison(new DashboardViolationRiskDTOComparator("APPLICATION_NAME"), -1,
        newDTO(5, 0, "MyPolicy", "MyApp", null), newDTO(5, 0, "MyPolicy", "MyApp1", null));
  }

  @Test
  public void testCompare_AppName_DESC_SmallerLast() {
    assertComparison(new DashboardViolationRiskDTOComparator("-APPLICATION_NAME"), 1,
        newDTO(5, 0, "MyPolicy", "MyApp", null), newDTO(5, 0, "MyPolicy", "MyApp1", null));
  }

  @Test
  public void testCompare_InvalidOrderBy() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> assertComparison(new DashboardViolationRiskDTOComparator("Invalid"), 0,
            newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"), newDTO(4, 0, "MyPolicy", "MyApp", "d.jar")))
        .withMessage("Invalid orderBy property.");
  }

  @Test
  public void testCompare_NullOrderBy_NoChange() {
    assertComparison(new DashboardViolationRiskDTOComparator(null), 0, newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"),
        newDTO(5, 0, "MyPolicy", "MyApp", "d.jar"));
  }

  @Test
  public void testCompare_EmptyOrderBy() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> assertComparison(new DashboardViolationRiskDTOComparator(""), 0,
            newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"), newDTO(6, 1, "MyPolicy1", "MyApp1", "d.jar")))
        .withMessage("Invalid orderBy property.");
  }

  @Test
  public void testCompare_LeadingCommaOrderBy() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> assertComparison(new DashboardViolationRiskDTOComparator(",THREAT_LEVEL"), 1,
            newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"),
            newDTO(4, 0, "MyPolicy", "MyApp", "d.jar")))
        .withMessage("Invalid orderBy property.");
  }

  @Test
  public void testCompare_MultipleOrderBys() {
    // policy names are the same so the compare is made with -THREAT_LEVEL
    assertComparison(new DashboardViolationRiskDTOComparator("POLICY_NAME,-THREAT_LEVEL"), -1,
        newDTO(5, 0, "MyPolicy", "MyApp", null), newDTO(4, 0, "MyPolicy", "MyApp", null));
  }
}
