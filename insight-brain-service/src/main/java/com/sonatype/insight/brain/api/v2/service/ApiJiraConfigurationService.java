/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ApiJiraConfigurationService
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ApiJiraConfigurationService.class);

  // Visible for testing
  public static final String NO_CONFIG_ERROR_MSG = "A JIRA configuration must be specified.";

  // Visible for testing
  static final String BAD_CONFIG_ERROR_MSG = "The given JSON cannot be deserialized into a JIRA configuration.";

  // Visible for testing
  static final String NO_PASSWORD_ERROR_MSG = "A password must be provided to change the JIRA server address.";

  // Visible for testing
  static final String NO_USERNAME_PASSWORD_PAIR = "A username and password must be provided together.";

  // Visible for testing
  static final String TASK_NAME = "JiraConfiguration";

  private static final String CONFIG_APPLY_ERROR = "Error when applying JIRA config";

  private final JiraConfigurationDAO jiraConfigurationDAO;

  private final PasswordHandler passwordHandler;

  private final Set<JiraConfigurationListener> jiraConfigurationListeners;

  private final TaskScheduler taskScheduler;

  @Inject
  public ApiJiraConfigurationService(
      JiraConfigurationDAO jiraConfigurationDAO,
      PasswordHandler passwordHandler,
      Set<JiraConfigurationListener> jiraConfigurationListeners,
      TaskScheduler taskScheduler)
  {
    this.jiraConfigurationDAO = jiraConfigurationDAO;
    this.passwordHandler = passwordHandler;
    this.jiraConfigurationListeners = jiraConfigurationListeners;
    this.taskScheduler = taskScheduler;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiJiraConfigurationDTO getConfiguration() {
    return convertToDTO(jiraConfigurationDAO.getNotNull());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setConfiguration(JsonNode jsonNode) {
    setConfigurationNoAuthz(jsonNode);
  }

  public void setConfigurationNoAuthz(JsonNode jsonNode) {
    setConfigurationInDatabaseNoAuthz(jsonNode);
    updateAllClusterNodesFromConfiguration();
  }

  public void setConfigurationInDatabaseNoAuthz(JsonNode jsonNode) {
    try (TransactionContext tx = jiraConfigurationDAO.createTransactionContext()) {
      tx.begin();
      setConfigurationInDatabaseNoAuthz(tx, jsonNode);
      tx.commit();
    }
  }

  public void setConfigurationInDatabaseNoAuthz(TransactionContext tx, JsonNode jsonNode) {
    JiraConfiguration config = createOrUpdateJiraConfiguration(jsonNode);
    auditConfiguration(config);
    jiraConfigurationDAO.set(tx, config);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration() {
    JiraConfiguration config = jiraConfigurationDAO.getNotNull();
    deleteConfigurationNoAuthz(config);
  }

  private void deleteConfigurationNoAuthz(JiraConfiguration config) {
    deleteConfigurationInDatabaseNoAuthz(config);
    updateAllClusterNodesFromConfiguration();
  }

  private void deleteConfigurationInDatabaseNoAuthz(JiraConfiguration config) {
    auditConfiguration(config);
    jiraConfigurationDAO.delete();
  }

  private ApiJiraConfigurationDTO convertToDTO(JiraConfiguration config) {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = config.getUrl();
    dto.username = config.getUsername();
    // Not setting password as it's sensitive
    dto.customFields = config.getCustomFields();
    return dto;
  }

  private JiraConfiguration createOrUpdateJiraConfiguration(JsonNode jsonNode) {
    if (jsonNode == null) {
      throw new BadRequestException(NO_CONFIG_ERROR_MSG);
    }
    ApiJiraConfigurationDTO dto;
    try {
      dto = JsonUtils.asPojo(jsonNode, ApiJiraConfigurationDTO.class);
    }
    catch (IOException e) {
      throw new BadRequestException(BAD_CONFIG_ERROR_MSG);
    }
    JiraConfiguration result;
    JiraConfiguration jiraConfiguration = convertFromDTO(dto);
    JiraConfiguration existingJiraConfiguration = jiraConfigurationDAO.get();
    if (existingJiraConfiguration == null) {
      result = jiraConfiguration;
    }
    else {
      if (jsonNode.has("url")) {
        if (!jsonNode.has("password") &&
            existingJiraConfiguration.getPassword() != null &&
            !existingJiraConfiguration.getUrl().equals(jiraConfiguration.getUrl())) {
          throw new BadRequestException(NO_PASSWORD_ERROR_MSG);
        }
        existingJiraConfiguration.setUrl(jiraConfiguration.getUrl());
      }
      if (jsonNode.has("username")) {
        existingJiraConfiguration.setUsername(jiraConfiguration.getUsername());
      }
      if (jsonNode.has("password")) {
        existingJiraConfiguration.setPassword(jiraConfiguration.getPassword());
      }
      if (jsonNode.has("customFields")) {
        existingJiraConfiguration.setCustomFields(jiraConfiguration.getCustomFields());
      }
      result = existingJiraConfiguration;
    }
    if ((result.getUsername() != null && result.getPassword() == null) ||
        (result.getUsername() == null && result.getPassword() != null)) {
      throw new BadRequestException(NO_USERNAME_PASSWORD_PAIR);
    }
    return result;
  }

  private JiraConfiguration convertFromDTO(ApiJiraConfigurationDTO dto) {
    JiraConfiguration config = new JiraConfiguration();
    config.setUrl(dto.url);
    config.setUsername(dto.username);
    config.setPassword(passwordHandler.encryptPassword(dto.password));
    config.setCustomFields(dto.customFields);
    return config;
  }

  private void auditConfiguration(JiraConfiguration configuration) {
    AuditData.get().setData("url", configuration.getUrl()).setData("username", configuration.getUsername());
  }

  public void applyJiraConfigurationToClients() {
    jiraConfigurationListeners.forEach(JiraConfigurationListener::jiraConfigurationChanged);
  }

  // Visible for testing
  void updateAllClusterNodesFromConfiguration() {
    applyJiraConfigurationToClients();
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this);
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    execute(this::applyJiraConfigurationToClients, log, CONFIG_APPLY_ERROR);
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
