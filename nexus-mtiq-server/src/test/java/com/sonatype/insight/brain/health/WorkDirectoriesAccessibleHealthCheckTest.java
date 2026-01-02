/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;
import java.nio.file.Files;

import com.sonatype.insight.brain.service.InsightConfig;

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class WorkDirectoriesAccessibleHealthCheckTest
{
  @Test
  public void testHealthCheck_InvalidSonatypeWork() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork("not-a-real-sonatype-work");
    insightConfig.setClusterDirectory(Files.createTempDirectory("a-real-cluster-directory").toString());

    WorkDirectoriesAccessibleHealthCheck healthCheck = new WorkDirectoriesAccessibleHealthCheck(insightConfig);
    assertThat(healthCheck.getName()).isEqualTo("work-directories");
    Result result = healthCheck.check();
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).startsWith(
        "Work directory 'sonatypeWork' failed accessibility check. Directory being checked:");
    // In this particular test the test folder doesn't exist so we can assert a NoSuchFileException
    assertThat(result.getMessage()).contains("Type: ExecutionException, Message: java.nio.file.NoSuchFileException:");
  }

  @Test
  public void testHealthCheck_InvalidClusterDirectory() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(Files.createTempDirectory("a-real-sonatype-work-directory").toString());
    insightConfig.setClusterDirectory("not-a-real-cluster-directory");

    WorkDirectoriesAccessibleHealthCheck healthCheck = new WorkDirectoriesAccessibleHealthCheck(insightConfig);
    assertThat(healthCheck.getName()).isEqualTo("work-directories");
    Result result = healthCheck.check();
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).startsWith(
        "Work directory 'clusterDirectory' failed accessibility check. Directory being checked:");
    // In this particular test the test folder doesn't exist so we can assert a NoSuchFileException
    assertThat(result.getMessage()).contains("Type: ExecutionException, Message: java.nio.file.NoSuchFileException:");
  }

  @Test
  public void testHealthCheck_FileSystemReadTimeout() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(Files.createTempDirectory("a-real-cluster-directory").toString());
    insightConfig.setClusterDirectory(Files.createTempDirectory("a-real-cluster-directory").toString());
    WorkDirectoriesAccessibleHealthCheck healthCheck = new WorkDirectoriesAccessibleHealthCheck(insightConfig)
    {
      // Override the readTestFile method to simulate a timeout
      @Override
      Void readTestFile(final File dir) {
        try {
          // Times 2 for two checks + 100ms for good measure
          Thread.sleep((WorkDirectoriesAccessibleHealthCheck.TIMEOUT_IN_SECONDS * 1000 * 2) + 100);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
        throw new RuntimeException("Should not reach here");
      }
    };

    assertThat(healthCheck.getName()).isEqualTo("work-directories");
    Result result = healthCheck.check();
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).startsWith(
        "Work directory 'clusterDirectory' failed accessibility check. Directory being checked:");
    assertThat(result.getMessage()).contains("Type: TimeoutException");
  }

  @Test
  public void testHealthCheck_Valid() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(Files.createTempDirectory("a-real-cluster-directory").toString());
    insightConfig.setClusterDirectory(Files.createTempDirectory("a-real-cluster-directory").toString());
    WorkDirectoriesAccessibleHealthCheck healthCheck = new WorkDirectoriesAccessibleHealthCheck(insightConfig);

    assertThat(healthCheck.getName()).isEqualTo("work-directories");
    Result result = healthCheck.check();
    assertThat(result.isHealthy()).isTrue();
  }
}
