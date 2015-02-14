/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;

import com.google.inject.Module;

/**
 * Test helper for CLM server. It wraps and manages a CLM brain service/server and a mocked HDS server.
 * 
 * @since 1.11
 */
public class TestCLMServer
{
  private final InsightMockServerRule insightMockServer;

  private final TestInsightBrainServiceRule brain;

  private final boolean isProxyRequiredToReachHds;

  private static int startCount;

  private static int stopCount;

  private static int totalStartTime;

  private static int totalStopTime;

  TestCLMServer(boolean isProxyRequiredToReachHds, List<Module> modules) {
    this.isProxyRequiredToReachHds = isProxyRequiredToReachHds;

    int insightMockServerPort = PortAllocator.findFreePort(8090);

    insightMockServer = new InsightMockServerRule(insightMockServerPort, isProxyRequiredToReachHds);
    brain = new TestInsightBrainServiceRule(PortAllocator.findFreePort(8070), PortAllocator.findFreePort(8071),
        null /* baseUrl */, "http://localhost:" + insightMockServerPort, isProxyRequiredToReachHds, modules);
  }

  public void start() throws Throwable {
    long start = System.currentTimeMillis();
    startCount++;

    insightMockServer.start();
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
    insightMockServer.stop();

    long stopTime = System.currentTimeMillis() - start;
    totalStopTime += stopTime;

    System.out.println("Stopped " + TestCLMServer.class.getSimpleName() + " " + stopCount + " times. This stop time="
        + stopTime + "ms. Total start time=" + totalStartTime + "ms. Total stop time=" + totalStopTime);
  }

  public TestInsightBrainServiceRule getCLMServer() {
    return brain;
  }

  public InsightMockServerRule getInsightServer() {
    return insightMockServer;
  }

  public boolean isProxyRequiredToReachHds() {
    return isProxyRequiredToReachHds;
  }
}
