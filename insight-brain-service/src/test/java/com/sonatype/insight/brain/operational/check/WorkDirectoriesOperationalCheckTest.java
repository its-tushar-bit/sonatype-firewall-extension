/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkDirectoriesOperationalCheckTest
    extends AbstractComponentTest
{
  @Inject
  private WorkDirectoriesOperationalCheck workDirectoriesOperationalCheck;

  @Inject
  private InsightConfig insightConfig;

  @Test
  public void testCheck() {
    insightConfig.setClusterDirectory(insightConfig.getSonatypeWork().getAbsolutePath());

    Result result = workDirectoriesOperationalCheck.check();

    assertThat(result.isHealthy()).isTrue();
  }

  @Test
  public void testCheck_SonatypeWork_NotADirectory() {
    insightConfig.setSonatypeWork("doesNotExist");

    Result result = workDirectoriesOperationalCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).isEqualTo(insightConfig.getSonatypeWork() + " is not a directory");
  }

  @Test
  public void testCheck_ClusterDirectory_NotADirectory() {
    insightConfig.setClusterDirectory("doesNotExist");

    Result result = workDirectoriesOperationalCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).isEqualTo(insightConfig.getClusterDirectory() + " is not a directory");
  }
}
