/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.client.utils.HttpClientUtils;
import java.util.List;
import org.springframework.context.ApplicationContext;

/**
 * Interface for test IQ server instances.
 *
 * <p>
 * <b>Migration Note:</b> This interface now reflects the Spring-based test server APIs.
 * The legacy injector lookup has been replaced with getApplicationContext().
 * The legacy override-module hook has been replaced with addTestConfigurations().
 * </p>
 */
public interface TestInsightBrainService
{
  String DEFAULT_CONFIG_FILE_PATH = "target/test-classes/config-test.yml";

  interface Configurator
  {
    void configure(InsightConfig config);

    default String getConfigFilePath() {
      return DEFAULT_CONFIG_FILE_PATH;
    }

    /**
     * The {@link Configurator} interface is used to determine if the test IQ instance needs to be restarted or not. The
     * default is 'false' since any test that uses a custom Configurator will want a clean IQ to start, however base
     * class usage will often want to return true.
     */
    default boolean isReusable() {
      return false;
    }
  }

  void setHttpPort(int port);

  void setHttpAdminPort(int port);

  void setKeyStore(String path, String password);

  void setHdsUrl(final String hdsUrl);

  void setDatabaseContainer(DatabaseContainer databaseContainer);

  ProxyServerConfiguration getTestProxyServerConfiguration();

  void setProxyServerConfiguration(String host, int port, String user, String pass);

  void clearProxyServerConfiguration();

  void setConfigurator(Configurator configurator);

  HttpClientUtils.Configuration getClientConfiguration();

  void start() throws Exception;

  void disableForTesting();

  void stop() throws Exception;

  InsightConfig getConfiguration();

  /**
   * Add test configuration classes to override production beans.
   */
  void addTestConfigurations(List<Class<?>> testConfigurations);

  /**
   * Get the Spring ApplicationContext.
   */
  ApplicationContext getApplicationContext();

  /**
   * Look up a bean by type.
   */
  <C> C getInstance(Class<C> type);

  boolean isInitialized();
}
