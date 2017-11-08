/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class NewestRiskDTOTest
{
  private NewestRiskDTO risk;

  @Before
  public void before() {
    risk = new NewestRiskDTO();
    risk.threatLevel = 7;
    risk.policyName = "p";
    risk.applicationName = "a";
    risk.firstOccurrenceTime = 0;
  }

  @Test
  public void testToCsvLine_notQuotedIfNotNecessary() {
    risk.pathnames = Arrays.asList("a/b/c.jar");
    assertThat(risk.toCsvLine(), is("7,p,a,c.jar,1970-01-01T00:00:00Z,0"));
  }

  @Test
  public void testToCsvLine_quotedIfNecessary() {
    risk.pathnames = Arrays.asList("a/b/c.jar", "d/e/");
    assertThat(risk.toCsvLine(), is("7,p,a,\"c.jar, e\",1970-01-01T00:00:00Z,0"));
  }

  @Test
  public void testToCsvLine_printsHashForUnknownWithNoPath() {
    risk.pathnames = Collections.emptyList();
    risk.hash = "theHash";
    assertThat(risk.toCsvLine(), is("7,p,a,(Anonymized Path) SHA1: theHash,1970-01-01T00:00:00Z,0"));
  }
}
