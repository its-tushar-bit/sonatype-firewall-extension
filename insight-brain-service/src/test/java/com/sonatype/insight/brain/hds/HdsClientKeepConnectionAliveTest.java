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
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.hds.util.TelemetryTestUtils;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.insight.test.networking.PortAllocator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
@RunWith(MockitoJUnitRunner.class)
public class HdsClientKeepConnectionAliveTest
    extends BrainInjectedTest
{
  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  private Configuration configuration;

  @Inject
  private ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  @Inject
  private ApiProxyServerConfigurationService proxyServerConfigurationService;

  private InsightConfig config;

  private TelemetryId telemetryId;

  private ProductLicense mockProductLicense;

  private InsightProxy insightProxy;

  private int port;

  private final CountDownLatch countDownLatch = new CountDownLatch(1);

  private final AtomicReference<Exception> serverException = new AtomicReference<>();

  private final Runnable stallingServer = () -> {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      countDownLatch.countDown();
      try (Socket socket = serverSocket.accept();
          BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          PrintWriter pw = new PrintWriter(socket.getOutputStream()))
      {

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
    port = PortAllocator.nextFreePort();

    // Clear any proxy configuration leaked by a prior test in the same fork. applyConfigurationToClients below only
    // refreshes the named properties, so a stale proxy in the shared Configuration cache would otherwise route this
    // request through a dead port and fail with "Connection refused".
    proxyServerConfigurationDAO.delete();
    proxyServerConfigurationService.applyProxyServerConfigurationToClients();

    config = new InsightConfig();
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL,
        "http://localhost:" + port);
    // Need to use the DAO since the service doesn't allow connect timeout to be below 5
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS, "1");
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL,
        SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS);
    var mockClusterIdentificationService = TelemetryTestUtils.setupReflectiveMockClusterIdentificationService();
    telemetryId = new TelemetryId(config, systemConfigurationPropertyDAO, mockClusterIdentificationService);

    mockProductLicense = mock(ProductLicense.class);
    when(mockProductLicense.isValid()).thenReturn(true);
    when(mockProductLicense.getFingerprint()).thenReturn("license-fingerprint");

    stallingServerThread = new Thread(stallingServer);
    insightProxy = new InsightProxy(configuration, passwordHandler);
  }

  @After
  public void exit() {
    stallingServerThread.interrupt();
    try {
      configurationService.deleteConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL,
          SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS);
    }
    finally {
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL,
          SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS);
    }
  }

  @Test
  public void testConnectTimeoutMustNotAffectRequestConfigSocketTimeout() throws InterruptedException {
    HdsClient client = new HdsClient(insightProxy, mockProductLicense, configuration, new DefaultVersionService(),
        telemetryId, null, 20, name -> new Retry(name, 0, null, e -> false, i -> Duration.ZERO));

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
