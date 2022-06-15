/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ApiSourceControlConfigurationService
    implements Job
{
  private static final Logger log = LoggerFactory.getLogger(ApiSourceControlConfigurationService.class);

  // Visible for testing
  static final String BAD_CONFIG_ERROR_MSG =
      "The given JSON cannot be deserialized into a source control configuration.";

  // Visible for testing
  static final String BAD_DEFAULT_BRANCH_MONITORING_START_TIME =
      "The default branch monitoring start time cannot be parsed.";

  // Visible for testing
  static final String TASK_NAME = "SourceControlConfiguration";

  private final SourceControlConfigurationDAO sourceControlConfigurationDAO;

  private final List<SourceControlConfigurationListener> sourceControlConfigurationListeners;

  private final TaskScheduler taskScheduler;

  @Inject
  public ApiSourceControlConfigurationService(
      SourceControlConfigurationDAO sourceControlConfigurationDAO,
      List<SourceControlConfigurationListener> sourceControlConfigurationListeners,
      TaskScheduler taskScheduler)
  {
    this.sourceControlConfigurationDAO = sourceControlConfigurationDAO;
    this.sourceControlConfigurationListeners = sourceControlConfigurationListeners;
    this.taskScheduler = taskScheduler;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiSourceControlConfigurationDTO getConfiguration() {
    return convertToDTO(sourceControlConfigurationDAO.getNotNull());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setConfiguration(JsonNode jsonNode) {
    setConfigurationNoAuthz(jsonNode);
    updateAllClusterNodesFromConfiguration();
  }

  public void setConfigurationNoAuthz(JsonNode jsonNode) {
    try (TransactionContext tx = sourceControlConfigurationDAO.createTransactionContext()) {
      tx.begin();
      setConfigurationNoAuthz(tx, jsonNode);
      tx.commit();
    }
  }

  public void setConfigurationNoAuthz(TransactionContext tx, JsonNode jsonNode) {
    SourceControlConfiguration config = resolveSourceControlConfiguration(jsonNode);
    auditConfiguration(config);
    sourceControlConfigurationDAO.set(tx, config);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration() {
    SourceControlConfiguration config = sourceControlConfigurationDAO.getNotNull();
    auditConfiguration(config);
    sourceControlConfigurationDAO.delete();
    updateAllClusterNodesFromConfiguration();
  }

  private ApiSourceControlConfigurationDTO convertToDTO(SourceControlConfiguration config) {
    ApiSourceControlConfigurationDTO dto = new ApiSourceControlConfigurationDTO();
    dto.cloneDirectory = config.getCloneDirectory();
    dto.gitImplementation = config.getGitImplementation();
    dto.prCommentPurgeWindow = config.getPrCommentPurgeWindow();
    dto.prEventPurgeWindow = config.getPrEventPurgeWindow();
    dto.gitExecutable = config.getGitExecutable();
    dto.gitTimeoutSeconds = config.getGitTimeoutSeconds();
    dto.commitUsername = config.getCommitUsername();
    dto.commitEmail = config.getCommitEmail();
    dto.useUsernameInRepositoryCloneUrl = config.isUseUsernameInRepositoryCloneUrl();
    dto.defaultBranchMonitoringStartTime = config.getDefaultBranchMonitoringStartTimeString();
    dto.defaultBranchMonitoringIntervalHours = config.getDefaultBranchMonitoringIntervalHours();
    dto.pullRequestMonitoringIntervalSeconds = config.getPullRequestMonitoringIntervalSeconds();
    return dto;
  }

  private SourceControlConfiguration resolveSourceControlConfiguration(JsonNode jsonNode) {
    if (jsonNode == null) {
      throw new BadRequestException(SourceControlConfigurationDAO.NO_CONFIG_ERROR_MSG);
    }
    ApiSourceControlConfigurationDTO dto;
    try {
      dto = JsonUtils.asPojo(jsonNode, ApiSourceControlConfigurationDTO.class);
    }
    catch (IOException e) {
      throw new BadRequestException(BAD_CONFIG_ERROR_MSG);
    }
    SourceControlConfiguration result;
    SourceControlConfiguration sourceControlConfiguration = convertFromDTO(dto);
    SourceControlConfiguration existingSourceControlConfiguration = sourceControlConfigurationDAO.get();
    if (existingSourceControlConfiguration == null) {
      result = sourceControlConfiguration;
    }
    else {
      if (jsonNode.has("cloneDirectory")) {
        existingSourceControlConfiguration.setCloneDirectory(sourceControlConfiguration.getCloneDirectory());
      }
      if (jsonNode.has("gitImplementation")) {
        existingSourceControlConfiguration.setGitImplementation(sourceControlConfiguration.getGitImplementation());
      }
      if (jsonNode.has("prCommentPurgeWindow")) {
        existingSourceControlConfiguration.setPrCommentPurgeWindow(
            sourceControlConfiguration.getPrCommentPurgeWindow());
      }
      if (jsonNode.has("prEventPurgeWindow")) {
        existingSourceControlConfiguration.setPrEventPurgeWindow(sourceControlConfiguration.getPrEventPurgeWindow());
      }
      if (jsonNode.has("gitExecutable")) {
        existingSourceControlConfiguration.setGitExecutable(sourceControlConfiguration.getGitExecutable());
      }
      if (jsonNode.has("gitTimeoutSeconds")) {
        existingSourceControlConfiguration.setGitTimeoutSeconds(sourceControlConfiguration.getGitTimeoutSeconds());
      }
      if (jsonNode.has("commitUsername")) {
        existingSourceControlConfiguration.setCommitUsername(sourceControlConfiguration.getCommitUsername());
      }
      if (jsonNode.has("commitEmail")) {
        existingSourceControlConfiguration.setCommitEmail(sourceControlConfiguration.getCommitEmail());
      }
      if (jsonNode.has("useUsernameInRepositoryCloneUrl")) {
        existingSourceControlConfiguration.setUseUsernameInRepositoryCloneUrl(
            sourceControlConfiguration.isUseUsernameInRepositoryCloneUrl());
      }
      if (jsonNode.has("defaultBranchMonitoringStartTime")) {
        existingSourceControlConfiguration.setDefaultBranchMonitoringStartTime(
            sourceControlConfiguration.getDefaultBranchMonitoringStartTime());
      }
      if (jsonNode.has("defaultBranchMonitoringIntervalHours")) {
        existingSourceControlConfiguration.setDefaultBranchMonitoringIntervalHours(
            sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours());
      }
      if (jsonNode.has("pullRequestMonitoringIntervalSeconds")) {
        existingSourceControlConfiguration.setPullRequestMonitoringIntervalSeconds(
            sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds());
      }
      result = existingSourceControlConfiguration;
    }
    return result;
  }

  private SourceControlConfiguration convertFromDTO(ApiSourceControlConfigurationDTO dto) {
    SourceControlConfiguration config = new SourceControlConfiguration();
    config.setCloneDirectory(dto.cloneDirectory);
    config.setGitImplementation(dto.gitImplementation);
    config.setPrCommentPurgeWindow(dto.prCommentPurgeWindow);
    config.setPrEventPurgeWindow(dto.prEventPurgeWindow);
    config.setGitExecutable(dto.gitExecutable);
    config.setGitTimeoutSeconds(dto.gitTimeoutSeconds);
    config.setCommitUsername(dto.commitUsername);
    config.setCommitEmail(dto.commitEmail);
    config.setUseUsernameInRepositoryCloneUrl(dto.useUsernameInRepositoryCloneUrl);
    try {
      config.setDefaultBranchMonitoringStartTimeString(dto.defaultBranchMonitoringStartTime);
    }
    catch (DateTimeParseException e) {
      throw new BadRequestException(BAD_DEFAULT_BRANCH_MONITORING_START_TIME, e);
    }
    config.setDefaultBranchMonitoringIntervalHours(dto.defaultBranchMonitoringIntervalHours);
    config.setPullRequestMonitoringIntervalSeconds(dto.pullRequestMonitoringIntervalSeconds);
    return config;
  }

  private void auditConfiguration(SourceControlConfiguration configuration) {
    AuditData.get()
        .setData("cloneDirectory", configuration.getCloneDirectory())
        .setData("gitImplementation", configuration.getGitImplementation())
        .setData("prCommentPurgeWindow", configuration.getPrCommentPurgeWindow())
        .setData("prEventPurgeWindow", configuration.getPrEventPurgeWindow())
        .setData("gitExecutable", configuration.getGitExecutable())
        .setData("gitTimeoutSeconds", configuration.getGitTimeoutSeconds())
        .setData("commitUsername", configuration.getCommitUsername())
        .setData("commitEmail", configuration.getCommitEmail())
        .setData("useUsernameInRepositoryCloneUrl", configuration.isUseUsernameInRepositoryCloneUrl())
        .setData("defaultBranchMonitoringStartTime", configuration.getDefaultBranchMonitoringStartTimeString())
        .setData("defaultBranchMonitoringIntervalHours", configuration.getDefaultBranchMonitoringIntervalHours())
        .setData("pullRequestMonitoringIntervalSeconds", configuration.getPullRequestMonitoringIntervalSeconds());
  }

  public void applySourceControlConfigurationToClients() {
    sourceControlConfigurationListeners.forEach(SourceControlConfigurationListener::sourceControlConfigurationChanged);
  }

  // Visible for testing
  void updateAllClusterNodesFromConfiguration() {
    applySourceControlConfigurationToClients();
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(getClass(), TASK_NAME);
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      applySourceControlConfigurationToClients();
    }
    catch (Exception e) {
      log.error("Error when applying source control config: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }
}
