/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ParametersTest
{
  @Test
  public void testGetPolicyEvaluatorClass_DefaultMode() throws Exception {
    assertThat(new Parameters().isExpandedCoverageMode(), is(false));
  }

  @Test
  public void testGetPolicyEvaluatorClass_ExpandedCoverageMode() throws Exception {
    assertThat(new Parameters("-xc").isExpandedCoverageMode(), is(true));

    assertThat(new Parameters("--expanded-coverage").isExpandedCoverageMode(), is(true));
  }
}
