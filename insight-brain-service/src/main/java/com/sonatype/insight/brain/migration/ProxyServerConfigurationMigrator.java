/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.Arrays;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.service.ProxyServerConfigurationListener;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.84
 */
@Named
public class ProxyServerConfigurationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ProxyServerConfigurationMigrator.class);

  static final String PROXY_EXCLUDE_HOSTS_PROP_NAME = "PROXY_EXCLUDE_HOSTS";

  static final String OBSOLETE_CONFIG_MESSAGE = "The proxy is now configured using the UI or the REST API. "
      + "The configuration in the config.yml or via system properties is obsolete.";

  static final String INVALID_CONFIG_MESSAGE =
      "The current proxy server configuration is invalid and cannot be migrated.";

  static final String MIGRATION_ID = "proxy-server-configuration";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final InsightConfig insightConfig;

  private final PasswordHandler passwordHandler;

  private final Set<ProxyServerConfigurationListener> proxyServerConfigurationListeners;

  @Inject
  public ProxyServerConfigurationMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      ProxyServerConfigurationDAO proxyServerConfigurationDAO,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      InsightConfig insightConfig,
      PasswordHandler passwordHandler,
      Set<ProxyServerConfigurationListener> proxyServerConfigurationListeners)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.proxyServerConfigurationDAO = proxyServerConfigurationDAO;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.insightConfig = insightConfig;
    this.passwordHandler = passwordHandler;
    this.proxyServerConfigurationListeners = proxyServerConfigurationListeners;
  }

  public void migrate() {
    ProxyConfig fileConfig = insightConfig.getProxyConfig();
    if (fileConfig != null) {
      log.warn(OBSOLETE_CONFIG_MESSAGE);
    }

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Proxy server configuration already migrated.");
      return;
    }

    log.debug("Migrating proxy server configuration to database...");

    boolean migrated = false;
    try (TransactionContext tx = proxyServerConfigurationDAO.createTransactionContext()) {
      tx.begin();

      SystemConfigurationProperty excludeHostsConfig =
          systemConfigurationPropertyDAO.getByName(tx, PROXY_EXCLUDE_HOSTS_PROP_NAME);

      if (fileConfig != null) {
        ProxyServerConfiguration dbConfig = new ProxyServerConfiguration();
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
          proxyServerConfigurationDAO.insert(tx, dbConfig);
          migrated = true;
        }
        catch (BadRequestException e) {
          log.warn(INVALID_CONFIG_MESSAGE, e);
        }
      }
      else {
        log.info("There is no proxy server configuration to migrate to database.");
      }

      if (excludeHostsConfig != null) {
        systemConfigurationPropertyDAO.delete(tx, excludeHostsConfig);
      }

      migrationTrackerDAO.insert(tx, new MigrationTracker(MIGRATION_ID));

      tx.commit();
    }

    log.info("Migrated proxy server configuration to database.");

    if (migrated) {
      proxyServerConfigurationListeners.forEach(ProxyServerConfigurationListener::proxyServerConfigurationChanged);
    }
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
