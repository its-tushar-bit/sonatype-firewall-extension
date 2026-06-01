/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonatype.insight.brain.metrics.datadog.StatsdMetricsConfig;
import com.sonatype.insight.brain.service.config.MultiTenantStorageConfig;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.db.DatabaseConfig;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import java.io.File;
import java.nio.file.Path;

public class MultiTenantInsightConfig
    extends InsightConfig
{
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

  @JsonProperty("mtiq-health")
  private Object mtiqHealth;

  public Object getMtiqHealth() {
    return mtiqHealth;
  }

  public void setMtiqHealth(Object mtiqHealth) {
    this.mtiqHealth = mtiqHealth;
  }

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
  public DatabaseConfig getDatabase() {
    return getMainDatabase();
  }

  @Override
  public void setDatabase(final DatabaseConfig database) {
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
}
