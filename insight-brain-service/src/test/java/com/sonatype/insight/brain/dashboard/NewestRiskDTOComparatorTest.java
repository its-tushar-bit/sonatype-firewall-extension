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

public class NewestRiskDTOComparatorTest
{
  private NewestRiskDTO newDTO(int threatLevel,
                               long time,
                               String policyName,
                               String applicationName,
                               String derivedComponentName)
  {
    NewestRiskDTO dto = new NewestRiskDTO();
    dto.threatLevel = threatLevel;
    dto.firstOccurrenceTime = time;
    dto.policyName = policyName;
    dto.applicationName = applicationName;
    dto.derivedComponentName = derivedComponentName;
    return dto;
  }

  private void assertComparison(Comparator<NewestRiskDTO> comparator,
                                int expected,
                                NewestRiskDTO dto1,
                                NewestRiskDTO dto2)
  {
    assertThat(comparator.compare(dto1, dto2), is(expected));
    assertThat(comparator.compare(dto2, dto1), is(-expected));
  }

  @Test
  public void testCompare_ThreatLevel_ASC_GreaterLast() {
    assertComparison(new NewestRiskDTOComparator("THREAT_LEVEL"), 1, newDTO(5, 0, "MyPolicy", "MyApp", null),
        newDTO(4, 0, "MyPolicy", "MyApp", null));
  }

  @Test
  public void testCompare_ThreatLevel_DESC_GreaterFirst() {
    assertComparison(new NewestRiskDTOComparator("-THREAT_LEVEL"), -1, newDTO(5, 0, "MyPolicy", "MyApp", null),
        newDTO(4, 0, "MyPolicy", "MyApp", null));
  }

  @Test
  public void testCompare_Time_ASC_GreaterLast() {
    assertComparison(new NewestRiskDTOComparator("AGE"), 1, newDTO(5, 1, "MyPolicy", "MyApp", null),
        newDTO(5, 0, "MyPolicy", "MyApp", null));
  }

  @Test
  public void testCompare_Time_DESC_GreaterFirst() {
    assertComparison(new NewestRiskDTOComparator("-AGE"), -1, newDTO(5, 1, "MyPolicy", "MyApp", null),
        newDTO(5, 0, "MyPolicy", "MyApp", null));
  }

  @Test
  public void testCompare_PolicyName_ASC_SmallerFirst() {
    assertComparison(new NewestRiskDTOComparator("POLICY_NAME"), -1, newDTO(5, 0, "MyPolicy", "MyApp", null),
        newDTO(5, 0, "MyPolicy1", "MyApp", null));
  }

  @Test
  public void testCompare_PolicyName_DESC_SmallerLast() {
    assertComparison(new NewestRiskDTOComparator("-POLICY_NAME"), 1, newDTO(5, 0, "MyPolicy", "MyApp", null),
        newDTO(5, 0, "MyPolicy1", "MyApp", null));
  }

  @Test
  public void testCompare_AppName_ASC_SmallerFirst() {
    assertComparison(new NewestRiskDTOComparator("APPLICATION_NAME"), -1, newDTO(5, 0, "MyPolicy", "MyApp", null),
        newDTO(5, 0, "MyPolicy", "MyApp1", null));
  }

  @Test
  public void testCompare_AppName_DESC_SmallerLast() {
    assertComparison(new NewestRiskDTOComparator("-APPLICATION_NAME"), 1, newDTO(5, 0, "MyPolicy", "MyApp", null),
        newDTO(5, 0, "MyPolicy", "MyApp1", null));
  }

  @Test
  public void testCompare_ComponentName_ASC_SmallerFirst() {
    assertComparison(new NewestRiskDTOComparator("COMPONENT_NAME"), -1, newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"),
        newDTO(5, 0, "MyPolicy", "MyApp", "d.jar"));
  }

  @Test
  public void testCompare_ComponentName_DESC_SmallerLast() {
    assertComparison(new NewestRiskDTOComparator("-COMPONENT_NAME"), 1, newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"),
        newDTO(5, 0, "MyPolicy", "MyApp", "d.jar"));
  }

  @Test
  public void testCompare_InvalidOrderBy() {
    try {
      assertComparison(new NewestRiskDTOComparator("Invalid"), 0, newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"),
          newDTO(4, 0, "MyPolicy", "MyApp", "d.jar"));
      fail("Expected BadRequestException when invalid orderBy is used");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid orderBy property."));
    }
  }

  @Test
  public void testCompare_NullOrderBy_NoChange() {
    assertComparison(new NewestRiskDTOComparator(null), 0, newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"),
        newDTO(5, 0, "MyPolicy", "MyApp", "d.jar"));
  }

  @Test
  public void testCompare_EmptyOrderBy() {
    try {
      assertComparison(new NewestRiskDTOComparator(""), 0, newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"),
          newDTO(6, 1, "MyPolicy1", "MyApp1", "d.jar"));
      fail("Expected BadRequestException when invalid orderBy is used");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid orderBy property."));
    }
  }

  @Test
  public void testCompare_LeadingCommaOrderBy() {
    try {
      assertComparison(new NewestRiskDTOComparator(",THREAT_LEVEL"), 1, newDTO(5, 0, "MyPolicy", "MyApp", "c.jar"),
          newDTO(4, 0, "MyPolicy", "MyApp", "d.jar"));
      fail("Expected BadRequestException when invalid orderBy is used");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid orderBy property."));
    }
  }

  @Test
  public void testCompare_MultipleOrderBys() {
    // policy names are the same so the compare is made with -THREAT_LEVEL
    assertComparison(new NewestRiskDTOComparator("POLICY_NAME,-THREAT_LEVEL"), -1,
        newDTO(5, 0, "MyPolicy", "MyApp", null), newDTO(4, 0, "MyPolicy", "MyApp", null));  
  }
}
