/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import com.sonatype.insight.mock.InsightMockServer;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.9.1
 */
public class InsightMockServerRule
    extends ExternalResource
{
  private static final Logger log = LoggerFactory.getLogger(InsightMockServerRule.class);

  private final int port;

  private final boolean isProxyRequired;

  protected InsightMockServer insightMockServer;

  public InsightMockServerRule(int port, boolean isProxyRequired) {
    this.port = port;
    this.isProxyRequired = isProxyRequired;
  }

  @Override
  protected void before() throws Throwable {
    start();
  }

  @Override
  protected void after() {
    stop();
  }

  public void start() throws Exception {
    long start = System.currentTimeMillis();

    log.debug("Starting InsightMockServer on port {}", port);
    insightMockServer = new InsightMockServer();
    insightMockServer.setHttpPort(port);
    if (isProxyRequired) {
      insightMockServer.setKeyStore(System.getProperty("javax.net.ssl.trustStore"), "server-pwd");
      insightMockServer.setProxyAuthentication("proxyuser", "proxypass");
    }
    insightMockServer.start();
    log.debug("Started InsightMockServer in {}", System.currentTimeMillis() - start);
  }

  public void stop() {
    long start = System.currentTimeMillis();
    if (insightMockServer != null) {
      insightMockServer.stop();
      insightMockServer = null;
    }

    log.debug("Stopped InsightMockServer in {}", System.currentTimeMillis() - start);
  }

  void setResponseForURI(String uri, Object body, int status) {
    insightMockServer.setResponseForURI(uri, body, status);
  }

  public void reset() throws IOException {
    insightMockServer.reset();
  }
}
