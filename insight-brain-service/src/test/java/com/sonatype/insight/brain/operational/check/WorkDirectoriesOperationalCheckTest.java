/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.testsupport.TempFolder;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

public class WorkDirectoriesOperationalCheckTest
{
  @RegisterExtension
  public TempFolder temporaryFolder = new TempFolder();

  private InsightConfig insightConfig;

  private WorkDirectoriesOperationalCheck workDirectoriesOperationalCheck;

  @BeforeEach
  public void setUp() throws Exception {
    insightConfig = new InsightConfig();
    File sonatypeWork = temporaryFolder.newFolder("sonatype-work");
    File clusterDirectory = temporaryFolder.newFolder("cluster-dir");
    insightConfig.setSonatypeWork(sonatypeWork.getAbsolutePath());
    insightConfig.setClusterDirectory(clusterDirectory.getAbsolutePath());
    workDirectoriesOperationalCheck = new WorkDirectoriesOperationalCheck(insightConfig);
  }

  @Test
  public void testCheck() {
    Health health = workDirectoriesOperationalCheck.check();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  public void testCheck_SonatypeWork_NotADirectory() {
    insightConfig.setSonatypeWork("doesNotExist");

    Health health = workDirectoriesOperationalCheck.check();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails().get("message")).isEqualTo(insightConfig.getSonatypeWork() + " is not a directory");
  }

  @Test
  public void testCheck_ClusterDirectory_NotADirectory() {
    insightConfig.setClusterDirectory("doesNotExist");

    Health health = workDirectoriesOperationalCheck.check();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails().get("message"))
        .isEqualTo(insightConfig.getClusterDirectory() + " is not a directory");
  }
}
