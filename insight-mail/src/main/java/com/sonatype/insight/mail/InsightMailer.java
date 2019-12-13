/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mail;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.EmailerConfiguration;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.MailRequestStatus;
import org.sonatype.micromailer.imp.DefaultMailType;

import org.apache.commons.lang.StringUtils;

public class InsightMailer
{
  private final EMailer mailer;

  private final String hostname;

  private final int port;

  private final boolean disableSend;

  private final boolean debug;

  private final boolean ssl;

  private final boolean tls;

  private final String username;

  private final String password;

  private final String systemPersonal;

  private final String systemEmail;

  public InsightMailer(EMailer mailer, MailConfig mailConfig) {
    if (System.getProperty("mail.host", "").isEmpty()) {
      // avoid DNS delays/issues in javax.mail.internet.InternetAddress.getLocalAddress()
      System.setProperty("mail.host", "localhost");
    }
    this.mailer = mailer;
    this.hostname = mailConfig.getHostname();
    this.port = mailConfig.getPort();
    this.disableSend = mailConfig.isDisableSend();
    this.debug = mailConfig.isDebug();
    this.ssl = mailConfig.isSsl();
    this.tls = mailConfig.isTls();
    this.username = mailConfig.getUsername();
    this.password = mailConfig.getPassword();
    this.systemEmail = mailConfig.getSystemEmail();
    this.systemPersonal = mailConfig.getSystemPersonal();
    this.mailer.configure(getEmailConfiguration());
  }

  private EmailerConfiguration getEmailConfiguration() {
    EmailerConfiguration config = new EmailerConfiguration();
    config.setMailHost(getHostname());
    config.setSendMails(!isDisableSend());
    config.setDebug(isDebug());
    // CLM-8443 SSL must be set before the port else it may override the port (i.e. if true it changes port 25 to 465)
    config.setSsl(isSsl());
    config.setMailPort(getPort());
    config.setTls(isTls());
    config.setUsername(getUsername());
    config.setPassword(getPassword());
    return config;
  }

  public String getHostname() {
    String hostname = get("mail.hostname");

    if (hostname != null) {
      return hostname;
    }

    return this.hostname;
  }

  public EMailer getMailer() {
    return mailer;
  }

  public int getPort() {
    String port = get("mail.port");

    if (port != null) {
      return Integer.valueOf(port);
    }

    return this.port;
  }

  public boolean isDisableSend() {
    return disableSend;
  }

  public boolean isDebug() {
    return debug;
  }

  public boolean isSsl() {
    return ssl;
  }

  public boolean isTls() {
    return tls;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getSystemPersonal() {
    return StringUtils.isEmpty(systemPersonal) ? "Nexus IQ Server" : systemPersonal;
  }

  public String getSystemEmail() {
    return StringUtils.isEmpty(systemEmail) ? "NexusIQServer@localhost" : systemEmail;
  }

  public String getDefaultMailTypeId() {
    return DefaultMailType.DEFAULT_TYPE_ID;
  }

  public String getMailId() {
    StringBuilder sb = new StringBuilder("INSIGHT");

    sb.append(String.valueOf(System.currentTimeMillis()));

    return sb.toString();
  }

  public MailRequest getDefaultMailRequest(String subject, String body) {
    MailRequest request = new MailRequest(getMailId(), getDefaultMailTypeId());
    request.setFrom(getSystemAddress());
    request.getBodyContext().put(DefaultMailType.SUBJECT_KEY, subject);
    request.getBodyContext().put(DefaultMailType.BODY_KEY, body);
    return request;
  }

  public MailRequestStatus sendMail(MailRequest request) {
    if (request.getFrom() == null) {
      request.setFrom(getSystemAddress());
    }

    return mailer.sendMail(request);
  }

  private Address getSystemAddress() {
    return new Address(getSystemEmail(), getSystemPersonal());
  }

  private String get(String key) {
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
