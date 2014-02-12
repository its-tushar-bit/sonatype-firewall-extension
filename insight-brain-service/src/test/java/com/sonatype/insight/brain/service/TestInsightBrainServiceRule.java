/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.9.1
 */
public class TestInsightBrainServiceRule
    extends ExternalResource
{
  private static final Logger log = LoggerFactory.getLogger(TestInsightBrainServiceRule.class);

  private final int port;

  private final int adminPort;

  private final String baseUrl;

  private final String saasAddress;

  private final boolean isSaasProxyRequired;

  private final List<Module> modules;

  private TestInsightBrainService brain;

  public TestInsightBrainServiceRule(int port, int adminPort, String baseUrl, String saasAddress,
      boolean isSaasProxyRequired, List<Module> modules)
  {
    this.port = port;
    this.adminPort = adminPort;
    this.baseUrl = baseUrl;
    this.saasAddress = saasAddress;
    this.isSaasProxyRequired = isSaasProxyRequired;
    this.modules = modules;
  }

  @Override
  protected void before() throws Throwable {
    long start = System.currentTimeMillis();

    log.debug("Starting TestInsightBrainService on port {}, admin port {}", port, adminPort);
    brain = new TestInsightBrainService();
    brain.setHttpPort(port);
    brain.setHttpAdminPort(adminPort);
    if (baseUrl != null) {
      brain.setBaseUrl(baseUrl);
    }
    if (saasAddress != null) {
      brain.setSaasAddress(saasAddress);
    }
    if (isSaasProxyRequired) {
      brain.setProxyConfig("127.0.0.1", new URL(saasAddress).getPort(), "proxyuser", "proxypass");
    }
    if (modules != null) {
      brain.addModules(modules);
    }
    brain.start();

    log.debug("Started TestInsightBrainService in {}", System.currentTimeMillis() - start);
  }

  @Override
  protected void after() {
    long start = System.currentTimeMillis();

    if (brain != null) {
      try {
        brain.stop();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
      brain = null;
    }

    log.debug("Stopped test servers in {}", System.currentTimeMillis() - start);
  }

  public Configuration getClientConfiguration() {
    return brain.getClientConfiguration();
  }

  public File getAuditDir(String appId) {
    return brain.getAuditDir(appId);
  }

  public File getDataDir() {
    return brain.getDataDir();
  }

  public Injector getInjector() {
    return brain.getInjector();
  }

  public File getReportDir(String appId, String scanId) {
    return brain.getReportDir(appId, scanId);
  }

  public File getWorkDir() {
    return brain.getWorkDir();
  }

  public int getPort() {
    return port;
  }

  public InsightConfig getConfiguration() {
    return brain.getConfiguration();
  }
}
