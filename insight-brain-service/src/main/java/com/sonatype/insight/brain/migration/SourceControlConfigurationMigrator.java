/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.service.ApiSourceControlConfigurationService;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.140
 */
@Named
public class SourceControlConfigurationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlConfigurationMigrator.class);

  // Visible for testing
  static final String OBSOLETE_CONFIG_MESSAGE =
      "Source control and branch monitoring is now configured using the REST API. "
          + "The configuration in the config.yml or via system properties is obsolete.";

  // Visible for testing
  static final String MIGRATION_ID = "source-control-config";

  private final InsightConfig insightConfig;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ApiSourceControlConfigurationService sourceControlConfigurationService;

  @Inject
  public SourceControlConfigurationMigrator(
      InsightConfig insightConfig,
      MigrationTrackerDAO migrationTrackerDAO,
      ApiSourceControlConfigurationService sourceControlConfigurationService)
  {
    this.insightConfig = insightConfig;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.sourceControlConfigurationService = sourceControlConfigurationService;
  }

  void migrate() {
    SourceControlConfigurationMigrator.SourceControlConfig sourceControlConfig = insightConfig.getSourceControl();
    SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig defaultBranchMonitoring =
        insightConfig.getDefaultBranchMonitoring();
    Integer pullRequestMonitoringIntervalSeconds = insightConfig.getPullRequestMonitoringIntervalInSeconds();

    if (sourceControlConfig != null || defaultBranchMonitoring != null ||
        pullRequestMonitoringIntervalSeconds != null)
    {
      log.warn(OBSOLETE_CONFIG_MESSAGE);
    }

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Source control and branch monitoring configuration already migrated.");
      return;
    }

    log.debug("Migrating source control and branch monitoring configuration to the database...");
    try (TransactionContext tx = migrationTrackerDAO.createTransactionContext()) {
      tx.begin();
      if (sourceControlConfig != null || defaultBranchMonitoring != null ||
          pullRequestMonitoringIntervalSeconds != null)
      {
        try {
          ObjectNode objectNode = new ObjectMapper().createObjectNode();
          if (sourceControlConfig != null) {
            sourceControlConfig.setSonatypeWorkDir(insightConfig.getSonatypeWork());
            objectNode.setAll((ObjectNode) JsonUtils.asTree(sourceControlConfig));
          }
          if (defaultBranchMonitoring != null) {
            objectNode.put("defaultBranchMonitoringStartTime", defaultBranchMonitoring.startTime);
            objectNode.put("defaultBranchMonitoringIntervalHours", defaultBranchMonitoring.intervalInHours);
          }
          if (pullRequestMonitoringIntervalSeconds != null) {
            int minimumInterval = SourceControlConfiguration.DEFAULT_PULL_REQUEST_MONITORING_INTERVAL_SECONDS;
            int valueToStore = pullRequestMonitoringIntervalSeconds < minimumInterval
                ? minimumInterval
                : pullRequestMonitoringIntervalSeconds;
            objectNode.put("pullRequestMonitoringIntervalSeconds", valueToStore);
          }
          sourceControlConfigurationService.setConfigurationInDatabaseNoAuthz(tx, objectNode);
        }
        catch (BadRequestException e) {
          log.warn("The current source control or branch monitoring configuration is invalid and cannot be migrated.",
              e);
        }
      }
      migrationTrackerDAO.insertTracker(tx, MIGRATION_ID);
      tx.commit();
    }

    log.info("Migrated source control and branch monitoring configuration to the database.");
    sourceControlConfigurationService.applySourceControlConfigurationToClients();
  }

  /**
   * This class should be used only for migrating the source control configuration from config.yml to the db. We need to
   * keep this class because future classes representing the source control configuration may change and not be
   * backwards compatible with the source control configuration in config.yml.
   */
  public static class SourceControlConfig
  {
    private String cloneDirectory;

    private File sonatypeWorkDir;

    private String gitImplementation;

    /**
     * Purge window for PR comment records in days
     */
    private Integer prCommentPurgeWindow;

    /**
     * Purge window for PR event records in days
     */
    private Integer prEventPurgeWindow;

    /**
     * @since 1.83
     */
    private String gitExecutable;

    /**
     * @since 1.137 Time in seconds until when a git command can execute before timing out
     */
    private int gitTimeoutSeconds;

    /**
     * Hidden config to customize the commit username for SCM features
     *
     * @since 1.121
     */
    private String commitUsername;

    /**
     * Hidden config to customize the commit email address for SCM features
     *
     * @since 1.121
     */
    private String commitEmail;

    /**
     * Hidden config to add the username to the repository clone URL. Used in conjunction with `commitEmail` to support
     * Bitbucket Server 'Verified Committer' feature. See INT-4453.
     *
     * @since 1.121
     */
    private boolean useUsernameInRepositoryCloneUrl;

    /**
     * Return the {@link #cloneDirectory} as a {@link File}. If not set will default to {@link
     * SourceControlConfiguration#DEFAULT_SOURCE_CONTROL_CLONE_DIR}. If {@link #cloneDirectory} is not a fully qualified
     * path then it will be created under the {@link #sonatypeWorkDir} which needs to be set with {@link
     * #setCloneDirectory(String)}. Note that this will happen automatically when called via {@link
     * InsightConfig#getSourceControl()}.
     */
    public File getCloneDirectory() {
      if (StringUtils.isBlank(cloneDirectory)) {
        cloneDirectory = SourceControlConfiguration.DEFAULT_SOURCE_CONTROL_CLONE_DIR;
      }

      File file = new File(cloneDirectory);
      if (!file.isAbsolute()) {
        file = new File(sonatypeWorkDir, cloneDirectory);
      }

      return file;
    }

    public void setCloneDirectory(String cloneDirectory) {
      this.cloneDirectory = cloneDirectory;
    }

    public void setSonatypeWorkDir(File sonatypeWorkDir) {
      this.sonatypeWorkDir = sonatypeWorkDir;
    }

    public String getGitImplementation() {
      return gitImplementation;
    }

    public void setGitImplementation(String gitImplementation) {
      this.gitImplementation = gitImplementation;
    }

    public String getGitExecutable() {
      return gitExecutable;
    }

    public void setGitExecutable(String gitExecutable) {
      this.gitExecutable = gitExecutable;
    }

    public int getGitTimeoutSeconds() {
      return gitTimeoutSeconds;
    }

    public void setGitTimeoutSeconds(int gitTimeoutSeconds) {
      this.gitTimeoutSeconds = gitTimeoutSeconds;
    }

    public String getCommitUsername() {
      return commitUsername;
    }

    public void setCommitUsername(String commitUsername) {
      this.commitUsername = commitUsername;
    }

    public String getCommitEmail() {
      return commitEmail;
    }

    public void setCommitEmail(String commitEmail) {
      this.commitEmail = commitEmail;
    }

    public boolean getUseUsernameInRepositoryCloneUrl() {
      return useUsernameInRepositoryCloneUrl;
    }

    public void setUseUsernameInRepositoryCloneUrl(boolean useUsernameInRepositoryCloneUrl) {
      this.useUsernameInRepositoryCloneUrl = useUsernameInRepositoryCloneUrl;
    }

    public Integer getPrCommentPurgeWindow() {
      return prCommentPurgeWindow;
    }

    public void setPrCommentPurgeWindow(Integer prCommentPurgeWindow) {
      this.prCommentPurgeWindow = prCommentPurgeWindow;
    }

    public Integer getPrEventPurgeWindow() {
      return prEventPurgeWindow;
    }

    public void setPrEventPurgeWindow(Integer prEventPurgeWindow) {
      this.prEventPurgeWindow = prEventPurgeWindow;
    }
  }

  /**
   * This class should be used only for migrating the default branch monitoring config from config.yml to the db. We
   * need to keep this class because customers could specify only some values (in config.yml or system properties) and
   * rely on default values for others. Also future classes representing the source control configuration may change and
   * not be backwards compatible with the default branch monitoring config in config.yml.
   */
  public static class DefaultBranchMonitoringConfig
  {
    private String startTime = "00:00";

    private Integer intervalInHours = 24;

    public String getStartTime() {
      return startTime;
    }

    public void setStartTime(String startTime) {
      this.startTime = startTime;
    }

    public Integer getIntervalInHours() {
      return intervalInHours;
    }

    public void setIntervalInHours(Integer intervalInHours) {
      this.intervalInHours = intervalInHours;
    }
  }
}
