/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiReverseProxyAuthenticationConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.error.exception.BadRequestException;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ApiReverseProxyAuthenticationConfigurationService
    implements Job
{
  private static final Logger log = LoggerFactory.getLogger(ApiReverseProxyAuthenticationConfigurationService.class);

  // Visible for testing
  public static final String NO_DTO_ERROR_MSG = "A reverse proxy authentication configuration must be specified.";

  // Visible for testing
  static final String TASK_NAME = "ReverseProxyAuthenticationConfiguration";

  private final ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO;

  private final List<ReverseProxyAuthenticationConfigurationListener> reverseProxyAuthenticationConfigurationListeners;

  private final TaskScheduler taskScheduler;

  @Inject
  public ApiReverseProxyAuthenticationConfigurationService(
      ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO,
      List<ReverseProxyAuthenticationConfigurationListener> reverseProxyAuthenticationConfigurationListeners,
      TaskScheduler taskScheduler)
  {
    this.reverseProxyAuthenticationConfigurationDAO = reverseProxyAuthenticationConfigurationDAO;
    this.reverseProxyAuthenticationConfigurationListeners = reverseProxyAuthenticationConfigurationListeners;
    this.taskScheduler = taskScheduler;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiReverseProxyAuthenticationConfigurationDTO getConfiguration() {
    return convertToDTO(reverseProxyAuthenticationConfigurationDAO.getNotNull());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setConfiguration(ApiReverseProxyAuthenticationConfigurationDTO dto) {
    if (dto == null) {
      throw new BadRequestException(NO_DTO_ERROR_MSG);
    }
    ReverseProxyAuthenticationConfiguration config = convertFromDTO(dto);
    auditConfiguration(config);
    reverseProxyAuthenticationConfigurationDAO.set(config);
    updateAllClusterNodesFromConfiguration();
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration() {
    ReverseProxyAuthenticationConfiguration config = reverseProxyAuthenticationConfigurationDAO.getNotNull();
    auditConfiguration(config);
    reverseProxyAuthenticationConfigurationDAO.delete();
    updateAllClusterNodesFromConfiguration();
  }

  private ApiReverseProxyAuthenticationConfigurationDTO convertToDTO(ReverseProxyAuthenticationConfiguration config) {
    ApiReverseProxyAuthenticationConfigurationDTO dto = new ApiReverseProxyAuthenticationConfigurationDTO();
    dto.enabled = config.isEnabled();
    dto.usernameHeader = config.getUsernameHeader();
    dto.csrfProtectionDisabled = config.isCsrfProtectionDisabled();
    dto.logoutUrl = config.getLogoutUrl();
    return dto;
  }

  private ReverseProxyAuthenticationConfiguration convertFromDTO(ApiReverseProxyAuthenticationConfigurationDTO dto) {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setEnabled(dto.enabled);
    if (dto.usernameHeader != null) {
      config.setUsernameHeader(dto.usernameHeader);
    }
    config.setCsrfProtectionDisabled(dto.csrfProtectionDisabled);
    config.setLogoutUrl(dto.logoutUrl);
    return config;
  }

  private void auditConfiguration(ReverseProxyAuthenticationConfiguration configuration) {
    AuditData.get()
        .setData("enabled", configuration.isEnabled())
        .setData("usernameHeader", configuration.getUsernameHeader())
        .setData("csrfProtectionDisabled", configuration.isCsrfProtectionDisabled())
        .setData("logoutUrl", configuration.getLogoutUrl());
  }

  public void applyReverseProxyAuthenticationConfigurationToClients() {
    reverseProxyAuthenticationConfigurationListeners.forEach(
        ReverseProxyAuthenticationConfigurationListener::reverseProxyAuthenticationConfigurationChanged);
  }

  // Visible for testing
  void updateAllClusterNodesFromConfiguration() {
    applyReverseProxyAuthenticationConfigurationToClients();
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(getClass(), TASK_NAME);
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      applyReverseProxyAuthenticationConfigurationToClients();
    }
    catch (Exception e) {
      log.error("Error when applying reverse proxy authentication config: {}", e.getMessage(), e);
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
