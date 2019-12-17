/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since MIGRATE_MAIL_CONFIG
 */
@Named
public class ApiMailConfigurationService
{
  public static final char[] FAKE_PASSWORD = "#~FAKE~PASSWORD~#".toCharArray();

  private final MailConfigurationDAO mailConfigurationDAO;

  private final InsightMail insightMail;

  @Inject
  public ApiMailConfigurationService(MailConfigurationDAO mailConfigurationDAO, InsightMail insightMail) {
    this.mailConfigurationDAO = mailConfigurationDAO;
    this.insightMail = insightMail;
  }

  private RuntimeException newNotFoundException() {
    return new NotFoundException("Mail server not configured.");
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiMailConfigurationDTO getConfiguration() {
    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    if (mailConfiguration == null) {
      throw newNotFoundException();
    }

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = mailConfiguration.getHostname();
    configurationDTO.port = mailConfiguration.getPort();
    configurationDTO.username = mailConfiguration.getUsername();
    configurationDTO.password = FAKE_PASSWORD;
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

    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    if (mailConfiguration == null) {
      mailConfiguration = new MailConfiguration();
    }

    mailConfiguration.setHostname(configurationDTO.hostname);
    mailConfiguration.setPort(configurationDTO.port);
    mailConfiguration.setUsername(configurationDTO.username);
    if (!Arrays.equals(FAKE_PASSWORD, configurationDTO.password)) {
      if (configurationDTO.password != null) {
        mailConfiguration.setPassword(insightMail.encryptPassword(configurationDTO.password));
        Arrays.fill(configurationDTO.password, '0');
      }
      else {
        mailConfiguration.setPassword(null);
      }
    }
    mailConfiguration.setSslEnabled(configurationDTO.sslEnabled);
    mailConfiguration.setStartTlsEnabled(configurationDTO.startTlsEnabled);
    mailConfiguration.setSystemEmail(configurationDTO.systemEmail);

    auditConfiguration(mailConfiguration);
    mailConfigurationDAO.set(mailConfiguration);

    insightMail.loadMailConfiguration();
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration() {
    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    if (mailConfiguration == null) {
      throw newNotFoundException();
    }
    auditConfiguration(mailConfiguration);
    mailConfigurationDAO.delete();

    insightMail.loadMailConfiguration();
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
