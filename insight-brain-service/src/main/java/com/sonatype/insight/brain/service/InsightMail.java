/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class InsightMail
{
  private static final Logger log = LoggerFactory.getLogger(InsightMail.class);

  private static final String ENC = "CMMDwoV";

  private final InsightConfig config;

  private final PlexusCipher cipher;

  @Inject
  public InsightMail(final InsightConfig config, PlexusCipher cipher) {
    this.config = config;
    this.cipher = cipher;
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
    MailConfiguration mailConfiguration = new MailConfigurationDAO().get();
    return mailConfiguration == null ? null : mailConfiguration.getHostname() + ':' + mailConfiguration.getPort();
  }

  public String getCdnUrl() {
    return config.getCdnUrl();
  }

  public void sendHtml(String mailAddress, String subject, String body) {
    MailConfiguration mailConfiguration = new MailConfigurationDAO().get();
    sendHtml(mailConfiguration, mailAddress, subject, body);
  }

  public void sendHtml(MailConfiguration mailConfiguration, String mailAddress, String subject, String body) {
    if (mailConfiguration == null) {
      throw new IllegalStateException("Mail is not configured.");
    }

    long start = System.currentTimeMillis();

    log.debug("Sending mail to {} using server {}:{}.", mailAddress, mailConfiguration.getHostname(),
        mailConfiguration.getPort());

    try {
      HtmlEmail email = new InsightHtmlEmail();
      email.setHostName(mailConfiguration.getHostname());
      if (mailConfiguration.isSslEnabled()) {
        email.setSslSmtpPort(Integer.toString(mailConfiguration.getPort()));
      }
      else {
        email.setSmtpPort(mailConfiguration.getPort());
      }
      email.setSSLOnConnect(mailConfiguration.isSslEnabled());
      email.setStartTLSEnabled(mailConfiguration.isStartTlsEnabled());

      if (StringUtils.isNotBlank(mailConfiguration.getUsername())) {
        email.setAuthentication( //
            mailConfiguration.getUsername(), //
            mailConfiguration.getPassword() == null ? null
                : String.valueOf(decryptPassword(mailConfiguration.getPassword())));
      }

      email.addTo(mailAddress);
      email.setFrom(mailConfiguration.getSystemEmail(), "Nexus IQ Server");

      email.setSubject(subject);
      email.setHtmlMsg(body);

      email.send();

      log.debug("Sent mail to {} in {} ms.", mailAddress, System.currentTimeMillis() - start);
    }
    catch (EmailException e) {
      throw new RuntimeException(e);
    }
  }

  private static class InsightHtmlEmail
      extends HtmlEmail
  {
    @Override
    protected MimeMessage createMimeMessage(Session aSession) {
      return new InsightMimeMessage(aSession);
    }
  }

  private static class InsightMimeMessage
      extends MimeMessage
  {
    public InsightMimeMessage(Session session) {
      super(session);
    }

    /**
     * This method is very slow (can be seconds) in the {@link MimeMessage} parent class.
     * We don't need fancy message IDs.
     */
    @Override
    protected void updateMessageID() throws MessagingException {
      setHeader("Message-ID", UUID.randomUUID().toString());
    }
  }
}
