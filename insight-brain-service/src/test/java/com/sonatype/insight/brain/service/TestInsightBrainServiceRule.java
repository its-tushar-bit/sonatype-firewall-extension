/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.testing.InsightBrainServiceFactory;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import java.net.URL;
import java.util.List;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * TestInsightBrainService as Junit ExternalResource (which is a Junit TestRule).
 *
 * <p>
 * <b>Migration Note:</b> This rule now drives the Spring-based test server infrastructure.
 * The legacy injector lookup has been replaced with getApplicationContext().
 * The old module list has been replaced with testConfigurations.
 * </p>
 *
 * @since 1.9.1
 */
public class TestInsightBrainServiceRule
    extends ExternalResource
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final InsightBrainServiceFactory insightBrainServiceFactory;

  protected final int port;

  protected final int adminPort;

  protected final String hdsUrl;

  protected final DatabaseContainer databaseContainer;

  protected final boolean isHdsProxyRequired;

  protected final List<Class<?>> testConfigurations;

  protected Configurator configurator;

  protected TestInsightBrainService brain;

  private DAOFactory daoFactory;

  private String keyStorePath;

  private String keyStorePassword;

  public TestInsightBrainServiceRule(
      InsightBrainServiceFactory insightBrainServiceFactory,
      int port,
      int adminPort,
      String hdsUrl,
      DatabaseContainer databaseContainer,
      boolean isHdsProxyRequired,
      List<Class<?>> testConfigurations)
  {
    this.insightBrainServiceFactory = insightBrainServiceFactory;
    this.port = port;
    this.adminPort = adminPort;
    this.hdsUrl = hdsUrl;
    this.databaseContainer = databaseContainer;
    this.isHdsProxyRequired = isHdsProxyRequired;
    this.testConfigurations = testConfigurations;
    daoFactory = new TestDAOFactory(databaseContainer);
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

    brain = insightBrainServiceFactory.createTestInsightBrainService();
    log.info("Starting {} on port {}, admin port {}", brain.getClass().getSimpleName(), port, adminPort);

    brain.setHttpPort(port);
    brain.setHttpAdminPort(adminPort);
    brain.setKeyStore(keyStorePath, keyStorePassword);
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
    brain.addTestConfigurations(testConfigurations);
    brain.setConfigurator(configurator);

    // Need to set this configuration on DB before server start
    setProxyConfigurationOnDB();

    brain.start();

    log.info("Started TestInsightBrainService in {} ms.", System.currentTimeMillis() - start);
  }

  private void setProxyConfigurationOnDB() {
    ProxyServerConfiguration proxyServerConfiguration = getProxyServerConfiguration();
    ProxyServerConfigurationDAO proxyServerConfigurationDAO = daoFactory.createProxyServerConfigurationDAO();
    if (proxyServerConfiguration != null) {
      proxyServerConfigurationDAO.set(proxyServerConfiguration);
    }
    else {
      proxyServerConfigurationDAO.delete();
    }
  }

  public void setHdsUrl() {
    ApiConfigurationService configurationService = getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL, hdsUrl);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);

    // refresh license details from the new HDS
    getInstance(CLMLicenseManager.class).loadLicense();
  }

  public void setProxyConfiguration() {
    setProxyConfigurationOnDB();

    if (isHdsProxyRequired) {
      getInstance(ApiProxyServerConfigurationService.class).applyProxyServerConfigurationToClients();
    }
  }

  public void setCspEnabled(boolean cspEnabled) {
    ApiConfigurationService configurationService = getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.CSP_ENABLED, cspEnabled);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.CSP_ENABLED);
  }

  public void stop() {
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
    if (!brain.isInitialized()) {
      return null;
    }
    return brain.getInstance(type);
  }

  /**
   * Get the Spring ApplicationContext.
   */
  public ApplicationContext getApplicationContext() {
    if (!brain.isInitialized()) {
      return null;
    }
    return brain.getApplicationContext();
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

  public TestInsightBrainServiceRule setKeyStore(String path, String password) {
    this.keyStorePath = path;
    this.keyStorePassword = password;
    if (brain != null) {
      brain.setKeyStore(path, password);
    }
    return this;
  }

  public void resetDisableForTesting() {
    if (brain != null && brain.isInitialized()) {
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

  public boolean isRunning() {
    return brain != null;
  }

  public boolean getIsHdsProxyRequired() {
    return isHdsProxyRequired;
  }
}
