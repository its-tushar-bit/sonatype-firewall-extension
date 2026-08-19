/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.139
 */
@Named
public class JiraConfigurationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(JiraConfigurationMigrator.class);

  // Visible for testing
  static final String OBSOLETE_CONFIG_MESSAGE = "JIRA is now configured using the REST API. "
      + "The configuration in the config.yml or via system properties is obsolete.";

  // Visible for testing
  static final String MIGRATION_ID = "jira-config";

  private final InsightConfig insightConfig;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ApiJiraConfigurationService jiraConfigurationService;

  @Inject
  public JiraConfigurationMigrator(
      InsightConfig insightConfig,
      MigrationTrackerDAO migrationTrackerDAO,
      ApiJiraConfigurationService jiraConfigurationService)
  {
    this.insightConfig = insightConfig;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.jiraConfigurationService = jiraConfigurationService;
  }

  void migrate() {
    JiraConfigurationMigrator.JiraConfig config = insightConfig.getJiraConfig();
    if (config != null) {
      log.warn(OBSOLETE_CONFIG_MESSAGE);
    }

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("JIRA configuration already migrated.");
      return;
    }

    log.debug("Migrating JIRA configuration to the database...");
    try (TransactionContext tx = migrationTrackerDAO.createTransactionContext()) {
      tx.begin();
      if (config != null) {
        try {
          jiraConfigurationService.setConfigurationInDatabaseNoAuthz(tx, JsonUtils.asTree(config));
        }
        catch (BadRequestException e) {
          log.warn("The current JIRA configuration is invalid and cannot be migrated.", e);
        }
      }
      migrationTrackerDAO.insertTracker(tx, MIGRATION_ID);
      tx.commit();
    }

    log.info("Migrated JIRA configuration to the database.");
    jiraConfigurationService.applyJiraConfigurationToClients();
  }

  /**
   * This class should be used only for migrating the JIRA configuration from config.yml to the db.
   * We need to keep this class because future classes representing the JIRA configuration may change and not be
   * backwards compatible with the JIRA configuration in config.yml.
   */
  public static class JiraConfig
  {
    private String url;

    private String username;

    private char[] password;

    private Map<String, Object> customFields;

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

    public char[] getPassword() {
      return password;
    }

    public void setPassword(char[] password) {
      this.password = password;
    }

    public Map<String, Object> getCustomFields() {
      return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
      this.customFields = customFields;
    }
  }
}
