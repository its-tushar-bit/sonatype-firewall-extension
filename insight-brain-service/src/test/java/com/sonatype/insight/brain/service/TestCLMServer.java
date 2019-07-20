/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.nio.channels.FileLock;
import java.util.List;

import com.sonatype.insight.mock.hds.HdsMockServer.HdsConfigurator;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

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

  private static final File NEXT_PORT_FILE = new File(System.getProperty("java.io.tmpdir"), "nx-test-port-allocator");

  /**
   * Unlike the current PortAllocator, this does deliberately NOT pick ephemeral ports which are prone to immediate
   * reuse by the OS for a different purpose the moment we release the found port (to be used for the intended purpose).
   * Instead, this manually scans a range which doesn't overlap with ephemeral ports and uses a shared temporary file to
   * collaborate with other/forked JVMs. The key feature of this port allocation is that a found port is not
   * reused/refound until the entire port range is exhausted.
   */
  private static synchronized int nextFreePort() {
    int minPort = 10000;
    int maxPort = 30000;
    try (RandomAccessFile raf = new RandomAccessFile(NEXT_PORT_FILE, "rw"); FileLock lock = raf.getChannel().lock()) {
      int nextPort = raf.length() < 4 ? minPort : raf.readInt();
      try {
        while (true) {
          if (nextPort > maxPort) {
            nextPort = minPort;
          }
          try (ServerSocket socket = new ServerSocket(nextPort++)) {
            return socket.getLocalPort();
          }
          catch (BindException e) {
            // port blocked, try the next one
          }
        }
      }
      finally {
        raf.seek(0);
        raf.writeInt(nextPort);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public TestCLMServer(boolean isProxyRequiredToReachHds,
                       List<Module> modules,
                       Configurator configurator,
                       HdsConfigurator hdsConfigurator)
  {
    this.isProxyRequiredToReachHds = isProxyRequiredToReachHds;

    int hdsMockServerPort = nextFreePort();

    hdsMockServer = new HdsMockServerRule(hdsMockServerPort, isProxyRequiredToReachHds)
        .setConfigurator(hdsConfigurator);

    brain = new TestInsightBrainServiceRule(nextFreePort(), nextFreePort(),
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
