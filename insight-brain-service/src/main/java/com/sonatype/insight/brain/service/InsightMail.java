/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.migration.MailConfigurationMigrator.MailConfig;
import com.sonatype.insight.mail.EmailUtil;
import com.sonatype.insight.mail.InsightMailer;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.imp.HtmlMailType;

@Named
@Singleton
public class InsightMail
{
  private final InsightMailer insightMailer;

  private final InsightConfig config;

  @Inject
  public InsightMail(final InsightConfig config, final EMailer mailer) {
    this.config = config;
    // CLM-14357: rework to use current db config
    MailConfig mailConfig = config.getMailConfig();
    if (mailConfig == null) {
      mailConfig = new MailConfig();
    }
    insightMailer = new InsightMailer(mailer, mailConfig);
  }

  public String getServer() {
    return insightMailer.getHostname() + ':' + insightMailer.getPort();
  }

  public String getCdnUrl() {
    return config.getCdnUrl();
  }

  public void sendHtml(final String mailId, final String mailAddress, final String subject, final String body) {
    final MailRequest message = new MailRequest(mailId, HtmlMailType.HTML_TYPE_ID);

    message.setToAddresses(Collections.singletonList(new Address(mailAddress)));
    message.setExpandedSubject(subject);
    message.setExpandedBody(body);

    EmailUtil.waitForMailStatus(insightMailer.sendMail(message));
  }
}
