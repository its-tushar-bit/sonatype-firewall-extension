/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.mail.InsightMailer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiMailConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiMailConfigurationService mailConfigurationService;

  @Inject
  private MailConfigurationDAO mailConfigurationDAO;

  @Inject
  private InsightMail insightMail;

  @Test
  public void testGetConfiguration() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("servtest");
    mailConfiguration.setPort(58285);
    mailConfiguration.setUsername("smtpuser");
    mailConfiguration.setPassword(insightMail.encryptPassword("smtppass".toCharArray()));
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setSystemEmail("nxiq@test");
    mailConfigurationDAO.set(mailConfiguration);

    ApiMailConfigurationDTO configurationDTO = mailConfigurationService.getConfiguration();
    assertThat(configurationDTO.hostname).isEqualTo(mailConfiguration.getHostname());
    assertThat(configurationDTO.port).isEqualTo(mailConfiguration.getPort());
    assertThat(configurationDTO.username).isEqualTo(mailConfiguration.getUsername());
    assertThat(configurationDTO.password).isNull();
    assertThat(configurationDTO.passwordIsIncluded).isFalse();
    assertThat(configurationDTO.sslEnabled).isEqualTo(mailConfiguration.isSslEnabled());
    assertThat(configurationDTO.startTlsEnabled).isEqualTo(mailConfiguration.isStartTlsEnabled());
    assertThat(configurationDTO.systemEmail).isEqualTo(mailConfiguration.getSystemEmail());
  }

  @Test
  public void testGetConfiguration_NoConfiguration() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      mailConfigurationService.getConfiguration();
    }).withMessageContaining("Mail server not configured");
  }

  @Test
  public void testSetConfiguration_Insert_PasswordNotNull() {
    mailConfigurationDAO.delete();
    char[] password = "smtppass".toCharArray();
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = Arrays.copyOf(password, password.length);
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    mailConfigurationService.setConfiguration(configurationDTO);

    assertThat(configurationDTO.password).containsOnly('0');

    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(mailConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(mailConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(insightMail.decryptPassword(mailConfiguration.getPassword())).isEqualTo(password);
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(configurationDTO.sslEnabled);
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(configurationDTO.startTlsEnabled);
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(configurationDTO.systemEmail);

    InsightMailer insightMailer = insightMail.getInsightMailer();
    assertThat(insightMailer.getHostname()).isEqualTo("servtest");
    assertThat(insightMailer.getPort()).isEqualTo(58285);
    assertThat(insightMailer.getUsername()).isEqualTo("smtpuser");
    assertThat(insightMailer.isSsl()).isTrue();
    assertThat(insightMailer.isTls()).isFalse();
    assertThat(insightMailer.getSystemEmail()).isEqualTo("nxiq@test");
    assertThat(insightMailer.getSystemPersonal()).isEqualTo("Nexus IQ Server");
  }

  @Test
  public void testSetConfiguration_Update_PasswordNotNull() {
    char[] encryptedPassword = insightMail.encryptPassword("smtppass".toCharArray());
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(encryptedPassword);
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    char[] password = "smtppass".toCharArray();
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = Arrays.copyOf(password, password.length);
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    mailConfigurationService.setConfiguration(configurationDTO);

    assertThat(configurationDTO.password).containsOnly('0');

    mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(mailConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(mailConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(insightMail.decryptPassword(mailConfiguration.getPassword())).isEqualTo(password);
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(configurationDTO.sslEnabled);
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(configurationDTO.startTlsEnabled);
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(configurationDTO.systemEmail);

    InsightMailer insightMailer = insightMail.getInsightMailer();
    assertThat(insightMailer.getHostname()).isEqualTo("servtest");
    assertThat(insightMailer.getPort()).isEqualTo(58285);
    assertThat(insightMailer.getUsername()).isEqualTo("smtpuser");
    assertThat(insightMailer.isSsl()).isTrue();
    assertThat(insightMailer.isTls()).isFalse();
    assertThat(insightMailer.getSystemEmail()).isEqualTo("nxiq@test");
    assertThat(insightMailer.getSystemPersonal()).isEqualTo("Nexus IQ Server");
  }

  @Test
  public void testSetConfiguration_Insert_PasswordNull() {
    mailConfigurationDAO.delete();
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = null;
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    mailConfigurationService.setConfiguration(configurationDTO);

    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(mailConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(mailConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(mailConfiguration.getPassword()).isNull();
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(configurationDTO.sslEnabled);
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(configurationDTO.startTlsEnabled);
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(configurationDTO.systemEmail);

    InsightMailer insightMailer = insightMail.getInsightMailer();
    assertThat(insightMailer.getHostname()).isEqualTo("servtest");
    assertThat(insightMailer.getPort()).isEqualTo(58285);
    assertThat(insightMailer.getUsername()).isEqualTo("smtpuser");
    assertThat(insightMailer.isSsl()).isTrue();
    assertThat(insightMailer.isTls()).isFalse();
    assertThat(insightMailer.getSystemEmail()).isEqualTo("nxiq@test");
    assertThat(insightMailer.getSystemPersonal()).isEqualTo("Nexus IQ Server");
  }

  @Test
  public void testSetConfiguration_Insert_PasswordEmpty() {
    mailConfigurationDAO.delete();
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = new char[0];
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    mailConfigurationService.setConfiguration(configurationDTO);

    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(mailConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(mailConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(mailConfiguration.getPassword()).isNull();
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(configurationDTO.sslEnabled);
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(configurationDTO.startTlsEnabled);
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(configurationDTO.systemEmail);

    InsightMailer insightMailer = insightMail.getInsightMailer();
    assertThat(insightMailer.getHostname()).isEqualTo("servtest");
    assertThat(insightMailer.getPort()).isEqualTo(58285);
    assertThat(insightMailer.getUsername()).isEqualTo("smtpuser");
    assertThat(insightMailer.isSsl()).isTrue();
    assertThat(insightMailer.isTls()).isFalse();
    assertThat(insightMailer.getSystemEmail()).isEqualTo("nxiq@test");
    assertThat(insightMailer.getSystemPersonal()).isEqualTo("Nexus IQ Server");
  }

  @Test
  public void testSetConfiguration_Update_PasswordNull() {
    char[] encryptedPassword = insightMail.encryptPassword("smtppass".toCharArray());
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(encryptedPassword);
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = null;
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    mailConfigurationService.setConfiguration(configurationDTO);

    mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(mailConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(mailConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(mailConfiguration.getPassword()).isNull();
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(configurationDTO.sslEnabled);
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(configurationDTO.startTlsEnabled);
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(configurationDTO.systemEmail);

    InsightMailer insightMailer = insightMail.getInsightMailer();
    assertThat(insightMailer.getHostname()).isEqualTo("servtest");
    assertThat(insightMailer.getPort()).isEqualTo(58285);
    assertThat(insightMailer.getUsername()).isEqualTo("smtpuser");
    assertThat(insightMailer.isSsl()).isTrue();
    assertThat(insightMailer.isTls()).isFalse();
    assertThat(insightMailer.getSystemEmail()).isEqualTo("nxiq@test");
    assertThat(insightMailer.getSystemPersonal()).isEqualTo("Nexus IQ Server");
  }

  @Test
  public void testSetConfiguration_Update_PasswordEmpty() {
    char[] encryptedPassword = insightMail.encryptPassword("smtppass".toCharArray());
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(encryptedPassword);
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = new char[0];
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    mailConfigurationService.setConfiguration(configurationDTO);

    mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(mailConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(mailConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(mailConfiguration.getPassword()).isNull();
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(configurationDTO.sslEnabled);
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(configurationDTO.startTlsEnabled);
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(configurationDTO.systemEmail);

    InsightMailer insightMailer = insightMail.getInsightMailer();
    assertThat(insightMailer.getHostname()).isEqualTo("servtest");
    assertThat(insightMailer.getPort()).isEqualTo(58285);
    assertThat(insightMailer.getUsername()).isEqualTo("smtpuser");
    assertThat(insightMailer.isSsl()).isTrue();
    assertThat(insightMailer.isTls()).isFalse();
    assertThat(insightMailer.getSystemEmail()).isEqualTo("nxiq@test");
    assertThat(insightMailer.getSystemPersonal()).isEqualTo("Nexus IQ Server");
  }

  @Test
  public void testSetConfiguration_Update_PasswordNotIncluded_HostnameUnchanged_PortUnchanged() {
    char[] encryptedPassword = insightMail.encryptPassword("smtppass".toCharArray());
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(encryptedPassword);
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = mailConfiguration.getHostname();
    configurationDTO.port = mailConfiguration.getPort();
    configurationDTO.username = "smtpuser";
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    mailConfigurationService.setConfiguration(configurationDTO);

    mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(mailConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(mailConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(mailConfiguration.getPassword()).isEqualTo(encryptedPassword);
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(configurationDTO.sslEnabled);
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(configurationDTO.startTlsEnabled);
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(configurationDTO.systemEmail);
  }

  @Test
  public void testSetConfiguration_Update_PasswordNotIncluded_HostnameChanged_PortUnchanged() {
    char[] encryptedPassword = insightMail.encryptPassword("smtppass".toCharArray());
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(encryptedPassword);
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "othertest";
    configurationDTO.port = mailConfiguration.getPort();
    configurationDTO.username = "smtpuser";
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      mailConfigurationService.setConfiguration(configurationDTO);
    }).withMessageContaining("The password must be provided when the hostname or port are updated");

    // Verify the stored mail configuration was not changed
    MailConfiguration storedMailConfiguration = mailConfigurationDAO.get();
    assertThat(storedMailConfiguration.getHostname()).isEqualTo(mailConfiguration.getHostname());
    assertThat(storedMailConfiguration.getPort()).isEqualTo(mailConfiguration.getPort());
    assertThat(storedMailConfiguration.getUsername()).isEqualTo(mailConfiguration.getUsername());
    assertThat(storedMailConfiguration.getPassword()).isEqualTo(mailConfiguration.getPassword());
    assertThat(storedMailConfiguration.isSslEnabled()).isEqualTo(mailConfiguration.isSslEnabled());
    assertThat(storedMailConfiguration.isStartTlsEnabled()).isEqualTo(mailConfiguration.isStartTlsEnabled());
    assertThat(storedMailConfiguration.getSystemEmail()).isEqualTo(mailConfiguration.getSystemEmail());
  }

  @Test
  public void testSetConfiguration_Update_PasswordNotIncluded_HostnameUnchanged_PortChanged() {
    char[] encryptedPassword = insightMail.encryptPassword("smtppass".toCharArray());
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(encryptedPassword);
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = mailConfiguration.getHostname();
    configurationDTO.port = 2;
    configurationDTO.username = "smtpuser";
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      mailConfigurationService.setConfiguration(configurationDTO);
    }).withMessageContaining("The password must be provided when the hostname or port are updated");

    // Verify the stored mail configuration was not changed
    MailConfiguration storedMailConfiguration = mailConfigurationDAO.get();
    assertThat(storedMailConfiguration.getHostname()).isEqualTo(mailConfiguration.getHostname());
    assertThat(storedMailConfiguration.getPort()).isEqualTo(mailConfiguration.getPort());
    assertThat(storedMailConfiguration.getUsername()).isEqualTo(mailConfiguration.getUsername());
    assertThat(storedMailConfiguration.getPassword()).isEqualTo(mailConfiguration.getPassword());
    assertThat(storedMailConfiguration.isSslEnabled()).isEqualTo(mailConfiguration.isSslEnabled());
    assertThat(storedMailConfiguration.isStartTlsEnabled()).isEqualTo(mailConfiguration.isStartTlsEnabled());
    assertThat(storedMailConfiguration.getSystemEmail()).isEqualTo(mailConfiguration.getSystemEmail());
  }

  @Test
  public void testSetConfiguration_NoRequestDTO() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      mailConfigurationService.setConfiguration(null);
    }).withMessageContaining("No mail server configuration was provided");
  }

  @Test
  public void testDeleteConfiguration() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("servtest");
    mailConfiguration.setPort(58285);
    mailConfiguration.setSystemEmail("nxiq@test");
    mailConfigurationDAO.set(mailConfiguration);
    insightMail.loadMailConfiguration();
    assertThat(insightMail.getInsightMailer()).isNotNull();

    mailConfigurationService.deleteConfiguration();

    assertThat(mailConfigurationDAO.get()).isNull();

    assertThat(insightMail.getInsightMailer()).isNull();
  }

  @Test
  public void testDeleteConfiguration_NoConfiguration() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      mailConfigurationService.deleteConfiguration();
    }).withMessageContaining("Mail server not configured");
  }
}
