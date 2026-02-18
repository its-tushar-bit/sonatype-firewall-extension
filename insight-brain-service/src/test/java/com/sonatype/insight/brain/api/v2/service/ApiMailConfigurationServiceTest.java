/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import com.sonatype.insight.brain.test.MailboxTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiMailConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiMailConfigurationService mailConfigurationService;

  @Inject
  private MailConfigurationDAO mailConfigurationDAO;

  @Inject
  private InsightMail insightMail;

  @Inject
  private BaseUrl baseUrl;

  @Before
  public void before() {
    // Always start with baseUrl unconfigured
    resetBaseUrl();
    // Sanity check
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> baseUrl.getConfigured())
        .withMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

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
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> mailConfigurationService.getConfiguration())
        .withMessageContaining("Mail server not configured");
  }

  @Test
  public void testSetConfiguration_Insert_PasswordNotNull() {
    mailConfigurationDAO.delete();
    char[] password = "smtppass".toCharArray();
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = password.clone();
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
    configurationDTO.password = password.clone();
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
    configurationDTO.password = "mysecret".toCharArray();
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    mailConfigurationService.setConfiguration(configurationDTO);

    assertThat(configurationDTO.password).containsOnly('0');

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
    configurationDTO.password = "mysecret".toCharArray();
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> mailConfigurationService.setConfiguration(configurationDTO))
        .withMessageContaining("The password must be provided when the hostname or port are updated");

    assertThat(configurationDTO.password).containsOnly('0');

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
    configurationDTO.password = "mysecret".toCharArray();
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> mailConfigurationService.setConfiguration(configurationDTO))
            .withMessageContaining("The password must be provided when the hostname or port are updated");

    assertThat(configurationDTO.password).containsOnly('0');

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
  public void testSetConfiguration_NoConfigurationDTO() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> mailConfigurationService.setConfiguration(null))
        .withMessageContaining("No mail server configuration was provided");
  }

  @Test
  public void testDeleteConfiguration() {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("servtest");
    mailConfiguration.setPort(58285);
    mailConfiguration.setSystemEmail("nxiq@test");
    mailConfigurationDAO.set(mailConfiguration);

    mailConfigurationService.deleteConfiguration();

    assertThat(mailConfigurationDAO.get()).isNull();
  }

  @Test
  public void testDeleteConfiguration_NoConfiguration() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> mailConfigurationService.deleteConfiguration())
        .withMessageContaining("Mail server not configured");
  }

  @Test
  public void testTestConfiguration_DoesNotChangeStoredMailConfiguration() throws Exception {
    char[] encryptedPassword = insightMail.encryptPassword("testpass".toCharArray());
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(encryptedPassword);
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    MailboxTestUtil.clearAll();
    setBaseUrl("http://localhost");

    char[] password = "smtppass".toCharArray();
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "smtpserver";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = password.clone();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.sslEnabled = false;
    configurationDTO.startTlsEnabled = false;
    configurationDTO.systemEmail = "noreply@localhost";
    String recipientEmail = "test@example.com";
    mailConfigurationService.testConfiguration(recipientEmail, configurationDTO);

    assertThat(configurationDTO.password).containsOnly('0');

    assertTestConfigurationEmail(recipientEmail, configurationDTO, password);

    mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getHostname()).isEqualTo("test");
    assertThat(mailConfiguration.getPort()).isEqualTo(1);
    assertThat(mailConfiguration.getUsername()).isEqualTo("testuser");
    assertThat(mailConfiguration.getPassword()).isEqualTo(encryptedPassword);
    assertThat(mailConfiguration.isSslEnabled()).isTrue();
    assertThat(mailConfiguration.isStartTlsEnabled()).isTrue();
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo("void@test");
  }

  @Test
  public void testTestConfiguration_PasswordNotIncluded_HostnameChanged_PortUnchanged() {
    char[] password = "smtppass".toCharArray();
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(insightMail.encryptPassword(password));
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    MailboxTestUtil.clearAll();
    setBaseUrl("http://localhost");

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "otherhost";
    configurationDTO.port = mailConfiguration.getPort();
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "foo".toCharArray();
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.systemEmail = "noreply@localhost";
    String recipientEmail = "test@example.com";
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> mailConfigurationService.testConfiguration(recipientEmail, configurationDTO))
        .withMessageContaining("The password must be provided when the hostname or port are updated");

    assertThat(configurationDTO.password).containsOnly('0');
  }

  @Test
  public void testTestConfiguration_PasswordNotIncluded_HostnameUnchanged_PortChanged() {
    char[] password = "smtppass".toCharArray();
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(insightMail.encryptPassword(password));
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    MailboxTestUtil.clearAll();
    setBaseUrl("http://localhost");

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = mailConfiguration.getHostname();
    configurationDTO.port = 234;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "foo".toCharArray();
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.systemEmail = "noreply@localhost";
    String recipientEmail = "test@example.com";
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> mailConfigurationService.testConfiguration(recipientEmail, configurationDTO))
        .withMessageContaining("The password must be provided when the hostname or port are updated");

    assertThat(configurationDTO.password).containsOnly('0');
  }

  @Test
  public void testTestConfiguration_PasswordNotIncluded_HostnameUnchanged_PortUnchanged() throws Exception {
    char[] password = "smtppass".toCharArray();
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("test");
    mailConfiguration.setPort(1);
    mailConfiguration.setUsername("testuser");
    mailConfiguration.setPassword(insightMail.encryptPassword(password));
    mailConfiguration.setSystemEmail("void@test");
    mailConfigurationDAO.set(mailConfiguration);

    MailboxTestUtil.clearAll();
    setBaseUrl("http://localhost");

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = mailConfiguration.getHostname();
    configurationDTO.port = mailConfiguration.getPort();
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "foo".toCharArray();
    configurationDTO.passwordIsIncluded = false;
    configurationDTO.systemEmail = "noreply@localhost";
    String recipientEmail = "test@example.com";

    mailConfigurationService.testConfiguration(recipientEmail, configurationDTO);

    assertThat(configurationDTO.password).containsOnly('0');

    assertTestConfigurationEmail(recipientEmail, configurationDTO, password);
  }

  @Test
  public void testTestConfiguration_PasswordNotNull() throws Exception {
    MailboxTestUtil.clearAll();
    setBaseUrl("http://localhost");

    char[] password = "smtppass".toCharArray();
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "smtpserver";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = password.clone();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.systemEmail = "noreply@localhost";
    String recipientEmail = "test@example.com";
    mailConfigurationService.testConfiguration(recipientEmail, configurationDTO);

    assertThat(configurationDTO.password).containsOnly('0');

    assertTestConfigurationEmail(recipientEmail, configurationDTO, password);
  }

  @Test
  public void testTestConfiguration_PasswordNull() throws Exception {
    MailboxTestUtil.clearAll();
    setBaseUrl("http://localhost");

    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "smtpserver";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = null;
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.systemEmail = "noreply@localhost";
    String recipientEmail = "test@example.com";
    mailConfigurationService.testConfiguration(recipientEmail, configurationDTO);

    assertTestConfigurationEmail(recipientEmail, configurationDTO, null /* password */);
  }

  @Test
  public void testTestConfiguration_BadConfigurationDTO() {
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = null;
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "smtppass".toCharArray();
    configurationDTO.systemEmail = "noreply@localhost";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> mailConfigurationService.testConfiguration("test@example.com", configurationDTO))
        .withMessage("The SMTP host is required.");
    assertThat(configurationDTO.password).containsOnly('0');
  }

  @Test
  public void testTestConfiguration_NoBaseUrl() {
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "smtpserver";
    configurationDTO.port = 58285;
    configurationDTO.systemEmail = "noreply@localhost";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> mailConfigurationService.testConfiguration("test@example.com", configurationDTO))
        .withMessageContainingAll("Test mail configuration failed. Error ID",
            BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  @Test
  public void testTestConfiguration_NoConfigurationDTO() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> mailConfigurationService.testConfiguration("test@example.com", null))
        .withMessage("No mail server configuration was provided.");
  }

  // CLM-38607: Guard tests for multi-tenant access control

  private ApiMailConfigurationService createMultiTenantService() {
    TenantUtil mockTenantUtil = mock(TenantUtil.class);
    when(mockTenantUtil.isSingleTenant()).thenReturn(false);
    when(mockTenantUtil.isGlobalTenant()).thenReturn(false);
    return new ApiMailConfigurationService(mailConfigurationDAO, insightMail, baseUrl, mockTenantUtil);
  }

  private ApiMailConfigurationService createGlobalTenantService() {
    TenantUtil mockTenantUtil = mock(TenantUtil.class);
    when(mockTenantUtil.isSingleTenant()).thenReturn(false);
    when(mockTenantUtil.isGlobalTenant()).thenReturn(true);
    return new ApiMailConfigurationService(mailConfigurationDAO, insightMail, baseUrl, mockTenantUtil);
  }

  @Test
  public void testGetConfiguration_blockedForMtiqTenantWithoutConfig() {
    ApiMailConfigurationService mtiqService = createMultiTenantService();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(mtiqService::getConfiguration)
        .withMessageContaining("not available for this tenant");
  }

  @Test
  public void testSetConfiguration_blockedForMtiqTenantWithoutConfig() {
    ApiMailConfigurationService mtiqService = createMultiTenantService();

    ApiMailConfigurationDTO dto = new ApiMailConfigurationDTO();
    dto.hostname = "smtp.evil.com";
    dto.port = 587;
    dto.passwordIsIncluded = true;
    dto.password = "pass".toCharArray();
    dto.systemEmail = "test@evil.com";

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> mtiqService.setConfiguration(dto))
        .withMessageContaining("not available for this tenant");
  }

  @Test
  public void testDeleteConfiguration_blockedForMtiqTenantWithoutConfig() {
    ApiMailConfigurationService mtiqService = createMultiTenantService();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(mtiqService::deleteConfiguration)
        .withMessageContaining("not available for this tenant");
  }

  @Test
  public void testTestConfiguration_blockedForMtiqTenantWithoutConfig() {
    ApiMailConfigurationService mtiqService = createMultiTenantService();

    ApiMailConfigurationDTO dto = new ApiMailConfigurationDTO();
    dto.hostname = "smtp.evil.com";
    dto.port = 587;
    dto.passwordIsIncluded = true;
    dto.password = "pass".toCharArray();
    dto.systemEmail = "test@evil.com";

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> mtiqService.testConfiguration("test@example.com", dto))
        .withMessageContaining("not available for this tenant");
  }

  @Test
  public void testSetConfiguration_allowedForMtiqTenantWithExistingConfig() {
    MailConfiguration existingConfig = new MailConfiguration();
    existingConfig.setHostname("smtp.existing.com");
    existingConfig.setPort(587);
    existingConfig.setSystemEmail("existing@test.com");
    mailConfigurationDAO.set(existingConfig);

    ApiMailConfigurationService mtiqService = createMultiTenantService();

    ApiMailConfigurationDTO dto = new ApiMailConfigurationDTO();
    dto.hostname = "smtp.existing.com";
    dto.port = 587;
    dto.username = "user";
    dto.password = "pass".toCharArray();
    dto.passwordIsIncluded = true;
    dto.systemEmail = "updated@test.com";

    mtiqService.setConfiguration(dto);

    MailConfiguration updated = mailConfigurationDAO.get();
    assertThat(updated.getSystemEmail()).isEqualTo("updated@test.com");
  }

  @Test
  public void testGetConfiguration_allowedForMtiqTenantWithExistingConfig() {
    MailConfiguration existingConfig = new MailConfiguration();
    existingConfig.setHostname("smtp.existing.com");
    existingConfig.setPort(587);
    existingConfig.setSystemEmail("existing@test.com");
    mailConfigurationDAO.set(existingConfig);

    ApiMailConfigurationService mtiqService = createMultiTenantService();

    ApiMailConfigurationDTO result = mtiqService.getConfiguration();

    assertThat(result.hostname).isEqualTo("smtp.existing.com");
  }

  @Test
  public void testDeleteConfiguration_allowedForMtiqTenantWithExistingConfig() {
    MailConfiguration existingConfig = new MailConfiguration();
    existingConfig.setHostname("smtp.existing.com");
    existingConfig.setPort(587);
    existingConfig.setSystemEmail("existing@test.com");
    mailConfigurationDAO.set(existingConfig);

    ApiMailConfigurationService mtiqService = createMultiTenantService();

    mtiqService.deleteConfiguration();

    assertThat(mailConfigurationDAO.getWithoutFallback()).isNull();
  }

  @Test
  public void testTestConfiguration_allowedForMtiqTenantWithExistingConfig() throws Exception {
    MailConfiguration existingConfig = new MailConfiguration();
    existingConfig.setHostname("smtp.existing.com");
    existingConfig.setPort(587);
    existingConfig.setSystemEmail("existing@test.com");
    mailConfigurationDAO.set(existingConfig);

    MailboxTestUtil.clearAll();
    setBaseUrl("http://localhost");

    ApiMailConfigurationService mtiqService = createMultiTenantService();

    ApiMailConfigurationDTO dto = new ApiMailConfigurationDTO();
    dto.hostname = "smtp.existing.com";
    dto.port = 587;
    dto.passwordIsIncluded = true;
    dto.password = "pass".toCharArray();
    dto.systemEmail = "existing@test.com";

    mtiqService.testConfiguration("test@example.com", dto);
  }

  @Test
  public void testSetConfiguration_allowedForGlobalTenantWithoutConfig() {
    mailConfigurationDAO.delete();
    ApiMailConfigurationService globalService = createGlobalTenantService();

    ApiMailConfigurationDTO dto = new ApiMailConfigurationDTO();
    dto.hostname = "smtp.sendgrid.com";
    dto.port = 587;
    dto.username = "user";
    dto.password = "pass".toCharArray();
    dto.passwordIsIncluded = true;
    dto.systemEmail = "noreply@sonatype.com";

    globalService.setConfiguration(dto);

    MailConfiguration saved = mailConfigurationDAO.get();
    assertThat(saved.getHostname()).isEqualTo("smtp.sendgrid.com");
  }

  @Test
  public void testDeleteConfiguration_usesGetWithoutFallback() {
    mailConfigurationDAO.delete();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> mailConfigurationService.deleteConfiguration())
        .withMessageContaining("Mail server not configured");
  }

  private void assertTestConfigurationEmail(
      String toEmailAddress,
      ApiMailConfigurationDTO configurationDTO,
      char[] password) throws MessagingException, IOException
  {
    List<Message> emails = MailboxTestUtil.get(toEmailAddress);

    assertThat(emails).hasSize(1);
    Message email = emails.get(0);

    // Assert mail server
    Session session = email.getSession();
    assertThat(session.getProperties()) //
        .containsEntry("mail.smtp.host", configurationDTO.hostname)
        .containsEntry("mail.smtp.port", String.valueOf(configurationDTO.port))
        .containsEntry("mail.smtp.starttls.enable", String.valueOf(configurationDTO.startTlsEnabled));

    // Assert authentication
    PasswordAuthentication passwordAuthentication = session.requestPasswordAuthentication(null, 0, null, null, null);
    if (configurationDTO.username == null) {
      assertThat(passwordAuthentication).isNull();
    }
    else {
      assertThat(passwordAuthentication.getUserName()).isEqualTo(configurationDTO.username);
      if (password == null) {
        assertThat(passwordAuthentication.getPassword()).isNull();
      }
      else {
        assertThat(passwordAuthentication.getPassword()).isEqualTo(String.valueOf(password));
      }
    }

    // Assert "to" and "from" addresses
    Address[] recipients = email.getRecipients(RecipientType.TO);
    assertThat(recipients).hasSize(1);
    assertThat(recipients[0].toString()).isEqualTo(toEmailAddress);
    assertThat(email.getFrom()[0].toString()).isEqualTo("Nexus IQ Server <" + configurationDTO.systemEmail + ">");

    // Assert email subject and body
    assertThat(email.getSubject()).isEqualTo("Test Email Configuration");
    String emailBody = IOUtils.toString(email.getInputStream(), StandardCharsets.UTF_8);
    assertThat(emailBody).contains("Success! This is a test mail from http://localhost");
  }
}
