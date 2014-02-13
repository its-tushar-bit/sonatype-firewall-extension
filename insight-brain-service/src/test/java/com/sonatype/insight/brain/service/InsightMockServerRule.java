/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

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

  private final File workDir;

  private final boolean isProxyRequired;

  protected InsightMockServer insightMockServer;

  public InsightMockServerRule(int port, File workDir, boolean isProxyRequired) {
    this.port = port;
    this.workDir = workDir;
    this.isProxyRequired = isProxyRequired;
  }

  @Override
  protected void before() throws Throwable {
    long start = System.currentTimeMillis();

    log.debug("Starting InsightMockServer on port {}", port);
    insightMockServer = new InsightMockServer();
    insightMockServer.setHttpPort(port);
    insightMockServer.setJsonResponseDirectory(getJsonResponseDirectory());
    insightMockServer.setZipResponseDirectory(getZipResponseDirectory());
    if (isProxyRequired) {
      insightMockServer.setKeyStore(System.getProperty("javax.net.ssl.trustStore"), "server-pwd");
      insightMockServer.setProxyAuthentication("proxyuser", "proxypass");
    }
    insightMockServer.start();
    log.debug("Started InsightMockServer in {}", System.currentTimeMillis() - start);
  }

  @Override
  protected void after() {
    stop();
  }

  public void stop() {
    long start = System.currentTimeMillis();
    if (insightMockServer != null) {
      insightMockServer.stop();
      insightMockServer = null;
    }

    log.debug("Stopped InsightMockServer in {}", System.currentTimeMillis() - start);
  }

  private File getJsonResponseDirectory() {
    return new File(workDir, "json");
  }

  private File getZipResponseDirectory() {
    return new File(workDir, "zip");
  }

  void setResponseForURI(String uri, Object body, int status) {
    insightMockServer.setResponseForURI(uri, body, status);
  }

  void setResponseForURI(String uri, String body, int status) {
    insightMockServer.setResponseForURI(uri, body, status);
  }
}
