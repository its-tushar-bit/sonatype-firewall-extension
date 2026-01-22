/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.83
 */
@Named
public class ApiMailConfigurationService
{
  private final Logger log = LoggerFactory.getLogger(ApiMailConfigurationService.class);

  private final MailConfigurationDAO mailConfigurationDAO;

  private final InsightMail insightMail;

  private final BaseUrl baseUrl;

  @Inject
  public ApiMailConfigurationService(
      MailConfigurationDAO mailConfigurationDAO,
      InsightMail insightMail,
      BaseUrl baseUrl)
  {
    this.mailConfigurationDAO = mailConfigurationDAO;
    this.insightMail = insightMail;
    this.baseUrl = baseUrl;
  }

  private RuntimeException newNotFoundException() {
    return new NotFoundException("Mail server not configured.");
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiMailConfigurationDTO getConfiguration() {
    MailConfiguration mailConfiguration = mailConfigurationDAO.getWithoutFallback();
    if (mailConfiguration == null) {
      throw newNotFoundException();
    }

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = mailConfiguration.getHostname();
    configurationDTO.port = mailConfiguration.getPort();
    configurationDTO.username = mailConfiguration.getUsername();
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.sslEnabled = mailConfiguration.isSslEnabled();
    configurationDTO.startTlsEnabled = mailConfiguration.isStartTlsEnabled();
    configurationDTO.systemEmail = mailConfiguration.getSystemEmail();
    return configurationDTO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setConfiguration(ApiMailConfigurationDTO configurationDTO) {
    if (configurationDTO == null) {
      throw new BadRequestException("No mail server configuration was provided.");
    }

    MailConfiguration mailConfiguration = importMailConfigurationFromDto(configurationDTO);

    auditConfiguration(mailConfiguration);
    mailConfigurationDAO.set(mailConfiguration);
  }

  private MailConfiguration importMailConfigurationFromDto(
      ApiMailConfigurationDTO configurationDTO)
  {
    MailConfiguration mailConfiguration = mailConfigurationDAO.getWithoutFallback();
    if (mailConfiguration == null) {
      mailConfiguration = new MailConfiguration();
    }
    else {
      // This is a mail configuration update.
      // If the hostname and port are changed, then the user must provide the password.
      // Otherwise, the password can be stolen by using a fake email server:
      // - The user starts a fake server that logs the password
      // - The user sets the configuration to the hostname & port of the fake server and passwordIsIncluded to false
      // - Because passwordIsIncluded is false, the system does not update the password field
      // - The next email notification sends a request to the fake server and the password is stolen
      if (!configurationDTO.passwordIsIncluded) {
        if (!mailConfiguration.getHostname().equals(configurationDTO.hostname)
            || mailConfiguration.getPort() != configurationDTO.port) {
          clearPassword(configurationDTO);
          throw new BadRequestException("The password must be provided when the hostname or port are updated");
        }
      }
    }

    mailConfiguration.setHostname(configurationDTO.hostname);
    mailConfiguration.setPort(configurationDTO.port);
    mailConfiguration.setUsername(configurationDTO.username);
    if (configurationDTO.passwordIsIncluded) {
      if (configurationDTO.password != null && configurationDTO.password.length != 0) {
        mailConfiguration.setPassword(insightMail.encryptPassword(configurationDTO.password));
      }
      else {
        mailConfiguration.setPassword(null);
      }
    }
    mailConfiguration.setSslEnabled(configurationDTO.sslEnabled);
    mailConfiguration.setStartTlsEnabled(configurationDTO.startTlsEnabled);
    mailConfiguration.setSystemEmail(configurationDTO.systemEmail);
    clearPassword(configurationDTO);
    return mailConfiguration;
  }

  private void clearPassword(ApiMailConfigurationDTO configurationDTO) {
    if (configurationDTO.password != null && configurationDTO.password.length != 0) {
      Arrays.fill(configurationDTO.password, '0');
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration() {
    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    if (mailConfiguration == null) {
      throw newNotFoundException();
    }
    auditConfiguration(mailConfiguration);
    mailConfigurationDAO.delete();
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void testConfiguration(String recipientEmail, ApiMailConfigurationDTO mailConfigurationDTO) {
    if (mailConfigurationDTO == null) {
      throw new BadRequestException("No mail server configuration was provided.");
    }

    MailConfiguration mailConfiguration = importMailConfigurationFromDto(mailConfigurationDTO);
    mailConfigurationDAO.validate(mailConfiguration);

    try {
      String subject = "Test Email Configuration";
      String messageBody = "Success! This is a test mail from " + baseUrl.getConfigured();
      insightMail.sendHtml(mailConfiguration, recipientEmail, subject, messageBody);
    }
    catch (Exception e) {
      String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
      String errorMessage = String.format("Test mail configuration failed. Error ID %s. %s", id, e.getMessage());
      log.debug(errorMessage, e);
      throw new BadRequestException(errorMessage, e);
    }
  }

  private void auditConfiguration(MailConfiguration mailConfiguration) {
    AuditData.get() //
        .setData("smtpHostname", mailConfiguration.getHostname()) //
        .setData("smtpPort", mailConfiguration.getPort()) //
        .setData("smtpUsername", mailConfiguration.getUsername()) //
        .setData("smtpSsl", mailConfiguration.isSslEnabled() ? "enabled" : "disabled") //
        .setData("smtpStartTls", mailConfiguration.isStartTlsEnabled() ? "enabled" : "disabled") //
        .setData("smtpSystemEmail", mailConfiguration.getSystemEmail());
  }
}
