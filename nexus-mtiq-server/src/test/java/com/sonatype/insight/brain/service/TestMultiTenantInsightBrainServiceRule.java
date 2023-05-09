/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URL;
import java.util.List;

import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import com.google.inject.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestInsightBrainService as Junit ExternalResource (which is a Junit TestRule).
 *
 * @since 1.9.1
 */
public class TestMultiTenantInsightBrainServiceRule
    extends TestInsightBrainServiceRule
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  public TestMultiTenantInsightBrainServiceRule(
      int port,
      int adminPort,
      String hdsUrl,
      DatabaseContainer databaseContainer,
      boolean isHdsProxyRequired,
      List<Module> modules)
  {
    super(port, adminPort, hdsUrl, databaseContainer, isHdsProxyRequired, modules);
  }

  @Override
  public void start() throws Exception {
    long start = System.currentTimeMillis();

    log.info("Starting TestInsightBrainService on port {}, admin port {}", port, adminPort);
    brain = new TestMultiTenantInsightBrainService();
    brain.setHttpPort(port);
    brain.setHttpAdminPort(adminPort);
    if (hdsUrl != null) {
      brain.setHdsUrl(hdsUrl);
    }
    brain.setDatabaseContainer(databaseContainer);
    if (isHdsProxyRequired) {
      brain.setProxyServerConfiguration("127.0.0.1", new URL(hdsUrl).getPort(), "proxyuser", "proxypass");
    }
    else {
      // Clear any proxy config set by previous tests
      brain.clearProxyServerConfiguration();
    }
    if (modules != null) {
      brain.addModules(modules);
    }
    brain.setConfigurator(configurator);
    brain.start();

    log.info("Started TestInsightBrainService in {} ms.", System.currentTimeMillis() - start);
  }

  @Override
  public TestMultiTenantInsightBrainServiceRule setConfigurator(Configurator configurator) {
    this.configurator = configurator;
    return this;
  }

  //Returns a mocked MultiTenantJwkProvider
  public MultiTenantJwkProvider getMultiTenantJwkTestProvider() {
    TestMultiTenantInsightBrainService testMultiTenantBrain = (TestMultiTenantInsightBrainService) brain;
    return testMultiTenantBrain.getMultitenantJwkProvider();
  }
}
