/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.net.URI;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.service.ReverseProxyAuthenticationConfigurationListener;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.138
 */
@Named
public class ReverseProxyAuthenticationConfigurationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ReverseProxyAuthenticationConfigurationMigrator.class);

  // Visible for testing
  static final String OBSOLETE_CONFIG_MESSAGE = "Reverse proxy authentication is now configured using the REST API. "
      + "The configuration in the config.yml or via system properties is obsolete.";

  // Visible for testing
  static final String MIGRATION_ID = "reverse-proxy-authentication-config";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO;

  private final InsightConfig insightConfig;

  private final Set<ReverseProxyAuthenticationConfigurationListener> reverseProxyAuthenticationConfigurationListeners;

  @Inject
  public ReverseProxyAuthenticationConfigurationMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO,
      InsightConfig insightConfig,
      Set<ReverseProxyAuthenticationConfigurationListener> reverseProxyAuthenticationConfigurationListeners)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.reverseProxyAuthenticationConfigurationDAO = reverseProxyAuthenticationConfigurationDAO;
    this.insightConfig = insightConfig;
    this.reverseProxyAuthenticationConfigurationListeners = reverseProxyAuthenticationConfigurationListeners;
  }

  void migrate() {
    ReverseProxyAuthenticationConfig config = insightConfig.getReverseProxyAuthentication();
    if (config != null) {
      log.warn(OBSOLETE_CONFIG_MESSAGE);
    }

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Reverse proxy authentication configuration already migrated.");
      return;
    }

    if (config == null) {
      // retain default configuration that was applied in old versions
      config = new ReverseProxyAuthenticationConfig();
    }

    log.debug("Migrating Reverse proxy authentication configuration to the database...");
    try (TransactionContext tx = reverseProxyAuthenticationConfigurationDAO.createTransactionContext()) {
      tx.begin();
      ReverseProxyAuthenticationConfiguration dbConfig = new ReverseProxyAuthenticationConfiguration();
      dbConfig.setEnabled(config.isEnabled());
      dbConfig.setUsernameHeader(config.getUsernameHeader());
      dbConfig.setCsrfProtectionDisabled(config.isCsrfProtectionDisabled());
      dbConfig.setLogoutUrl(config.getLogoutUrl() == null ? null : config.getLogoutUrl().toString());
      try {
        reverseProxyAuthenticationConfigurationDAO.insert(tx, dbConfig);
      }
      catch (BadRequestException e) {
        log.warn("The current reverse proxy authentication configuration is invalid and cannot be migrated.", e);
      }
      migrationTrackerDAO.insertTracker(tx, MIGRATION_ID);
      tx.commit();
    }

    log.info("Migrated reverse proxy authentication configuration to the database.");
    reverseProxyAuthenticationConfigurationListeners.forEach(
        ReverseProxyAuthenticationConfigurationListener::reverseProxyAuthenticationConfigurationChanged);
  }

  /**
   * This class should be used only for migrating the reverse proxy authentication configuration from config.yml to the
   * db. We need to keep this class because customers could specify only some values for the reverse proxy
   * authentication configuration (in config.yml or system properties) and rely on the default username header value.
   */
  public static class ReverseProxyAuthenticationConfig
  {
    private boolean enabled;

    private String usernameHeader = "REMOTE_USER";

    private boolean csrfProtectionDisabled;

    private URI logoutUrl;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getUsernameHeader() {
      return usernameHeader;
    }

    public void setUsernameHeader(String usernameHeader) {
      this.usernameHeader = usernameHeader;
    }

    public boolean isCsrfProtectionDisabled() {
      return csrfProtectionDisabled;
    }

    public void setCsrfProtectionDisabled(boolean csrfProtectionDisabled) {
      this.csrfProtectionDisabled = csrfProtectionDisabled;
    }

    public URI getLogoutUrl() {
      return logoutUrl;
    }

    public void setLogoutUrl(URI logoutUrl) {
      this.logoutUrl = logoutUrl;
    }
  }
}
