/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.api.v2.service.ApiProxyConfigurationServiceV2;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyConfigurationDAO;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.test.PortAllocator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HdsClientKeepConnectionAliveTest
{
  private InsightConfig config;

  private TelemetryId telemetryId;

  private CLMLicenseManager licenseManager;

  private InsightProxy insightProxy;

  private int port;

  private final CountDownLatch countDownLatch = new CountDownLatch(1);

  private final AtomicReference<Exception> serverException = new AtomicReference<>();

  private Runnable stallingServer = () -> {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      countDownLatch.countDown();
      try (Socket socket = serverSocket.accept();
           BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
           PrintWriter pw = new PrintWriter(socket.getOutputStream())) {

        while (!(br.readLine()).equals("")) {
          // consume all data
        }

        // This is a server that does some work for around 2 seconds before responding
        try {
          Thread.sleep(2000);
        }
        catch (InterruptedException ignored) {
          // This is expected
        }

        pw.write("HTTP/1.1 200 OK");
      }
    }
    catch (IOException e) {
      serverException.set(e);
      countDownLatch.countDown();
    }
  };

  private Thread stallingServerThread;

  @Before
  public void init() {
    port = PortAllocator.findFreePort(8090);

    config = new InsightConfig();
    config.setHdsUrl("http://localhost:" + port);
    config.setConnectTimeoutInSeconds(1);
    telemetryId = new TelemetryId(config);

    licenseManager = mock(CLMLicenseManager.class);
    when(licenseManager.getLicenseFingerprint()).thenReturn("license-fingerprint");
    ApiProxyConfigurationServiceV2 proxyConfigService = new ApiProxyConfigurationServiceV2(new ProxyConfigurationDAO());

    stallingServerThread = new Thread(stallingServer);
    insightProxy = new InsightProxy(config, proxyConfigService);
  }

  @After
  public void exit() {
    stallingServerThread.interrupt();
  }

  @Test
  public void testConnectTimeoutMustNotAffectRequestConfigSocketTimeout() throws InterruptedException {
    HdsClient client = new HdsClient(insightProxy, licenseManager, config, new VersionService(), telemetryId, 20);

    stallingServerThread.start();

    // Give time to server thread to start and the ServerSocket to start listening
    countDownLatch.await(2000, TimeUnit.MILLISECONDS);
    if (serverException.get() != null) {
      fail("Exception while starting server.", serverException.get());
    }

    // We have an HDS client configured with 1 second connect timeout
    // The server we are making our request to accepts the connection immediately but takes 2 seconds to respond
    // HDS Client must not timeout in 1 second and should wait for the response
    // If you want to make this test fail, set RequestConfig#socketTimeout to 1 sec in implementation
    // Another way would be to make the stalling thread wait for 16 minutes and this test should again fail
    // since the default RequestConfig#socketTimeout is 15 minutes
    client.get(String.class, "");
  }
}
