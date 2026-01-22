/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;

import org.apache.commons.io.IOUtils;
import org.apache.commons.mail2.core.EmailConstants;
import org.junit.Test;
import com.sonatype.insight.brain.test.MailboxTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class InsightMailTest
    extends AbstractComponentTest
{
  @Inject
  private MailConfigurationDAO mailConfigurationDAO;

  @Inject
  private InsightMail insightMail;

  @Test
  public void testGetServer_MailConfigured() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("testHostname");
    mailConfiguration.setPort(5555);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword(insightMail.encryptPassword("testPassword".toCharArray()));
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);
    mailConfiguration.setSystemEmail("testfrom@sonatype.com");
    mailConfigurationDAO.set(mailConfiguration);

    assertThat(insightMail.getServer()).isEqualTo("testHostname:5555");
  }

  @Test
  public void testGetServer_MailNotConfigured() {
    assertThat(insightMail.getServer()).isNull();
  }

  @Test
  public void testSendHtml_MailConfigured_SslEnabled() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("mail.example.com");
    mailConfiguration.setPort(12345);
    mailConfiguration.setSystemEmail("noreply@example.com");
    mailConfiguration.setSslEnabled(true);

    testSendHtml_MailConfigured(mailConfiguration);
  }

  @Test
  public void testSendHtml_MailConfigured_StartTlsEnabled() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("mail.example.com");
    mailConfiguration.setPort(12345);
    mailConfiguration.setSystemEmail("noreply@example.com");
    mailConfiguration.setStartTlsEnabled(true);

    testSendHtml_MailConfigured(mailConfiguration);
  }

  @Test
  public void testSendHtml_MailConfigured_NoAuthentication() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("mail.example.com");
    mailConfiguration.setPort(12345);
    mailConfiguration.setSystemEmail("noreply@example.com");

    testSendHtml_MailConfigured(mailConfiguration);
  }

  @Test
  public void testSendHtml_MailConfigured_Authentication() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("mail.example.com");
    mailConfiguration.setPort(12345);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword(insightMail.encryptPassword("testPassword".toCharArray()));
    mailConfiguration.setSystemEmail("noreply@example.com");

    testSendHtml_MailConfigured(mailConfiguration);
  }

  @Test
  public void testSendHtml_MailConfigured_Authentication_NoPassword() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("mail.example.com");
    mailConfiguration.setPort(12345);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword(null);
    mailConfiguration.setSystemEmail("noreply@example.com");

    testSendHtml_MailConfigured(mailConfiguration);
  }

  private void testSendHtml_MailConfigured(MailConfiguration mailConfiguration) throws Exception {
    mailConfigurationDAO.set(mailConfiguration);

    String toEmailAddress = "testuser@example.com";
    MailboxTestUtil.clearAll();
    List<Message> emails = MailboxTestUtil.get(toEmailAddress);

    String subject = "testSubject";
    String message = "testMessage";
    insightMail.sendHtml(toEmailAddress, subject, message);

    assertThat(emails).hasSize(1);
    Message email = emails.get(0);

    // Assert mail server
    Session session = email.getSession();
    assertThat(session.getProperties()) //
        .containsEntry(EmailConstants.MAIL_HOST, mailConfiguration.getHostname())
        .containsEntry(EmailConstants.MAIL_PORT, String.valueOf(mailConfiguration.getPort()))
        .containsEntry(EmailConstants.MAIL_TRANSPORT_STARTTLS_ENABLE,
            String.valueOf(mailConfiguration.isStartTlsEnabled()));

    if (mailConfiguration.isStartTlsEnabled() || mailConfiguration.isSslEnabled()) {
      assertThat(session.getProperties()).containsEntry(EmailConstants.MAIL_SMTP_SSL_CHECKSERVERIDENTITY, "true");
    }
    else {
      assertThat(session.getProperties()).doesNotContainEntry(EmailConstants.MAIL_SMTP_SSL_CHECKSERVERIDENTITY, "true");
    }

    // Assert authentication
    PasswordAuthentication passwordAuthentication = session.requestPasswordAuthentication(null, 0, null, null, null);
    if (mailConfiguration.getUsername() == null) {
      assertThat(passwordAuthentication).isNull();
    }
    else {
      assertThat(passwordAuthentication.getUserName()).isEqualTo(mailConfiguration.getUsername());
      if (mailConfiguration.getPassword() == null) {
        assertThat(passwordAuthentication.getPassword()).isNull();
      }
      else {
        assertThat(passwordAuthentication.getPassword())
            .isEqualTo(String.valueOf(insightMail.decryptPassword(mailConfiguration.getPassword())));
      }
    }

    // Assert "to" and "from" addresses
    Address[] recipients = email.getRecipients(RecipientType.TO);
    assertThat(recipients).hasSize(1);
    assertThat(recipients[0].toString()).isEqualTo(toEmailAddress);
    assertThat(email.getFrom()[0].toString()).isEqualTo("Nexus IQ Server <" + mailConfiguration.getSystemEmail() + ">");
    
    // Assert email subject and body
    assertThat(email.getSubject()).isEqualTo(subject);
    String emailBody = IOUtils.toString(email.getInputStream(), StandardCharsets.UTF_8);
    assertThat(emailBody).contains(message);
  }

  @Test
  public void testSendHtml_MailConfigurationNull() {
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () -> insightMail.sendHtml(null /* mailConfiguration */, "test@example.com", "testSubject", "testBody"))
        .withMessage("Mail is not configured.");
  }

  @Test
  public void testSendHtml_MailNotConfigured() {
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> insightMail.sendHtml("test@example.com", "testSubject", "testBody"))
        .withMessage("Mail is not configured.");
  }

  @Test
  public void testEncryptDecryptPassword() {
    char[] password = "testPassword".toCharArray();
    assertThat(insightMail.encryptPassword(password)).isNotEqualTo(password);
    assertThat(insightMail.decryptPassword(insightMail.encryptPassword(password))).isEqualTo(password);
  }

  @Test
  public void testEncryptPassword_Null() {
    assertThat(insightMail.encryptPassword(null)).isNull();
  }

  @Test
  public void testDecryptPassword_Null() {
    assertThat(insightMail.decryptPassword(null)).isNull();
  }
}
