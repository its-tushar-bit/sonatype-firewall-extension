/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.mail.InsightMailer;

import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.EmailerConfiguration;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.MailRequestStatus;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InsightMailTest
    extends AbstractComponentTest
{
  @Mock
  private EMailer eMailerMock;

  @Test
  public void testConstructor_MailConfigured() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("testHostname");
    mailConfiguration.setPort(5555);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword("testPassword");
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);
    mailConfiguration.setSystemEmail("testfrom@sonatype.com");
    new MailConfigurationDAO().set(mailConfiguration);

    ArgumentCaptor<EmailerConfiguration> emailerConfigurationArgumentCaptor =
        ArgumentCaptor.forClass(EmailerConfiguration.class);
    InsightMail insightMail = new InsightMail(null /* insightConfig */, eMailerMock);
    verify(eMailerMock).configure(emailerConfigurationArgumentCaptor.capture());
    EmailerConfiguration emailerConfiguration = emailerConfigurationArgumentCaptor.getValue();
    assertThat(emailerConfiguration.getMailHost()).isEqualTo("testHostname");
    assertThat(emailerConfiguration.getMailPort()).isEqualTo(5555);
    assertThat(emailerConfiguration.getUsername()).isEqualTo("testUsername");
    assertThat(emailerConfiguration.getPassword()).isEqualTo("testPassword");
    assertThat(emailerConfiguration.isSsl()).isTrue();
    assertThat(emailerConfiguration.isTls()).isTrue();

    InsightMailer insightMailer = insightMail.getInsightMailer();
    assertThat(insightMailer.getSystemEmail()).isEqualTo("testfrom@sonatype.com");
    assertThat(insightMailer.getSystemPersonal()).isEqualTo("Nexus IQ Server");
  }

  @Test
  public void testConstructor_MailNotConfigured() {
    // Should not throw any exception
    new InsightMail(null /* insightConfig */, eMailerMock);
  }

  @Test
  public void testGetServer_MailConfigured() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("testHostname");
    mailConfiguration.setPort(5555);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword("testPassword");
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);
    mailConfiguration.setSystemEmail("testfrom@sonatype.com");
    new MailConfigurationDAO().set(mailConfiguration);

    assertThat(new InsightMail(null /* insightConfig */, eMailerMock).getServer()).isEqualTo("testHostname:5555");
  }

  @Test
  public void testGetServer_MailNotConfigured() {
    assertThat(new InsightMail(null /* insightConfig */, eMailerMock).getServer()).isNull();
  }

  @Test
  public void testSendHtml_MailConfigured() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("testHostname");
    mailConfiguration.setPort(5555);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword("testPassword");
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);
    mailConfiguration.setSystemEmail("testfrom@sonatype.com");
    new MailConfigurationDAO().set(mailConfiguration);

    ArgumentCaptor<MailRequest> emailerMailRequestArgumentCaptor = ArgumentCaptor.forClass(MailRequest.class);
    MailRequestStatus mailRequestStatus = new MailRequestStatus(new MailRequest("id", "mailTypeId"));
    mailRequestStatus.setSent(true);
    when(eMailerMock.sendMail(any(MailRequest.class))).thenReturn(mailRequestStatus);
    new InsightMail(null /* insightConfig */, eMailerMock).sendHtml("testMailId", "test@example.com", "testSubject",
        "testBody");
    verify(eMailerMock).sendMail(emailerMailRequestArgumentCaptor.capture());
    MailRequest mailRequest = emailerMailRequestArgumentCaptor.getValue();
    assertThat(mailRequest.getFrom().getMailAddress()).isEqualTo("testfrom@sonatype.com");
  }

  @Test
  public void testSendHtml_MailNotConfigured() {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      new InsightMail(null /* insightConfig */, eMailerMock).sendHtml("testMailId", "test@example.com", "testSubject",
          "testBody");
    }).withMessage("Mail is not configured.");
  }
}
