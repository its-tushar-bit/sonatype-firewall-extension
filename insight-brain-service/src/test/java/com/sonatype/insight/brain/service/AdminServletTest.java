/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminServletTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test(timeout = 60_000)
  public void testCpuProfiling_NoEndlessBusyLoopOnNegativeFrequency_CLM_16983() throws Exception {
    assertThat(adminRequest().path("pprof").query("frequency", -1).query("duration", "1").get()).isNotNull();
  }
}
