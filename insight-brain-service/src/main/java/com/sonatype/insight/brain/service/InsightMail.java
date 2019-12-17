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

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.mail.EmailUtil;
import com.sonatype.insight.mail.InsightMailer;
import com.sonatype.insight.mail.MailConfig;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.imp.HtmlMailType;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class InsightMail
{
  private static final Logger log = LoggerFactory.getLogger(InsightMail.class);

  private static final String ENC = "CMMDwoV";

  private final EMailer eMailer;

  private volatile InsightMailer insightMailer;

  private final InsightConfig config;

  private final PlexusCipher cipher;

  @Inject
  public InsightMail(final InsightConfig config, final EMailer eMailer, PlexusCipher cipher) {
    this.config = config;
    this.eMailer = eMailer;
    this.cipher = cipher;

    loadMailConfiguration();
  }

  public void loadMailConfiguration() {
    MailConfig mailConfig = getMailConfig();
    if (mailConfig == null) {
      insightMailer = null;
    }
    else {
      insightMailer = new InsightMailer(eMailer, mailConfig);
      mailConfig.clearPassword();
    }
  }

  private MailConfig getMailConfig() {
    MailConfiguration mailConfiguration = new MailConfigurationDAO().get();
    if (mailConfiguration == null) {
      log.debug("Mail is not configured.");
      return null;
    }

    MailConfig mailConfig = new MailConfig();
    mailConfig.setHostname(mailConfiguration.getHostname());
    mailConfig.setPort(mailConfiguration.getPort());
    mailConfig.setSsl(mailConfiguration.isSslEnabled());
    mailConfig.setTls(mailConfiguration.isStartTlsEnabled());
    mailConfig.setUsername(mailConfiguration.getUsername());
    mailConfig.setPassword(decryptPassword(mailConfiguration.getPassword()));
    mailConfig.setSystemEmail(mailConfiguration.getSystemEmail());
    return mailConfig;
  }

  public char[] decryptPassword(char[] encryptedPassword) {
    if (encryptedPassword == null) {
      return null;
    }

    try {
      synchronized (cipher) {
        return cipher.decryptDecorated(String.valueOf(encryptedPassword), ENC).toCharArray();
      }
    }
    catch (PlexusCipherException e) {
      throw new IllegalStateException(e);
    }
  }

  public char[] encryptPassword(char[] password) {
    if (password == null) {
      return null;
    }

    try {
      synchronized (cipher) {
        return cipher.encryptAndDecorate(String.valueOf(password), ENC).toCharArray();
      }
    }
    catch (PlexusCipherException e) {
      throw new IllegalStateException(e);
    }
  }

  public String getServer() {
    return insightMailer == null ? null : insightMailer.getHostname() + ':' + insightMailer.getPort();
  }

  public String getCdnUrl() {
    return config.getCdnUrl();
  }

  public void sendHtml(final String mailId, final String mailAddress, final String subject, final String body) {
    if (insightMailer == null) {
      throw new IllegalStateException("Mail is not configured.");
    }

    final MailRequest message = new MailRequest(mailId, HtmlMailType.HTML_TYPE_ID);

    message.setToAddresses(Collections.singletonList(new Address(mailAddress)));
    message.setExpandedSubject(subject);
    message.setExpandedBody(body);

    EmailUtil.waitForMailStatus(insightMailer.sendMail(message));
  }

  /* Visible for tests only */
  public InsightMailer getInsightMailer() {
    return insightMailer;
  }
}
