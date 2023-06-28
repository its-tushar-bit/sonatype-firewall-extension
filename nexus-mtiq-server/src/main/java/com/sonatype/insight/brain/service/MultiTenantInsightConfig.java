/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URI;
import javax.validation.Valid;

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

  @Valid
  @JsonProperty(value = "auth0")
  private Auth0Config auth0Config;

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
}
