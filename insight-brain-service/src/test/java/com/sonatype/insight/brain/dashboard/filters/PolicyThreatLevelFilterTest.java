/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PolicyThreatLevelFilterTest
{
  @Test
  public void testMinimumPolicyThreatLevel() {
    PolicyThreatLevelFilter filter = new PolicyThreatLevelFilter(Integer.valueOf(4), null);
    PolicyViolation trueViolation = new PolicyViolation();
    trueViolation.setThreatLevel(4);

    PolicyViolation falseViolation = new PolicyViolation();
    falseViolation.setThreatLevel(0);

    assertThat(filter.apply(trueViolation), is(true));
    assertThat(filter.apply(falseViolation), is(false));
  }

  @Test
  public void testMaximumPolicyThreatLevel() {
    PolicyThreatLevelFilter filter = new PolicyThreatLevelFilter(null, Integer.valueOf(4));
    PolicyViolation trueViolation = new PolicyViolation();
    trueViolation.setThreatLevel(4);

    PolicyViolation falseViolation = new PolicyViolation();
    falseViolation.setThreatLevel(5);

    assertThat(filter.apply(trueViolation), is(true));
    assertThat(filter.apply(falseViolation), is(false));
  }

  @Test
  public void testMinimumAndMaximumPolicyThreatLevel() {
    PolicyThreatLevelFilter filter = new PolicyThreatLevelFilter(Integer.valueOf(2), Integer.valueOf(4));
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    v1.setThreatLevel(4);
    v2.setThreatLevel(2);
    v3.setThreatLevel(0);

    assertThat(filter.apply(v1), is(true));
    assertThat(filter.apply(v2), is(true));
    assertThat(filter.apply(v3), is(false));
  }

  @Test
  public void testMinimumAndMaximumPolicyEqualThreatLevels() {
    PolicyThreatLevelFilter filter = new PolicyThreatLevelFilter(Integer.valueOf(2), Integer.valueOf(2));
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    v1.setThreatLevel(2);
    v2.setThreatLevel(2);
    v3.setThreatLevel(0);

    assertThat(filter.apply(v1), is(true));
    assertThat(filter.apply(v2), is(true));
    assertThat(filter.apply(v3), is(false));
  }

  @Test
  public void testMinimumThreatLevelExceedsMaximumThreatLevel() {
    try {
      new PolicyThreatLevelFilter(Integer.valueOf(4), Integer.valueOf(2));
      fail("Filter should throw a bad request exception when minimum threat level exceeds maximum threat level.");
    }
    catch (BadRequestException e) {
      assertEquals("Minimum policy threat level should not exceed maximum policy threat level.", e.getMessage());
    }
  }

  @Test
  public void testNullViolation() {
    PolicyThreatLevelFilter filter = new PolicyThreatLevelFilter(null, Integer.valueOf(4));
    assertThat(filter.apply(null), is(false));
  }

  @Test
  public void testStringConstructionWithMalformedRange() {
    String noComma = "3 5";
    String singlePoint = "3";
    String notAnInteger = "a,a";
    String tooManyPoints = "1,2,3";
    String nullString = null;
    String emptyString = "";

    try {
      new PolicyThreatLevelFilter(noComma);
      fail("Filter should throw a bad request exception when unable to parse range.");
    }
    catch (BadRequestException e) {
      assertEquals("Unable to parse policy threat range from " + noComma
          + ". Expected format is 'min,max' or ',max' or 'min,'.", e.getMessage());
    }

    try {
      new PolicyThreatLevelFilter(singlePoint);
      fail("Filter should throw a bad request exception when unable to parse range.");
    }
    catch (BadRequestException e) {
      assertEquals("Unable to parse policy threat range from " + singlePoint
          + ". Expected format is 'min,max' or ',max' or 'min,'.", e.getMessage());
    }

    try {
      new PolicyThreatLevelFilter(notAnInteger);
      fail("Filter should throw a bad request exception when unable to parse range.");
    }
    catch (BadRequestException e) {
      assertEquals("Unable to parse policy threat range from " + notAnInteger + ".", e.getMessage());
    }

    try {
      new PolicyThreatLevelFilter(tooManyPoints);
      fail("Filter should throw a bad request exception when unable to parse range.");
    }
    catch (BadRequestException e) {
      assertEquals("Unable to parse policy threat range from " + tooManyPoints
          + ". Expected format is 'min,max' or ',max' or 'min,'.", e.getMessage());
    }

    try {
      new PolicyThreatLevelFilter(nullString);
      fail("Filter should throw a bad request exception when unable to parse range.");
    }
    catch (BadRequestException e) {
      assertEquals("Unable to parse policy threat range from empty or null range.", e.getMessage());
    }

    try {
      new PolicyThreatLevelFilter(emptyString);
      fail("Filter should throw a bad request exception when unable to parse range.");
    }
    catch (BadRequestException e) {
      assertEquals("Unable to parse policy threat range from empty or null range.", e.getMessage());
    }
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
    
    assertTrue(noMinFilter.apply(v1));
    assertTrue(noMinFilter.apply(v2));
    assertFalse(noMinFilter.apply(v3));

    assertFalse(noMaxFilter.apply(v1));
    assertFalse(noMaxFilter.apply(v2));
    assertTrue(noMaxFilter.apply(v3));

    assertFalse(minAndMaxFilter.apply(v1));
    assertFalse(minAndMaxFilter.apply(v2));
    assertTrue(minAndMaxFilter.apply(v3));

    assertTrue(noMinAndNoMaxFilter.apply(v1));
    assertTrue(noMinAndNoMaxFilter.apply(v2));
    assertTrue(noMinAndNoMaxFilter.apply(v3));

    assertFalse(spacesInMinAndMaxFilter.apply(v1));
    assertFalse(spacesInMinAndMaxFilter.apply(v2));
    assertTrue(spacesInMinAndMaxFilter.apply(v3));
  }
}
