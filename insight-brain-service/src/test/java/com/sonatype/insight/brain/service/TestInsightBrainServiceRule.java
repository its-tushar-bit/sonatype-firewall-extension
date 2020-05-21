/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

import com.google.inject.Module;
import org.junit.rules.ExternalResource;

/**
 * TestInsightBrainService as Junit ExternalResource (which is a Junit TestRule).
 * 
 * @since 1.9.1
 */
public class TestInsightBrainServiceRule
    extends ExternalResource
{
  private final int port;

  private final int adminPort;

  private final String hdsUrl;

  private final boolean isHdsProxyRequired;

  private final List<Module> modules;

  private Configurator configurator;

  private TestInsightBrainService brain;

  public TestInsightBrainServiceRule(int port,
                                     int adminPort,
                                     String hdsUrl,
                                     boolean isHdsProxyRequired,
                                     List<Module> modules)
  {
    this.port = port;
    this.adminPort = adminPort;
    this.hdsUrl = hdsUrl;
    this.isHdsProxyRequired = isHdsProxyRequired;
    this.modules = modules;
  }

  @Override
  protected void before() throws Throwable {
    start();
  }

  @Override
  protected void after() {
    stop();
  }

  void start() throws Exception {
    long start = System.currentTimeMillis();

    System.out.println("Starting TestInsightBrainService on port " + port + ", admin port " + adminPort);
    brain = new TestInsightBrainService();
    brain.setHttpPort(port);
    brain.setHttpAdminPort(adminPort);
    if (hdsUrl != null) {
      brain.setHdsUrl(hdsUrl);
    }
    if (isHdsProxyRequired) {
      brain.setProxyServerConfiguration("127.0.0.1", new URL(hdsUrl).getPort(), "proxyuser", "proxypass");
    }
    if (modules != null) {
      brain.addModules(modules);
    }
    brain.setConfigurator(configurator);
    brain.start();

    System.out.println("Started TestInsightBrainService in " + (System.currentTimeMillis() - start) + " ms.");
  }

  void stop() {
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

    System.out.println("Stopped test servers in " + (System.currentTimeMillis() - start) + " ms.");
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

  public File getOrganizationIconDir() {
    return brain.getOrganizationIconDir();
  }

  public File getApplicationIconDir() {
    return brain.getApplicationIconDir();
  }

  public <T> T getInstance(Class<T> type) {
    if (brain.getInjector() == null) {
      return null;
    }
    return brain.getInstance(type);
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

  public Configurator getConfigurator() {
    return configurator;
  }

  public TestInsightBrainServiceRule setConfigurator(Configurator configurator) {
    this.configurator = configurator;
    return this;
  }
}
