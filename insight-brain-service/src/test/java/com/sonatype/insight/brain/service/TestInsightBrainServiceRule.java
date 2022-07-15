/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URL;
import java.util.List;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

import com.google.inject.Module;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestInsightBrainService as Junit ExternalResource (which is a Junit TestRule).
 * 
 * @since 1.9.1
 */
public class TestInsightBrainServiceRule
    extends ExternalResource
{
  private final Logger log = LoggerFactory.getLogger(getClass());

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

    log.info("Starting TestInsightBrainService on port {}, admin port {}", port, adminPort);
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

    log.info("Started TestInsightBrainService in {} ms.", System.currentTimeMillis() - start);
  }

  public void setHdsUrl() {
    ApiConfigurationService configurationService = getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.HDS_URL, hdsUrl);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
  }

  public void setCspEnabled(boolean cspEnabled) {
    ApiConfigurationService configurationService = getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.CSP_ENABLED, cspEnabled);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.CSP_ENABLED);
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

    log.info("Stopped test servers in {} ms.", System.currentTimeMillis() - start);
  }

  public Configuration getClientConfiguration() {
    return brain.getClientConfiguration();
  }

  public <T> T getInstance(Class<T> type) {
    if (brain.getInjector() == null) {
      return null;
    }
    return brain.getInstance(type);
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
  
  public void resetDisableForTesting() {
    if (brain != null && brain.getInjector() != null) {
      brain.disableForTesting();
      log.info("Reset TestInsightBrainService");
    }
  }

  public ProxyServerConfiguration getProxyServerConfiguration() {
    if (brain == null) {
      return null;
    }
    return brain.getTestProxyServerConfiguration();
  }
}
