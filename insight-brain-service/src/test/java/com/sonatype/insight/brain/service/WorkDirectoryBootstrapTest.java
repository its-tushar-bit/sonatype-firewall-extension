/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class WorkDirectoryBootstrapTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void shouldCreateDirectoriesAndReleaseLockOnShutdown() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    File sonatypeWork = new File(temporaryFolder.getRoot(), "work/nexus-iq");
    File clusterDirectory = new File(temporaryFolder.getRoot(), "cluster/nexus-iq");
    insightConfig.setSonatypeWork(sonatypeWork.getAbsolutePath());
    insightConfig.setClusterDirectory(clusterDirectory.getAbsolutePath());

    WorkDirectoryBootstrap bootstrap = new WorkDirectoryBootstrap(insightConfig);
    bootstrap.afterPropertiesSet();

    assertThat(sonatypeWork).exists().isDirectory();
    assertThat(clusterDirectory).exists().isDirectory();
    assertThat(new File(sonatypeWork, "lock")).exists();

    bootstrap.destroy();

    InsightFileLock verificationLock = new InsightFileLock(insightConfig);
    verificationLock.lock();
    verificationLock.release();
  }

  @Test
  public void shouldFailFastWhenWorkDirectoryIsAlreadyLocked() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    File sonatypeWork = temporaryFolder.newFolder("locked-work");
    File clusterDirectory = temporaryFolder.newFolder("locked-cluster");
    insightConfig.setSonatypeWork(sonatypeWork.getAbsolutePath());
    insightConfig.setClusterDirectory(clusterDirectory.getAbsolutePath());

    InsightFileLock existingLock = new InsightFileLock(insightConfig);
    existingLock.lock();

    WorkDirectoryBootstrap bootstrap = new WorkDirectoryBootstrap(insightConfig);
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(bootstrap::afterPropertiesSet)
        .withMessage("Work directory " + sonatypeWork.getAbsolutePath() + " is already in use.");

    bootstrap.destroy();
    existingLock.release();
  }
}
