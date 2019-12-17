/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;

import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.MailRequestStatus;

import com.google.inject.Binder;
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

  @Inject
  private InsightMail insightMail;

  @Override
  public void configure(Binder binder) {
    binder.bind(EMailer.class).toInstance(eMailerMock);

    super.configure(binder);
  }

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
    new MailConfigurationDAO().set(mailConfiguration);
    insightMail.loadMailConfiguration();

    assertThat(insightMail.getServer()).isEqualTo("testHostname:5555");
  }

  @Test
  public void testGetServer_MailNotConfigured() {
    assertThat(insightMail.getServer()).isNull();
  }

  @Test
  public void testSendHtml_MailConfigured() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("testHostname");
    mailConfiguration.setPort(5555);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword(insightMail.encryptPassword("testPassword".toCharArray()));
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);
    mailConfiguration.setSystemEmail("testfrom@sonatype.com");
    new MailConfigurationDAO().set(mailConfiguration);
    insightMail.loadMailConfiguration();

    ArgumentCaptor<MailRequest> emailerMailRequestArgumentCaptor = ArgumentCaptor.forClass(MailRequest.class);
    MailRequestStatus mailRequestStatus = new MailRequestStatus(new MailRequest("id", "mailTypeId"));
    mailRequestStatus.setSent(true);
    when(eMailerMock.sendMail(any(MailRequest.class))).thenReturn(mailRequestStatus);
    insightMail.sendHtml("testMailId", "test@example.com", "testSubject", "testBody");
    verify(eMailerMock).sendMail(emailerMailRequestArgumentCaptor.capture());
    MailRequest mailRequest = emailerMailRequestArgumentCaptor.getValue();
    assertThat(mailRequest.getFrom().getMailAddress()).isEqualTo("testfrom@sonatype.com");
  }

  @Test
  public void testSendHtml_MailNotConfigured() {
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      insightMail.sendHtml("testMailId", "test@example.com", "testSubject", "testBody");
    }).withMessage("Mail is not configured.");
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
