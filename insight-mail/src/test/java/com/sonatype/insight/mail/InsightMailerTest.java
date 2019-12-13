/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mail;

import java.util.Arrays;
import java.util.UUID;

import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.EmailerConfiguration;
import org.sonatype.micromailer.MailRequest;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InsightMailerTest
    extends InjectedTest
{
  @Rule
  public GreenMailRule smtpServer = new GreenMailRule(new ServerSetup(0, null, ServerSetup.PROTOCOL_SMTP));

  @Test
  public void sendTestMail() throws Exception {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setHostname("localhost");
    mailConfig.setPort(smtpServer.getSmtp().getPort());
    mailConfig.setSystemEmail("testFrom@sonatype.com");
    InsightMailer mailer = new InsightMailer(lookup(EMailer.class), mailConfig);

    String toAddr = UUID.randomUUID() + "testTo@sonatype.com";
    String subject = UUID.randomUUID().toString();
    String msg = UUID.randomUUID().toString();
    MailRequest request = mailer.getDefaultMailRequest(subject, msg);
    request.setToAddresses(Arrays.asList(new Address(toAddr)));
    mailer.sendMail(request);
    assertThat(smtpServer.waitForIncomingEmail(1)).isTrue();
    MimeMessage mail = smtpServer.getReceivedMessages()[0];
    System.out.println(GreenMailUtil.getHeaders(mail));
    assertThat(GreenMailUtil.getBody(mail)).contains(msg);
    assertThat(mail.getRecipients(RecipientType.TO)[0].toString()).isEqualTo(toAddr);
  }

  @Test
  public void getEmailConfigurationWithSslAndTlsToPort25() throws Exception {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setSsl(true);
    mailConfig.setTls(true);
    mailConfig.setPort(25);
    EMailer eMailer = mock(EMailer.class);
    ArgumentCaptor<EmailerConfiguration> emailerConfigurationArgumentCaptor = ArgumentCaptor
        .forClass(EmailerConfiguration.class);
    new InsightMailer(eMailer, mailConfig);
    verify(eMailer).configure(emailerConfigurationArgumentCaptor.capture());
    assertThat(emailerConfigurationArgumentCaptor.getValue().getMailPort()).isEqualTo(25);
  }
}
