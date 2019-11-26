/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;

import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.test.networking.PortAllocator;

import com.google.inject.Module;

/**
 * Test helper for CLM server. It wraps and manages a CLM brain service/server and a mocked HDS server.
 * 
 * @since 1.11
 */
public class TestCLMServer
{
  private final HdsMockServerRule hdsMockServer;

  private final boolean hdsMockServerOwned;

  private final TestInsightBrainServiceRule brain;

  private final boolean isProxyRequiredToReachHds;

  private boolean running;

  private static int startCount;

  private static int stopCount;

  private static int totalStartTime;

  private static int totalStopTime;

  public TestCLMServer(boolean isProxyRequiredToReachHds,
                       List<Module> modules,
                       Configurator configurator,
                       HdsMockServerRule hdsMockServer)
  {
    this.isProxyRequiredToReachHds = isProxyRequiredToReachHds;

    this.hdsMockServer = hdsMockServer;
    hdsMockServerOwned = false;

    brain = new TestInsightBrainServiceRule(PortAllocator.nextFreePort(), PortAllocator.nextFreePort(),
        hdsMockServer.getHttpUrl(), isProxyRequiredToReachHds, modules).setConfigurator(configurator);
  }

  public TestCLMServer(boolean isProxyRequiredToReachHds, List<Module> modules, Configurator configurator) {
    this.isProxyRequiredToReachHds = isProxyRequiredToReachHds;

    int hdsMockServerPort = PortAllocator.nextFreePort();

    hdsMockServer = new HdsMockServerRule(hdsMockServerPort, isProxyRequiredToReachHds);
    hdsMockServerOwned = true;

    brain = new TestInsightBrainServiceRule(PortAllocator.nextFreePort(), PortAllocator.nextFreePort(),
        "http://localhost:" + hdsMockServerPort, isProxyRequiredToReachHds, modules)
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
