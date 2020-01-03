/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.configuration.ProxyConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since MIGRATE_PROXY_CONFIG
 */
@Named
public class ProxyConfigurationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ProxyConfigurationMigrator.class);

  static final String PROXY_EXCLUDE_HOSTS_PROP_NAME = "PROXY_EXCLUDE_HOSTS";

  static final String OBSOLETE_CONFIG_MESSAGE = "The proxy is now configured using the UI or the REST API. "
      + "The configuration in the config.yml or via system properties is obsolete.";

  static final String INVALID_CONFIG_MESSAGE = "The current proxy configuration is invalid and cannot be migrated.";

  static final String MIGRATION_ID = "proxy-configuration";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ProxyConfigurationDAO proxyConfigurationDAO;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final InsightConfig insightConfig;

  private final PasswordHandler passwordHandler;

  @Inject
  public ProxyConfigurationMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      ProxyConfigurationDAO proxyConfigurationDAO,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      InsightConfig insightConfig,
      PasswordHandler passwordHandler)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.proxyConfigurationDAO = proxyConfigurationDAO;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.insightConfig = insightConfig;
    this.passwordHandler = passwordHandler;
  }

  public void migrate() {
    ProxyConfig fileConfig = insightConfig.getProxyConfig();
    if (fileConfig != null) {
      log.warn(OBSOLETE_CONFIG_MESSAGE);
    }

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Proxy configuration already migrated.");
      return;
    }

    log.debug("Migrating proxy configuration to database...");

    try (TransactionContext tx = proxyConfigurationDAO.createTransactionContext()) {
      tx.begin();

      SystemConfigurationProperty excludeHostsConfig =
          systemConfigurationPropertyDAO.getByName(tx, PROXY_EXCLUDE_HOSTS_PROP_NAME);

      if (fileConfig != null) {
        ProxyConfiguration dbConfig = new ProxyConfiguration();
        dbConfig.setHostname(fileConfig.getHostname());
        dbConfig.setPort(fileConfig.getPort());
        dbConfig.setUsername(fileConfig.getUsername());
        if (fileConfig.getPassword() != null) {
          dbConfig.setPassword(passwordHandler.encryptPassword(fileConfig.getPassword()));
          fileConfig.clearPassword();
        }
        if (excludeHostsConfig != null) {
          String excludeHosts = excludeHostsConfig.getValue().trim();
          if (!excludeHosts.isEmpty()) {
            dbConfig.setExcludeHosts(excludeHosts);
          }
        }

        try {
          proxyConfigurationDAO.insert(tx, dbConfig);
        }
        catch (BadRequestException e) {
          log.warn(INVALID_CONFIG_MESSAGE, e);
        }
      }
      else {
        log.info("There is no proxy configuration to migrate database.");
      }

      if (excludeHostsConfig != null) {
        systemConfigurationPropertyDAO.delete(tx, excludeHostsConfig);
      }

      migrationTrackerDAO.insert(tx, new MigrationTracker(MIGRATION_ID));

      tx.commit();
    }

    log.info("Migrated proxy configuration to database.");
  }

  /**
   * This class should be used only for the migration of the proxy configuration from config.yml to the db.
   * We need to keep this class because customers could specify only some values for the proxy configuration (in
   * config.yml or system properties) and rely on the default value for the port.
   */
  public static class ProxyConfig
  {
    private String hostname;

    private int port = 80;

    private String username;

    private char[] password;

    public String getHostname() {
      return hostname;
    }

    public int getPort() {
      return port;
    }

    public String getUsername() {
      return username;
    }

    public char[] getPassword() {
      return password;
    }

    public void setHostname(String hostname) {
      this.hostname = hostname;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public void setPassword(char[] password) {
      this.password = password;
    }

    public void clearPassword() {
      if (password != null) {
        Arrays.fill(password, '0');
      }
    }
  }
}
