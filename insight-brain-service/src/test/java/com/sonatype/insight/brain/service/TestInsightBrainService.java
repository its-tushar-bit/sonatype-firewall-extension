/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collection;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.client.utils.HttpClientUtils;

import com.google.inject.Injector;
import com.google.inject.Module;

public interface TestInsightBrainService
{
  String DEFAULT_CONFIG_FILE_PATH = "target/test-classes/config-test.yml";

  interface Configurator
  {
    void configure(InsightConfig config);

    default String getConfigFilePath() {
      return DEFAULT_CONFIG_FILE_PATH;
    }
  }

  void setHttpPort(int port);

  void setHttpAdminPort(int port);

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

  void addModules(Collection<Module> modules);

  Injector getInjector();

  <C> C getInstance(Class<C> type);

  boolean isInitialized();
}
