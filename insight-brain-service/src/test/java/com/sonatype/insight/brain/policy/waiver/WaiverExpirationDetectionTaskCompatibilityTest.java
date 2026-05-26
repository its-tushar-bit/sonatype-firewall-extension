/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class WaiverExpirationDetectionTaskCompatibilityTest
{
  @Test
  public void shouldKeepLegacyQuartzJobClassAvailable() throws Exception {
    Class<?> jobClass = Class.forName("com.sonatype.insight.brain.policy.waiver.WaiverExpirationDetectionTask");

    assertThat(jobClass).isEqualTo(WaiverExpirationDetectionTask.class);
    assertThat(new WaiverExpirationDetectionTask().getJobName()).isEqualTo("WaiverExpirationDetectionTask");
  }
}
