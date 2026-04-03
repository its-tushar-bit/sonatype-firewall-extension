/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.db;

public class DatabaseConfig
{
  private String driverClassName;

  private String connectionFactoryClassName;

  private String url;

  private String username;

  private String password;

  private String sessionVariables;

  private String options;

  private Integer maxConnections;

  private Integer maxIdleConnections;

  private Boolean readOnly;

  private boolean autoCommitOnReturnToPool = true;

  private boolean accessToUnderlyingConnectionAllowed;

  private int maxRetryAttempts = 1;

  private int maxRetryDurationSeconds = 30;

  private int maxConnectionLifetimeSeconds;

  private int connectionValidationTimeoutSeconds;

  private int maxWaitSeconds = -1;

  private String applicationName;

  public String getDriverClassName() {
    return driverClassName;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public String getConnectionFactoryClassName() {
    return connectionFactoryClassName;
  }

  public void setConnectionFactoryClassName(String connectionFactoryClassName) {
    this.connectionFactoryClassName = connectionFactoryClassName;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Integer getMaxConnections() {
    return maxConnections;
  }

  public void setMaxConnections(Integer maxConnections) {
    this.maxConnections = maxConnections;
  }

  public Integer getMaxIdleConnections() {
    return maxIdleConnections;
  }

  public void setMaxIdleConnections(Integer maxIdleConnections) {
    this.maxIdleConnections = maxIdleConnections;
  }

  public void setReadOnly(Boolean readOnly) {
    this.readOnly = readOnly;
  }

  public Boolean isReadOnly() {
    return readOnly;
  }

  public boolean isAutoCommitOnReturnToPool() {
    return autoCommitOnReturnToPool;
  }

  public void setAutoCommitOnReturnToPool(boolean autoCommitOnReturnToPool) {
    this.autoCommitOnReturnToPool = autoCommitOnReturnToPool;
  }

  public boolean isAccessToUnderlyingConnectionAllowed() {
    return accessToUnderlyingConnectionAllowed;
  }

  public void setAccessToUnderlyingConnectionAllowed(boolean allow) {
    accessToUnderlyingConnectionAllowed = allow;
  }

  public int getMaxRetryAttempts() {
    return maxRetryAttempts;
  }

  public void setMaxRetryAttempts(int maxRetryAttempts) {
    this.maxRetryAttempts = maxRetryAttempts;
  }

  public int getMaxRetryDurationSeconds() {
    return maxRetryDurationSeconds;
  }

  public void setMaxRetryDurationSeconds(int maxRetryDurationSeconds) {
    this.maxRetryDurationSeconds = maxRetryDurationSeconds;
  }

  public int getMaxConnectionLifetimeSeconds() {
    return maxConnectionLifetimeSeconds;
  }

  public void setMaxConnectionLifetimeSeconds(int maxConnectionLifetimeSeconds) {
    this.maxConnectionLifetimeSeconds = maxConnectionLifetimeSeconds;
  }

  public String getSessionVariables() {
    return sessionVariables;
  }

  public void setSessionVariables(final String sessionVariables) {
    this.sessionVariables = sessionVariables;
  }

  public String getOptions() {
    return options;
  }

  public void setOptions(final String options) {
    this.options = options;
  }

  public int getConnectionValidationTimeoutSeconds() {
    return connectionValidationTimeoutSeconds;
  }

  public void setConnectionValidationTimeoutSeconds(int connectionValidationTimeoutSeconds) {
    this.connectionValidationTimeoutSeconds = connectionValidationTimeoutSeconds;
  }

  public int getMaxWaitSeconds() {
    return maxWaitSeconds;
  }

  public void setMaxWaitSeconds(final int maxWaitSeconds) {
    this.maxWaitSeconds = maxWaitSeconds;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }
}
