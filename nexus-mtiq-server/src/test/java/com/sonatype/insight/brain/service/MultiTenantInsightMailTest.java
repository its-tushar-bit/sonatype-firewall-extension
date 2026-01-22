/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.test.MailboxTestUtil;

import jakarta.mail.Message;
import jakarta.mail.Session;
import org.apache.commons.mail2.core.EmailConstants;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MultiTenantInsightMailTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private PasswordHandler passwordHandler;

  private MailConfigurationDAO mailConfigurationDAO;

  private MultiTenantInsightMail underTest;

  @Before
  public void setup() {
    passwordHandler = lookup(PasswordHandler.class);
    mailConfigurationDAO = lookup(MailConfigurationDAO.class);
    underTest = (MultiTenantInsightMail) getTestCLMServer().getCLMServer().getInstance(InsightMail.class);
  }

  @Test
  public void testSendHtml_WhenExistsCustomMailConfigForTenant() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("mail.example.com");
    mailConfiguration.setPort(12345);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword(passwordHandler.encryptPassword("testPassword".toCharArray()));
    mailConfiguration.setSystemEmail("noreply@example.com");

    mailConfigurationDAO.set(mailConfiguration);

    testSendHtml_MailConfigured(mailConfiguration);
  }

  @Test
  public void testSendHtml_WhenOnlyExistsGlobalMailConfig() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("mailglobal.example.com");
    mailConfiguration.setPort(123);
    mailConfiguration.setUsername("testUsername");
    // encrypt password using current PasswordHandler to ensure compatibility
    String encryptedPassword = passwordHandler.encryptPassword("testPassword");
    mailConfiguration.setPassword(encryptedPassword.toCharArray());
    mailConfiguration.setSystemEmail("noreplyglobal@example.com");
    testAsGlobal(g -> mailConfigurationDAO.set(mailConfiguration));

    try {
      testSendHtml_MailConfigured(mailConfiguration);
    }
    finally {
      testAsGlobal(g -> mailConfigurationDAO.delete());
    }
  }

  @Test
  public void testSendHtml_MailConfigurationNull() {
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () -> underTest.sendHtml("testuser@example.com", "testSubject", "testMessage"))
        .withMessage("Mail is not configured.");
  }

  private void testSendHtml_MailConfigured(MailConfiguration mailConfiguration) throws Exception {
    String toEmailAddress = "testuser@example.com";
    MailboxTestUtil.clearAll();
    List<Message> emails = MailboxTestUtil.get(toEmailAddress);

    String subject = "testSubject";
    String message = "testMessage";
    underTest.sendHtml(toEmailAddress, subject, message);

    assertThat(emails).hasSize(1);
    Message email = emails.get(0);

    // Assert mail server
    Session session = email.getSession();
    assertThat(session.getProperties())
        .containsEntry(EmailConstants.MAIL_HOST, mailConfiguration.getHostname())
        .containsEntry(EmailConstants.MAIL_PORT, String.valueOf(mailConfiguration.getPort()));
  }
}
