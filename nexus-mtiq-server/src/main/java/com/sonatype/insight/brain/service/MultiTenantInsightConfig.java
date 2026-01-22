/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.sonatype.insight.brain.health.MultiTenantHealthFactory;
import com.sonatype.insight.brain.metrics.datadog.StatsdMetricsConfig;
import com.sonatype.insight.brain.service.config.MultiTenantStorageConfig;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.db.DatabaseConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.health.HealthFactory;

public class MultiTenantInsightConfig
    extends InsightConfig
{
  /**
   * Custom configs for the two MTIQ data sources. Note these are com.sonatype.insight.db.DatabaseConfig objects and not
   * com.sonatype.insight.service.DatabaseConfig. The latter does not have enough configuration attributes for
   * properties such as maxConnections.
   */
  @Valid
  @JsonProperty
  private DatabaseConfig mainDatabase;

  @Valid
  @JsonProperty
  private DatabaseConfig locksDatabase;

  @JsonProperty
  private boolean deleteBuiltInAdmin = true;

  @JsonProperty
  private boolean usingDefaultEncryptionKeyStore = false;

  @Valid
  @JsonProperty(value = "auth0")
  private Auth0Config auth0Config;

  @JsonProperty
  private String globalTenantEncryptionKeyName;

  @Nullable
  @JsonProperty
  private StatsdMetricsConfig statsdMetricsConfig;

  @JsonProperty
  private Path jemallocProfileDir = Path.of(".");

  @Valid
  @JsonProperty(value = "storage")
  private MultiTenantStorageConfig storage = new MultiTenantStorageConfig();

  @Override
  public MultiTenantStorageConfig getStorage() {
    return storage;
  }

  public void setStorage(final MultiTenantStorageConfig storage) {
    this.storage = storage;
  }

  @Override
  public File getSonatypeWork() {
    return new File(sonatypeWork, TenantThreadLocal.getTenant().tenantSlug);
  }

  @Override
  public File getClusterDirectory() {
    if (clusterDirectory == null) {
      throw new IllegalStateException("clusterDirectory is not set");
    }
    return new File(clusterDirectory, TenantThreadLocal.getTenant().tenantSlug);
  }

  @Override
  public com.sonatype.insight.brain.service.DatabaseConfig getDatabase() {
    // getDatabase() is called by Telemetry so we still need to provide a sensible response here
    com.sonatype.insight.brain.service.DatabaseConfig databaseConfig =
        new com.sonatype.insight.brain.service.DatabaseConfig();
    String jdbcUrl = getMainDatabase().getUrl().replace("jdbc:", ""); // drop 'jdbc:' to make it a valid URI
    URI uri = URI.create(jdbcUrl);
    databaseConfig.setName("mtiq");
    databaseConfig.setHostname(uri.getHost());
    databaseConfig.setPort(uri.getPort());
    databaseConfig.setType("postgres");
    return databaseConfig;
  }

  @Override
  public void setDatabase(final com.sonatype.insight.brain.service.DatabaseConfig database) {
    throw new RuntimeException("Cannot use 'database' config object in MTIQ. Use 'mainDatabase' instead.");
  }

  @Override
  public boolean isDatabaseEmbedded() {
    return false;
  }

  public DatabaseConfig getMainDatabase() {
    return mainDatabase;
  }

  public void setMainDatabase(final DatabaseConfig mainDatabase) {
    this.mainDatabase = mainDatabase;
  }

  public DatabaseConfig getLocksDatabase() {
    return locksDatabase;
  }

  public void setLocksDatabase(final DatabaseConfig locksDatabase) {
    this.locksDatabase = locksDatabase;
  }

  public boolean isDeleteBuiltInAdmin() {
    return deleteBuiltInAdmin;
  }

  public void setDeleteBuiltInAdmin(final boolean deleteBuiltInAdmin) {
    this.deleteBuiltInAdmin = deleteBuiltInAdmin;
  }

  public Auth0Config getAuth0Config() {
    return auth0Config;
  }

  public void setAuth0Config(final Auth0Config auth0Config) {
    this.auth0Config = auth0Config;
  }

  public boolean isUsingDefaultEncryptionKeyStore() {
    return usingDefaultEncryptionKeyStore;
  }

  public void setUsingDefaultEncryptionKeyStore(boolean usingDefaultEncryptionKeyStore) {
    this.usingDefaultEncryptionKeyStore = usingDefaultEncryptionKeyStore;
  }

  public String getGlobalTenantEncryptionKeyName() {
    return globalTenantEncryptionKeyName;
  }

  public void setGlobalTenantEncryptionKeyName(String globalTenantEncryptionKeyName) {
    this.globalTenantEncryptionKeyName = globalTenantEncryptionKeyName;
  }

  public StatsdMetricsConfig getStatsdMetricsConfig() {
    return statsdMetricsConfig;
  }

  public void setStatsdMetricsConfig(final StatsdMetricsConfig metricsConfig) {
    this.statsdMetricsConfig = metricsConfig;
  }

  public Path getJemallocProfileDir() {
    return jemallocProfileDir;
  }

  public void setJemallocProfileDir(final Path jemallocProfileDir) {
    this.jemallocProfileDir = jemallocProfileDir;
  }

  @Valid
  @NotNull
  private MultiTenantHealthFactory multiTenantHealthFactory;

  @JsonProperty("mtiq-health")
  public MultiTenantHealthFactory getMultiTenantHealthFactory() {
    return multiTenantHealthFactory;
  }

  @JsonProperty("mtiq-health")
  public void setMultiTenantHealthFactory(final MultiTenantHealthFactory multiTenantHealthFactory) {
    this.multiTenantHealthFactory = multiTenantHealthFactory;
  }

  @Override
  public Optional<HealthFactory> getHealthFactory() {
    // no-op - superseded by MultiTenantHealthFactory.
    // Note this disables the default 'old method' DropWizard health checks. See EnvironmentCommand#configure().
    return Optional.empty();
  }

  @Override
  public void setHealthFactory(final HealthFactory health) {
    // no-op - superseded by MultiTenantHealthFactory
  }
}
