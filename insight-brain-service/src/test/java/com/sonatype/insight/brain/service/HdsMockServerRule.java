/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Map;

import com.sonatype.insight.mock.hds.HdsMockResponse;
import com.sonatype.insight.mock.hds.HdsMockServer;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.9.1
 */
public class HdsMockServerRule
    extends ExternalResource
{
  private final Logger log = LoggerFactory.getLogger(getClass());

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

    log.info("Starting HDS mock on port {}", port);
    hdsMockServer = new HdsMockServer();
    hdsMockServer.setHttpPort(port);
    if (isProxyRequired) {
      hdsMockServer.setKeyStore(System.getProperty("javax.net.ssl.trustStore"), "server-pwd");
      hdsMockServer.setProxyAuthentication("proxyuser", "proxypass");
    }
    hdsMockServer.start();
    log.info("Started HDS mock on port {} in {} ms.", hdsMockServer.getHttpPort(), System.currentTimeMillis() - start);
  }

  public void stop() {
    long start = System.currentTimeMillis();
    if (hdsMockServer != null) {
      hdsMockServer.stop();
      hdsMockServer = null;
    }

    log.info("Stopped HDS mock in {} ms.", System.currentTimeMillis() - start);
  }

  public HdsMockResponse respondWith(Object body) {
    return hdsMockServer.respondWith(body);
  }

  public Map<String, String> getCapturedRequestHttpHeaders(String uri) {
    return hdsMockServer.getCapturedRequestHttpHeaders(uri);
  }

  public String getCapturedRequestBody(String uri) {
    return hdsMockServer.getCapturedRequestBody(uri);
  }

  public void reset() {
    hdsMockServer.reset();
    log.info("Reset HDS mock on port {}", port);
  }

  public boolean isReusable(boolean isProxyRequired) {
    return this.isProxyRequired == isProxyRequired;
  }

  public String getHttpUrl() {
    return hdsMockServer.getHttpUrl();
  }
}
