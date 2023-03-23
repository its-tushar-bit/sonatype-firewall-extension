/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.migration.MailConfigurationMigrator.MailConfig;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

@Named
@Singleton
public class MultiTenantMailConfigurationService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantMailConfigurationService.class);

  private final MailConfigurationDAO mailConfigurationDAO;

  private final InsightConfig insightConfig;

  private final PasswordHandler passwordHandler;

  @Inject
  public MultiTenantMailConfigurationService(final MailConfigurationDAO mailConfigurationDAO,
                                             final InsightConfig insightConfig,
                                             final PasswordHandler passwordHandler)
  {
    this.mailConfigurationDAO = mailConfigurationDAO;
    this.insightConfig = insightConfig;
    this.passwordHandler = passwordHandler;
  }

  @Override
  public void start() throws Exception {
    runAsGlobal(() -> {
      MailConfiguration mailConfiguration = getMailConfiguration();
      if (mailConfiguration != null && mailConfigurationDAO.get() == null) {
        log.info("Global smtp email configuration not set, updating with configured values");
        mailConfigurationDAO.set(mailConfiguration);
      }
      return null;
    });
  }

  private MailConfiguration getMailConfiguration() {
    MailConfig mailConfig = insightConfig.getMailConfig();
    if (mailConfig != null) {
      MailConfiguration mailConfiguration = new MailConfiguration();
      mailConfiguration.setHostname(mailConfig.getHostname());
      mailConfiguration.setPort(mailConfig.getPort());
      mailConfiguration.setSystemEmail(mailConfig.getSystemEmail());
      mailConfiguration.setUsername(mailConfig.getUsername());
      mailConfiguration.setPassword(passwordHandler.encryptPassword(mailConfig.getPassword()));
      mailConfiguration.setSslEnabled(mailConfig.isSsl());
      return mailConfiguration;
    }
    return null;
  }
}
