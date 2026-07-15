/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PolicyThreatLevelFilterTest
{
  @Test
  public void testMinimumThreatLevelExceedsMaximumThreatLevel() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> new PolicyThreatLevelFilter(4, 2))
        .withMessage("Minimum policy threat level should not exceed maximum policy threat level.");
  }

  @Test
  public void testStringConstructionWithMalformedRange() {
    String noComma = "3 5";
    String singlePoint = "3";
    String notAnInteger = "a,a";
    String tooManyPoints = "1,2,3";
    String nullString = null;
    String emptyString = "";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> new PolicyThreatLevelFilter(noComma))
        .withMessage("Unable to parse policy threat range from " + noComma +
            ". Expected format is 'min,max' or ',max' or 'min,'.");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> new PolicyThreatLevelFilter(singlePoint))
        .withMessage("Unable to parse policy threat range from " + singlePoint
            + ". Expected format is 'min,max' or ',max' or 'min,'.");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> new PolicyThreatLevelFilter(notAnInteger))
        .withMessage("Unable to parse policy threat range from " + notAnInteger + ".");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> new PolicyThreatLevelFilter(tooManyPoints))
        .withMessage("Unable to parse policy threat range from " + tooManyPoints
            + ". Expected format is 'min,max' or ',max' or 'min,'.");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> new PolicyThreatLevelFilter(nullString))
        .withMessage("Unable to parse policy threat range from empty or null range.");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> new PolicyThreatLevelFilter(emptyString))
        .withMessage("Unable to parse policy threat range from empty or null range.");
  }

  @Test
  public void testStringConstruction() {
    String noMin = ",5";
    String noMax = "3,";
    String minAndMax = "3,6";
    String noMinAndNoMax = ",";
    String spacesInMinAndMax = "    3     ,     6   ";

    new PolicyThreatLevelFilter(noMin);
    new PolicyThreatLevelFilter(noMax);
    new PolicyThreatLevelFilter(minAndMax);
    new PolicyThreatLevelFilter(noMinAndNoMax);
    new PolicyThreatLevelFilter(spacesInMinAndMax);
  }

  @Test
  public void jsonValue_deserializesLegacyStringRangeFormat() throws Exception {
    PolicyThreatLevelFilter filter = new ObjectMapper().readValue("\"1,8\"", PolicyThreatLevelFilter.class);

    assertThat(filter.getMinPolicyThreatLevel()).isEqualTo(1);
    assertThat(filter.getMaxPolicyThreatLevel()).isEqualTo(8);
  }

  @Test
  public void jsonCreator_deserializesMinMaxObject() throws Exception {
    PolicyThreatLevelFilter filter = new ObjectMapper().readValue(
        "{\"minPolicyThreatLevel\":1,\"maxPolicyThreatLevel\":8}",
        PolicyThreatLevelFilter.class);

    assertThat(filter.getMinPolicyThreatLevel()).isEqualTo(1);
    assertThat(filter.getMaxPolicyThreatLevel()).isEqualTo(8);
  }
}
