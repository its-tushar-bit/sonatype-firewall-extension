/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.testing.InsightBrainServiceFactory;
import com.sonatype.insight.test.networking.PortAllocator;

import com.google.inject.Module;

/**
 * Test helper for CLM server. It wraps and manages a CLM brain service/server and a mocked HDS server.
 *
 * @since 1.11
 */
public class TestCLMServer
{
  /**
   * When the system property {@code functional-test-webpack-dev-server} is set to {@code true}, the functional test
   * server uses fixed ports (8072 for the application, 8073 for admin) matching the default webpack-dev-server proxy
   * target. This enables a fast frontend development loop: run {@code yarn start} in insight-brain-frontend (serves on
   * port 8070 and proxies API calls to 8072), then run a functional test with
   * {@code -Dfunctional-test-webpack-dev-server=true} so the browser points at the webpack-dev-server for instant
   * frontend rebuilds while API calls are handled by the test server.
   */
  public static final boolean WEBPACK_DEV_MODE = Boolean.getBoolean("functional-test-webpack-dev-server");

  private static final int WEBPACK_WEBPACK_DEV_MODE_APP_PORT = 8072;

  private static final int WEBPACK_WEBPACK_DEV_MODE_ADMIN_PORT = 8073;

  private final HdsMockServerRule hdsMockServer;

  private final boolean hdsMockServerOwned;

  private final TestInsightBrainServiceRule brain;

  private final boolean isProxyRequiredToReachHds;

  private boolean running;

  private static int startCount;

  private static int stopCount;

  private static int totalStartTime;

  private static int totalStopTime;

  public TestCLMServer(
      InsightBrainServiceFactory insightBrainServiceFactory,
      boolean isProxyRequiredToReachHds,
      List<Module> modules,
      Configurator configurator,
      HdsMockServerRule hdsMockServer,
      DatabaseContainer databaseContainer)
  {
    this(isProxyRequiredToReachHds, hdsMockServer,
        new TestInsightBrainServiceRule(
            insightBrainServiceFactory,
            PortAllocator.nextFreePort(),
            PortAllocator.nextFreePort(),
            hdsMockServer.getHttpUrl(),
            databaseContainer,
            isProxyRequiredToReachHds,
            modules).setConfigurator(configurator));
  }

  public TestCLMServer(
      boolean isProxyRequiredToReachHds,
      HdsMockServerRule hdsMockServer,
      TestInsightBrainServiceRule brain)
  {
    this.isProxyRequiredToReachHds = isProxyRequiredToReachHds;

    this.hdsMockServer = hdsMockServer;
    hdsMockServerOwned = false;

    this.brain = brain;
  }

  public TestCLMServer(
      InsightBrainServiceFactory insightBrainServiceFactory,
      boolean isProxyRequiredToReachHds,
      List<Module> modules,
      Configurator configurator)
  {
    // null for DatabaseContainer indicates that the default one will be created
    // See TestInsightBrainService#createDatabaseContainer
    this(insightBrainServiceFactory, isProxyRequiredToReachHds, modules, configurator, /* DatabaseContainer */ null);
  }

  public TestCLMServer(
      InsightBrainServiceFactory insightBrainServiceFactory,
      boolean isProxyRequiredToReachHds,
      List<Module> modules,
      Configurator configurator,
      DatabaseContainer databaseContainer)
  {
    this.isProxyRequiredToReachHds = isProxyRequiredToReachHds;

    int hdsMockServerPort = PortAllocator.nextFreePort();

    hdsMockServer = new HdsMockServerRule(hdsMockServerPort, isProxyRequiredToReachHds);
    hdsMockServerOwned = true;

    int appPort = WEBPACK_DEV_MODE ? WEBPACK_WEBPACK_DEV_MODE_APP_PORT : PortAllocator.nextFreePort();
    int adminPort = WEBPACK_DEV_MODE ? WEBPACK_WEBPACK_DEV_MODE_ADMIN_PORT : PortAllocator.nextFreePort();

    brain = new TestInsightBrainServiceRule(insightBrainServiceFactory,
        appPort, adminPort,
        "http://localhost:" + hdsMockServerPort, databaseContainer, isProxyRequiredToReachHds, modules)
            .setConfigurator(configurator);
  }

  public void start() throws Exception {
    long start = System.currentTimeMillis();
    startCount++;

    if (hdsMockServerOwned) {
      hdsMockServer.start();
    }
    brain.start();
    running = true;

    long startTime = System.currentTimeMillis() - start;
    totalStartTime += startTime;

    System.out.println("Started " + TestCLMServer.class.getSimpleName() + " " + startCount + " times. This start time="
        + startTime + "ms. Total start time=" + totalStartTime + "ms. Total stop time=" + totalStopTime);
  }

  public void stop() {
    long start = System.currentTimeMillis();
    stopCount++;

    running = false;
    brain.stop();
    if (hdsMockServerOwned) {
      hdsMockServer.stop();
    }

    long stopTime = System.currentTimeMillis() - start;
    totalStopTime += stopTime;

    System.out.println("Stopped " + TestCLMServer.class.getSimpleName() + " " + stopCount + " times. This stop time="
        + stopTime + "ms. Total start time=" + totalStartTime + "ms. Total stop time=" + totalStopTime);
  }

  public boolean isRunning() {
    return running;
  }

  public TestInsightBrainServiceRule getCLMServer() {
    return brain;
  }

  public HdsMockServerRule getHdsServer() {
    return hdsMockServer;
  }

  public boolean isReusable(boolean proxyRequired, Configurator configurator) {
    return proxyRequired == isProxyRequiredToReachHds && configurator == brain.getConfigurator();
  }
}
