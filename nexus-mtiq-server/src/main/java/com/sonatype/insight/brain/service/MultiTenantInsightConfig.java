/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URI;
import javax.annotation.Nullable;
import javax.validation.Valid;

import com.sonatype.insight.brain.metrics.datadog.StatsdMetricsConfig;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.db.DatabaseConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

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
  private String auth0Domain;

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

  @Nullable
  @JsonProperty
  private String cloudyClusterConfigFilePath;

  @Nullable
  public String getCloudyClusterConfigFilePath() {
    return cloudyClusterConfigFilePath;
  }

  public void setCloudyClusterConfigFilePath(@Nullable final String cloudyClusterConfigFilePath) {
    this.cloudyClusterConfigFilePath = cloudyClusterConfigFilePath;
  }

  @Override
  public File getSonatypeWork() {
    return new File(sonatypeWork, TenantThreadLocal.getTenant().tenantSlug);
  }

  @Override
  public File getClusterDirectory() {
    return new File(super.getClusterDirectory(), TenantThreadLocal.getTenant().tenantSlug);
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

  public String getAuth0Domain() {
    return auth0Domain;
  }

  public void setAuth0Domain(final String auth0Domain) {
    this.auth0Domain = auth0Domain;
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
}
