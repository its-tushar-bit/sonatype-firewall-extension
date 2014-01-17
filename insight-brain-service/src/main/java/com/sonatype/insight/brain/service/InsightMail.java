/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.portal.mail.EmailUtil;
import com.sonatype.insight.portal.mail.InsightMailer;

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
    insightMailer = new InsightMailer(mailer, config.getMailConfig());
  }

  public String getCdnUrl() {
    return config.getCdnUrl();
  }

  public void sendHtml(final String mailId, final List<Address> to, final String subject, final String body) {
    final MailRequest message = new MailRequest(mailId, HtmlMailType.HTML_TYPE_ID);

    message.setToAddresses(to);
    message.setExpandedSubject(subject);
    message.setExpandedBody(body);

    EmailUtil.waitForMailStatus(insightMailer.sendMail(message));
  }
}
