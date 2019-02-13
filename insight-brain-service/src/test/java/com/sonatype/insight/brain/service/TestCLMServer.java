/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;

import com.sonatype.insight.mock.hds.HdsMockServer.HdsConfigurator;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.test.PortAllocator;

import com.google.inject.Module;

/**
 * Test helper for CLM server. It wraps and manages a CLM brain service/server and a mocked HDS server.
 * 
 * @since 1.11
 */
public class TestCLMServer
{
  private final HdsMockServerRule hdsMockServer;

  private final TestInsightBrainServiceRule brain;

  private final boolean isProxyRequiredToReachHds;

  private static int startCount;

  private static int stopCount;

  private static int totalStartTime;

  private static int totalStopTime;

  public TestCLMServer(boolean isProxyRequiredToReachHds,
                       List<Module> modules,
                       Configurator configurator,
                       HdsConfigurator hdsConfigurator)
  {
    this.isProxyRequiredToReachHds = isProxyRequiredToReachHds;

    int hdsMockServerPort = PortAllocator.findFreePort(8090);

    hdsMockServer = new HdsMockServerRule(hdsMockServerPort, isProxyRequiredToReachHds)
        .setConfigurator(hdsConfigurator);

    brain = new TestInsightBrainServiceRule(PortAllocator.findFreePort(8070), PortAllocator.findFreePort(8071),
        "http://localhost:" + hdsMockServerPort, isProxyRequiredToReachHds, modules)
        .setConfigurator(configurator);
  }

  public TestCLMServer(boolean isProxyRequiredToReachHds, List<Module> modules, Configurator configurator) {
    this(isProxyRequiredToReachHds, modules, configurator, null);
  }

  public void start() throws Exception {
    long start = System.currentTimeMillis();
    startCount++;

    hdsMockServer.start();
    brain.start();

    long startTime = System.currentTimeMillis() - start;
    totalStartTime += startTime;

    System.out.println("Started " + TestCLMServer.class.getSimpleName() + " " + startCount + " times. This start time="
        + startTime + "ms. Total start time=" + totalStartTime + "ms. Total stop time=" + totalStopTime);
  }

  public void stop() {
    long start = System.currentTimeMillis();
    stopCount++;

    brain.stop();
    hdsMockServer.stop();

    long stopTime = System.currentTimeMillis() - start;
    totalStopTime += stopTime;

    System.out.println("Stopped " + TestCLMServer.class.getSimpleName() + " " + stopCount + " times. This stop time="
        + stopTime + "ms. Total start time=" + totalStartTime + "ms. Total stop time=" + totalStopTime);
  }

  public TestInsightBrainServiceRule getCLMServer() {
    return brain;
  }

  public HdsMockServerRule getHdsServer() {
    return hdsMockServer;
  }

  public boolean isReusable(boolean proxyRequired, Configurator configurator, HdsConfigurator hdsConfigurator) {
    return proxyRequired == isProxyRequiredToReachHds && configurator == brain.getConfigurator()
        && hdsConfigurator == hdsMockServer.getConfigurator();
  }

  public boolean isReusable(boolean proxyRequired, Configurator configurator) {
    return isReusable(proxyRequired, configurator, null);
  }
}
