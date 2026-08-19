/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.db;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.postgresql.Driver;
import org.postgresql.PGProperty;

import static java.util.stream.Collectors.joining;

public class DatabaseConfig
{
  private String driverClassName;

  private String connectionFactoryClassName;

  @Pattern(regexp = "postgresql")
  private String type;

  private String hostname;

  @Min(1)
  @Max(65535)
  private Integer port;

  private String name;

  private Map<String, String> parameters;

  private String url;

  private String username;

  private String password;

  private String sessionVariables;

  private String options;

  @Min(1)
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

  @JsonIgnore
  private volatile boolean fieldsNeedSync = true;

  public DatabaseConfig(DatabaseConfig other) {
    this.driverClassName = other.driverClassName;
    this.connectionFactoryClassName = other.connectionFactoryClassName;
    this.type = other.type;
    this.hostname = other.hostname;
    this.port = other.port;
    this.name = other.name;
    this.parameters = other.parameters != null ? new LinkedHashMap<>(other.parameters) : null;
    this.url = other.url;
    this.username = other.username;
    this.password = other.password;
    this.sessionVariables = other.sessionVariables;
    this.options = other.options;
    this.maxConnections = other.maxConnections;
    this.maxIdleConnections = other.maxIdleConnections;
    this.readOnly = other.readOnly;
    this.autoCommitOnReturnToPool = other.autoCommitOnReturnToPool;
    this.accessToUnderlyingConnectionAllowed = other.accessToUnderlyingConnectionAllowed;
    this.maxRetryAttempts = other.maxRetryAttempts;
    this.maxRetryDurationSeconds = other.maxRetryDurationSeconds;
    this.maxConnectionLifetimeSeconds = other.maxConnectionLifetimeSeconds;
    this.connectionValidationTimeoutSeconds = other.connectionValidationTimeoutSeconds;
    this.maxWaitSeconds = other.maxWaitSeconds;
    this.applicationName = other.applicationName;
    this.fieldsNeedSync = other.fieldsNeedSync;
  }

  public DatabaseConfig() {
  }

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getHostname() {
    if (fieldsNeedSync) {
      syncFromUrl();
    }
    return hostname;
  }

  public synchronized void setHostname(String hostname) {
    syncFromUrl();
    this.hostname = hostname;
    this.url = resolveUrlFromFields();
  }

  public Integer getPort() {
    if (fieldsNeedSync) {
      syncFromUrl();
    }
    return port;
  }

  public synchronized void setPort(Integer port) {
    syncFromUrl();
    this.port = port;
    this.url = resolveUrlFromFields();
  }

  public String getName() {
    if (fieldsNeedSync) {
      syncFromUrl();
    }
    return name;
  }

  public synchronized void setName(String name) {
    syncFromUrl();
    this.name = name;
    this.url = resolveUrlFromFields();
  }

  public Map<String, String> getParameters() {
    if (fieldsNeedSync) {
      syncFromUrl();
    }
    return parameters;
  }

  public synchronized void setParameters(Map<String, String> parameters) {
    syncFromUrl();
    this.parameters = parameters;
    this.url = resolveUrlFromFields();
  }

  @JsonIgnore
  @AssertTrue(
      message = "database must specify either 'url' or both 'hostname' and 'name', plus 'username' and 'password'")
  public boolean isValidConnectionConfig() {
    boolean hasConnectionTarget = (getUrl() != null) || (getHostname() != null && getName() != null);
    return hasConnectionTarget && username != null && password != null;
  }

  public String getUrl() {
    return url;
  }

  public synchronized void setUrl(String url) {
    this.url = url;
    this.fieldsNeedSync = true;
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

  private String resolveUrlFromFields() {
    if (hostname == null || name == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder("jdbc:postgresql://").append(hostname);
    if (port != null) {
      sb.append(':').append(port);
    }
    sb.append('/').append(name);
    if (parameters != null && !parameters.isEmpty()) {
      String paramString = parameters.entrySet()
          .stream()
          .filter(entry -> !"user".equals(entry.getKey()) && !"password".equals(entry.getKey()))
          .map(entry -> entry.getKey() + '=' + entry.getValue())
          .collect(joining("&"));
      if (!paramString.isEmpty()) {
        sb.append('?').append(paramString);
      }
    }
    return sb.toString();
  }

  private synchronized void syncFromUrl() {
    if (!fieldsNeedSync) {
      return;
    }
    if (url == null) {
      hostname = null;
      port = null;
      name = null;
      parameters = null;
      fieldsNeedSync = false;
      return;
    }
    Properties parsed = Driver.parseURL(url, null);
    if (parsed == null) {
      hostname = null;
      port = null;
      name = null;
      parameters = null;
      fieldsNeedSync = false;
      return;
    }
    hostname = PGProperty.PG_HOST.getOrDefault(parsed);
    String portStr = PGProperty.PG_PORT.getOrDefault(parsed);
    port = portStr != null ? Integer.valueOf(portStr) : null;
    name = PGProperty.PG_DBNAME.getOrDefault(parsed);
    Map<String, String> queryParams = new LinkedHashMap<>();
    for (String key : parsed.stringPropertyNames()) {
      if (!"PGHOST".equals(key) && !"PGPORT".equals(key) && !"PGDBNAME".equals(key)
          && !"user".equals(key) && !"password".equals(key))
      {
        queryParams.put(key, parsed.getProperty(key));
      }
    }
    parameters = queryParams.isEmpty() ? null : queryParams;
    fieldsNeedSync = false;
  }
}
