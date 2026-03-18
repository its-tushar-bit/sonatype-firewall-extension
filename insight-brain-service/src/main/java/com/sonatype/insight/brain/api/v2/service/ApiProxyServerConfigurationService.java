/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiProxyServerConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.GlobalTenantJob;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ApiProxyServerConfigurationService
    implements InsightJob, GlobalTenantJob
{
  private static final Logger log = LoggerFactory.getLogger(ApiProxyServerConfigurationService.class);

  // based on https://stackoverflow.com/questions/106179/regular-expression-to-match-dns-hostname-or-ip-address
  private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
      "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");

  // Visible for testing
  static final String TASK_NAME = "ProxyServerConfiguration";

  private static final String CONFIG_APPLY_ERROR = "Error when applying proxy server config";

  private final ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  private final PasswordHandler passwordHandler;

  private final Set<ProxyServerConfigurationListener> proxyServerConfigurationListeners;

  private final TaskScheduler taskScheduler;

  @Inject
  public ApiProxyServerConfigurationService(
      ProxyServerConfigurationDAO proxyServerConfigurationDAO,
      PasswordHandler passwordHandler,
      Set<ProxyServerConfigurationListener> proxyServerConfigurationListeners,
      TaskScheduler taskScheduler)
  {
    this.proxyServerConfigurationDAO = proxyServerConfigurationDAO;
    this.passwordHandler = passwordHandler;
    this.proxyServerConfigurationListeners = proxyServerConfigurationListeners;
    this.taskScheduler = taskScheduler;
  }

  private RuntimeException newNotFoundException() {
    return new NotFoundException("Proxy server not configured.");
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiProxyServerConfigurationDTO getConfiguration() {
    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    if (proxyServerConfiguration == null) {
      throw newNotFoundException();
    }

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = proxyServerConfiguration.getHostname();
    configurationDTO.port = proxyServerConfiguration.getPort();
    configurationDTO.username = proxyServerConfiguration.getUsername();
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.excludeHosts = proxyServerConfiguration.getExcludeHostsList();
    return configurationDTO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setConfiguration(ApiProxyServerConfigurationDTO configurationDTO) {
    if (configurationDTO == null) {
      throw new BadRequestException("No proxy server configuration was provided.");
    }

    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    if (proxyServerConfiguration == null) {
      proxyServerConfiguration = new ProxyServerConfiguration();
    }
    else {
      // This is a proxy server configuration update.
      // If the hostname and port are changed, then the user must provide the password.
      // Otherwise, the password can be stolen by using a fake proxy server:
      // - The user starts a fake server that logs the password
      // - The user sets the configuration to the hostname & port of the fake server and passwordIsIncluded to false
      // - Because passwordIsIncluded is false, the system does not update the password field
      // - The HTTP request is sent to the fake server and the password is stolen
      if (!configurationDTO.passwordIsIncluded) {
        if (!proxyServerConfiguration.getHostname().equals(configurationDTO.hostname)
            || proxyServerConfiguration.getPort() != configurationDTO.port)
        {
          clearPassword(configurationDTO);
          throw new BadRequestException("The password must be provided when the hostname or port are updated");
        }
      }
    }

    if (StringUtils.isBlank(configurationDTO.hostname) ||
        !HOSTNAME_PATTERN.matcher(configurationDTO.hostname).matches())
    {
      throw new BadRequestException("Invalid hostname provided for the proxy server");
    }
    proxyServerConfiguration.setHostname(configurationDTO.hostname);
    proxyServerConfiguration.setPort(configurationDTO.port);
    proxyServerConfiguration.setUsername(configurationDTO.username);
    if (configurationDTO.passwordIsIncluded) {
      if (configurationDTO.password != null && configurationDTO.password.length != 0) {
        proxyServerConfiguration.setPassword(passwordHandler.encryptPassword(configurationDTO.password));
      }
      else {
        proxyServerConfiguration.setPassword(null);
      }
    }
    clearPassword(configurationDTO);

    if (configurationDTO.excludeHosts != null && !configurationDTO.excludeHosts.isEmpty()) {
      proxyServerConfiguration.setExcludeHosts(String.join(", ", configurationDTO.excludeHosts));
    }
    else {
      proxyServerConfiguration.setExcludeHosts(null);
    }

    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    auditConfiguration(proxyServerConfiguration);

    updateAllClusterNodesFromConfiguration();
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration() {
    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    if (proxyServerConfiguration == null) {
      throw newNotFoundException();
    }
    proxyServerConfigurationDAO.delete();

    auditConfiguration(proxyServerConfiguration);

    updateAllClusterNodesFromConfiguration();
  }

  private void auditConfiguration(ProxyServerConfiguration proxyServerConfiguration) {
    AuditData.get() //
        .setData("proxyServerHostname", proxyServerConfiguration.getHostname()) //
        .setData("proxyServerPort", proxyServerConfiguration.getPort()) //
        .setData("proxyServerUsername", proxyServerConfiguration.getUsername()) //
        .setData("proxyServerExcludeHosts", proxyServerConfiguration.getExcludeHostsList());
  }

  private void clearPassword(ApiProxyServerConfigurationDTO configurationDTO) {
    if (configurationDTO.password != null && configurationDTO.password.length != 0) {
      Arrays.fill(configurationDTO.password, '0');
    }
  }

  public void applyProxyServerConfigurationToClients() {
    proxyServerConfigurationListeners.forEach(ProxyServerConfigurationListener::proxyServerConfigurationChanged);
  }

  // Visible for testing
  void updateAllClusterNodesFromConfiguration() {
    applyProxyServerConfigurationToClients();
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this);
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::applyProxyServerConfigurationToClients, log, CONFIG_APPLY_ERROR);
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
