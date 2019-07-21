/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.mock.hds.HdsMockResponse;
import com.sonatype.insight.mock.hds.HdsMockServer;

import org.junit.rules.ExternalResource;

/**
 * @since 1.9.1
 */
public class HdsMockServerRule
    extends ExternalResource
{
  private final int port;

  private final boolean isProxyRequired;

  protected HdsMockServer hdsMockServer;

  public HdsMockServerRule() {
    this(0, false);
  }

  public HdsMockServerRule(int port, boolean isProxyRequired) {
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

    System.out.println("Starting HDS mock on port " + port);
    hdsMockServer = new HdsMockServer();
    hdsMockServer.setHttpPort(port);
    if (isProxyRequired) {
      hdsMockServer.setKeyStore(System.getProperty("javax.net.ssl.trustStore"), "server-pwd");
      hdsMockServer.setProxyAuthentication("proxyuser", "proxypass");
    }
    hdsMockServer.start();
    System.out.println("Started HDS mock on port " + hdsMockServer.getHttpPort() + " in "
        + (System.currentTimeMillis() - start) + " ms.");
  }

  public void stop() {
    long start = System.currentTimeMillis();
    if (hdsMockServer != null) {
      hdsMockServer.stop();
      hdsMockServer = null;
    }

    System.out.println("Stopped HDS mock in " + (System.currentTimeMillis() - start) + " ms.");
  }

  public HdsMockResponse respondWith(Object body) {
    return hdsMockServer.respondWith(body);
  }

  public void reset() {
    hdsMockServer.reset();
  }

  public boolean isReusable(boolean isProxyRequired) {
    return this.isProxyRequired == isProxyRequired;
  }

  public String getHttpUrl() {
    return hdsMockServer.getHttpUrl();
  }
}
