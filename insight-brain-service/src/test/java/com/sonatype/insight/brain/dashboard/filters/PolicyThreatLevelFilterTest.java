/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PolicyThreatLevelFilterTest
{
  @Test
  public void testMinimumPolicyThreatLevel() {
    PolicyThreatLevelFilter filter = new PolicyThreatLevelFilter(4, null);
    PolicyViolation trueViolation = new PolicyViolation();
    trueViolation.setThreatLevel(4);

    PolicyViolation falseViolation = new PolicyViolation();
    falseViolation.setThreatLevel(0);

    assertThat(filter.asPolicyViolationPredicate().test(trueViolation)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(falseViolation)).isFalse();
  }

  @Test
  public void testMaximumPolicyThreatLevel() {
    PolicyThreatLevelFilter filter = new PolicyThreatLevelFilter(null, 4);
    PolicyViolation trueViolation = new PolicyViolation();
    trueViolation.setThreatLevel(4);

    PolicyViolation falseViolation = new PolicyViolation();
    falseViolation.setThreatLevel(5);

    assertThat(filter.asPolicyViolationPredicate().test(trueViolation)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(falseViolation)).isFalse();
  }

  @Test
  public void testMinimumAndMaximumPolicyThreatLevel() {
    PolicyThreatLevelFilter filter = new PolicyThreatLevelFilter(2, 4);
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    v1.setThreatLevel(4);
    v2.setThreatLevel(2);
    v3.setThreatLevel(0);

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v2)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v3)).isFalse();
  }

  @Test
  public void testMinimumAndMaximumPolicyEqualThreatLevels() {
    PolicyThreatLevelFilter filter = new PolicyThreatLevelFilter(2, 2);
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    v1.setThreatLevel(2);
    v2.setThreatLevel(2);
    v3.setThreatLevel(0);

    assertThat(filter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v2)).isTrue();
    assertThat(filter.asPolicyViolationPredicate().test(v3)).isFalse();
  }

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

    PolicyThreatLevelFilter noMinFilter = new PolicyThreatLevelFilter(noMin);
    PolicyThreatLevelFilter noMaxFilter = new PolicyThreatLevelFilter(noMax);
    PolicyThreatLevelFilter minAndMaxFilter = new PolicyThreatLevelFilter(minAndMax);
    PolicyThreatLevelFilter noMinAndNoMaxFilter = new PolicyThreatLevelFilter(noMinAndNoMax);
    PolicyThreatLevelFilter spacesInMinAndMaxFilter = new PolicyThreatLevelFilter(spacesInMinAndMax);

    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    v1.setThreatLevel(1);
    v2.setThreatLevel(2);
    v3.setThreatLevel(6);

    assertThat(noMinFilter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(noMinFilter.asPolicyViolationPredicate().test(v2)).isTrue();
    assertThat(noMinFilter.asPolicyViolationPredicate().test(v3)).isFalse();

    assertThat(noMaxFilter.asPolicyViolationPredicate().test(v1)).isFalse();
    assertThat(noMaxFilter.asPolicyViolationPredicate().test(v2)).isFalse();
    assertThat(noMaxFilter.asPolicyViolationPredicate().test(v3)).isTrue();

    assertThat(minAndMaxFilter.asPolicyViolationPredicate().test(v1)).isFalse();
    assertThat(minAndMaxFilter.asPolicyViolationPredicate().test(v2)).isFalse();
    assertThat(minAndMaxFilter.asPolicyViolationPredicate().test(v3)).isTrue();

    assertThat(noMinAndNoMaxFilter.asPolicyViolationPredicate().test(v1)).isTrue();
    assertThat(noMinAndNoMaxFilter.asPolicyViolationPredicate().test(v2)).isTrue();
    assertThat(noMinAndNoMaxFilter.asPolicyViolationPredicate().test(v3)).isTrue();

    assertThat(spacesInMinAndMaxFilter.asPolicyViolationPredicate().test(v1)).isFalse();
    assertThat(spacesInMinAndMaxFilter.asPolicyViolationPredicate().test(v2)).isFalse();
    assertThat(spacesInMinAndMaxFilter.asPolicyViolationPredicate().test(v3)).isTrue();
  }
}
