/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.nio.file.Files;

@Named
@Singleton
public class WorkDirectoryBootstrap
    implements InitializingBean, DisposableBean
{
  private final InsightConfig insightConfig;

  private volatile InsightFileLock insightFileLock;

  @Inject
  public WorkDirectoryBootstrap(InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Files.createDirectories(insightConfig.getSonatypeWork().toPath());
    Files.createDirectories(insightConfig.getClusterDirectory().toPath());

    insightFileLock = new InsightFileLock(insightConfig);
    insightFileLock.lock();
  }

  @Override
  public void destroy() {
    if (insightFileLock != null) {
      insightFileLock.release();
      insightFileLock = null;
    }
  }
}
