/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.UUID;
import java.util.function.Supplier;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class InsightMail
{
  private static final Logger log = LoggerFactory.getLogger(InsightMail.class);

  private final Configuration configuration;

  private final PasswordHandler passwordHandler;

  protected final MailConfigurationDAO mailConfigurationDAO;

  @Inject
  public InsightMail(
      Configuration configuration,
      PasswordHandler passwordHandler,
      MailConfigurationDAO mailConfigurationDAO)
  {
    this.configuration = configuration;
    this.passwordHandler = passwordHandler;
    this.mailConfigurationDAO = mailConfigurationDAO;
  }

  public char[] decryptPassword(char[] encryptedPassword) {
    return passwordHandler.decryptPassword(encryptedPassword);
  }

  public char[] encryptPassword(char[] password) {
    return passwordHandler.encryptPassword(password);
  }

  public String getServer() {
    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    return mailConfiguration == null ? null : mailConfiguration.getHostname() + ':' + mailConfiguration.getPort();
  }

  public String getCdnUrl() {
    return configuration.getCdnUrl();
  }

  public void sendHtml(String mailAddress, String subject, String body) {
    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
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
      HtmlEmail email = createHtmlEmail();
      email.setHostName(mailConfiguration.getHostname());
      if (mailConfiguration.isSslEnabled()) {
        email.setSslSmtpPort(Integer.toString(mailConfiguration.getPort()));
      }
      else {
        email.setSmtpPort(mailConfiguration.getPort());
      }
      email.setSSLOnConnect(mailConfiguration.isSslEnabled());
      email.setStartTLSEnabled(mailConfiguration.isStartTlsEnabled());
      email.setSSLCheckServerIdentity(true);

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

  /**
   * Factory for creating HtmlEmail instances. Can be replaced for testing.
   */
  private static volatile Supplier<HtmlEmail> htmlEmailFactory = InsightHtmlEmail::new;

  /**
   * Sets a custom factory for creating HtmlEmail instances. This is primarily used for testing to intercept email
   * sends.
   *
   * @param factory the factory to use, or null to reset to default
   */
  public static void setHtmlEmailFactory(Supplier<HtmlEmail> factory) {
    htmlEmailFactory = factory != null ? factory : InsightHtmlEmail::new;
  }

  /**
   * Creates the HtmlEmail instance to use for sending. Uses the configured factory, which can be replaced for testing.
   */
  protected HtmlEmail createHtmlEmail() {
    return htmlEmailFactory.get();
  }

  /**
   * Custom HtmlEmail that uses faster message ID generation and supports testing. This class is protected to allow test
   * subclasses to override email behavior.
   */
  protected static class InsightHtmlEmail
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
     * This method is very slow (can be seconds) in the {@link MimeMessage} parent class. We don't need fancy message
     * IDs.
     */
    @Override
    protected void updateMessageID() throws MessagingException {
      setHeader("Message-ID", UUID.randomUUID().toString());
    }
  }
}
