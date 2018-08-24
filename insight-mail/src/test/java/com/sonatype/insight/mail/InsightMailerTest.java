/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mail;

import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.EmailerConfiguration;
import org.sonatype.micromailer.MailRequest;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.commons.lang.StringUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InsightMailerTest
    extends InjectedTest
{
  private GreenMail smtpServer;

  @Override
  public void configure(Properties properties) {
    Object value = get("mail.hostname");
    if (value == null) {
      value = "localhost";
    }
    properties.put("mail.hostname", value);
    System.setProperty("mail.hostname", value.toString());
    value = get("mail.port");
    if (value == null) {
      value = 3025;
    }
    properties.put("mail.port", value);
    System.setProperty("mail.port", value.toString());
    value = get("mail.systemEmail");
    if (value == null) {
      value = "test@test.com";
    }
    properties.put("mail.systemEmail", value);
    System.setProperty("mail.systemEmail", value.toString());

    super.configure(properties);
  }

  @Before
  public void init() {
    smtpServer = new GreenMail(new ServerSetup(Integer.valueOf(System.getProperty("mail.port")), null,
        ServerSetup.PROTOCOL_SMTP));
    smtpServer.start();
  }

  @After
  public void exit() {
    if (smtpServer != null) {
      smtpServer.stop();
    }
  }

  @Test
  public void sendTestMail() throws Exception {
    InsightMailer mailer = lookup(InsightMailer.class);

    String toAddr = UUID.randomUUID() + "test@sonatype.com";
    String subject = UUID.randomUUID().toString();
    String msg = UUID.randomUUID().toString();
    MailRequest request = mailer.getDefaultMailRequest(subject, msg);
    request.setToAddresses(Arrays.asList(new Address(toAddr)));
    mailer.sendMail(request);
    smtpServer.waitForIncomingEmail(1);
    MimeMessage mail = smtpServer.getReceivedMessages()[0];
    System.out.println(GreenMailUtil.getHeaders(mail));
    assertTrue(GreenMailUtil.getBody(mail).contains(msg));
    assertEquals(toAddr, mail.getRecipients(RecipientType.TO)[0].toString());
  }

  @Test
  public void getEmailConfigurationWithSslAndTlsToPort25() throws Exception {
    System.clearProperty("mail.port");
    MailConfig mailConfig = new MailConfig();
    mailConfig.setSsl(true);
    mailConfig.setTls(true);
    mailConfig.setPort(25);
    EMailer eMailer = mock(EMailer.class);
    ArgumentCaptor<EmailerConfiguration> emailerConfigurationArgumentCaptor = ArgumentCaptor
        .forClass(EmailerConfiguration.class);
    new InsightMailer(eMailer, mailConfig);
    verify(eMailer).configure(emailerConfigurationArgumentCaptor.capture());
    assertThat(emailerConfigurationArgumentCaptor.getValue().getMailPort(), is(25));
  }

  public static String get(String key) {
    String envvar = System.getenv(key.toUpperCase().replace("-", "_"));

    if (!StringUtils.isEmpty(envvar)) {
      return envvar;
    }

    String sysprop = System.getProperty(key);

    if (!StringUtils.isEmpty(sysprop)) {
      return sysprop;
    }

    return null;
  }
}
