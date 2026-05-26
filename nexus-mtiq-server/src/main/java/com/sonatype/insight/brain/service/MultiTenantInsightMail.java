/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.context.annotation.Primary;

@Named
@Singleton
@Primary
public class MultiTenantInsightMail
    extends InsightMail
{
  @Inject
  public MultiTenantInsightMail(
      final Configuration configuration,
      final PasswordHandler passwordHandler,
      final MailConfigurationDAO mailConfigurationDAO)
  {
    super(configuration, passwordHandler, mailConfigurationDAO);
  }

  @Override
  public void sendHtml(String mailAddress, String subject, String body) {
    MailConfiguration mailConfigFromSpecificTenant = mailConfigurationDAO.getWithoutFallback();
    if (mailConfigFromSpecificTenant != null) {
      sendHtml(mailConfigFromSpecificTenant, mailAddress, subject, body);
    }
    else {
      runAsGlobal(() -> {
        MailConfiguration mailConfigFromGlobalTenant = mailConfigurationDAO.get();
        sendHtml(mailConfigFromGlobalTenant, mailAddress, subject, body);
        return null;
      });
    }
  }
}
