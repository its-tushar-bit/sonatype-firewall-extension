/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.test.InjectedTest;
import com.sonatype.insight.test.networking.PortAllocator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HdsClientProxyTimeOutTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Inject
  private PasswordHandler passwordHandler;

  private InsightConfig config;

  private TelemetryId telemetryId;

  private ProductLicense productLicense;

  private InsightProxy insightProxy;

  private int port;

  private final CountDownLatch countDownLatch = new CountDownLatch(1);

  private final AtomicReference<Exception> serverException = new AtomicReference<>();

  private volatile String requestMethod = "";

  private Runnable nonResponsiveServer = () -> {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      countDownLatch.countDown();
      try (Socket socket = serverSocket.accept();
           BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

        requestMethod = br.readLine();
        while ((br.readLine()) != null) {
          // consume all incoming data
        }

        // This is a server that just sleeps and never responds
        try {
          Thread.sleep(10000);
        }
        catch (InterruptedException ignored) {
          // This is expected
        }
      }
    }
    catch (IOException e) {
      serverException.set(e);
      countDownLatch.countDown();
    }
  };

  private Thread nonResponsiveServerThread;

  @Before
  public void init() {
    port = PortAllocator.nextFreePort();

    tempEntity.setProxyServerConfiguration("localhost", port);

    config = new InsightConfig();
    config.setHdsUrl("https://www.example.com/");
    config.setConnectTimeoutInSeconds(1);
    telemetryId = new TelemetryId(config);

    productLicense = mock(ProductLicense.class);
    when(productLicense.getFingerprint()).thenReturn("license-fingerprint");

    nonResponsiveServerThread = new Thread(nonResponsiveServer);
    insightProxy = new InsightProxy(config, new ProxyServerConfigurationDAO(), passwordHandler);
  }

  @After
  public void exit() {
    nonResponsiveServerThread.interrupt();
  }

  @Test(timeout = 5000)
  public void testMustTimeOutAndNotWaitForever() throws InterruptedException {
    HdsClient client = new DefaultHdsClient(insightProxy, productLicense, config,
        new VersionService(), telemetryId, 20);

    nonResponsiveServerThread.start();

    // Give time to server thread to start and the ServerSocket to start listening
    countDownLatch.await(2500, TimeUnit.MILLISECONDS);
    if (serverException.get() != null) {
      fail("Exception while starting server.", serverException.get());
    }

    assertThatExceptionOfType(BadGatewayException.class).isThrownBy(() -> client.get(String.class, ""));
    assertThat(requestMethod).startsWith("CONNECT");
  }
}
