/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.migration.MailConfigurationMigrator.MailConfig;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class MultiTenantMailConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantMailConfigurationService.class);

  private final MailConfigurationDAO mailConfigurationDAO;

  private final InsightConfig insightConfig;

  private final PasswordHandler passwordHandler;

  @Inject
  public MultiTenantMailConfigurationService(
      final MailConfigurationDAO mailConfigurationDAO,
      final InsightConfig insightConfig,
      final PasswordHandler passwordHandler)
  {
    this.mailConfigurationDAO = mailConfigurationDAO;
    this.insightConfig = insightConfig;
    this.passwordHandler = passwordHandler;
  }

  @PostConstruct
  public void init() {
    log.info("Setting smtp email configuration");

    MailConfig mailConfig = insightConfig.getMailConfig();
    if (mailConfig == null) {
      log.error("Global smtp email configuration cannot be null");
      return;
    }

    runAsGlobal(() -> {
      MailConfiguration mailConfiguration = buildMailConfiguration(mailConfig);

      log.info("Saving or Updating global smtp email configuration");
      mailConfigurationDAO.set(mailConfiguration);

      return null;
    });
  }

  private MailConfiguration buildMailConfiguration(final MailConfig mailConfig) {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname(mailConfig.getHostname());
    mailConfiguration.setPort(mailConfig.getPort());
    mailConfiguration.setSystemEmail(mailConfig.getSystemEmail());
    mailConfiguration.setUsername(mailConfig.getUsername());
    mailConfiguration.setPassword(passwordHandler.encryptPassword(mailConfig.getPassword()));
    mailConfiguration.setSslEnabled(mailConfig.isSsl());
    return mailConfiguration;
  }
}
