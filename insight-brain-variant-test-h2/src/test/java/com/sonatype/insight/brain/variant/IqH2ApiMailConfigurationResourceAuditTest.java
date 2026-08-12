/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiMailConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private MailConfigurationDAO mailConfigurationDAO;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    mailConfigurationDAO = ctx.lookup(MailConfigurationDAO.class);
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public com.sonatype.insight.brain.dataaccess.policy.PolicyDAO getPolicyDAO() {
    return ctx.lookup(com.sonatype.insight.brain.dataaccess.policy.PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.MAIL_CONFIG_RESOURCE_PATH_V2);
  }

  private void assertAuditData(
      AuditDTO auditDTO,
      String hostname,
      int port,
      String username,
      char[] password,
      boolean sslEnabled,
      boolean startTlsEnabled,
      String systemEmail)
  {
    assertCustomData(auditDTO, "smtpHostname", hostname);
    assertCustomData(auditDTO, "smtpPort", port);
    assertCustomData(auditDTO, "smtpUsername", username);
    assertThat(auditDTO.data).doesNotContainValue(password);
    assertThat(auditDTO.data).doesNotContainValue(String.valueOf(password));
    assertCustomData(auditDTO, "smtpSsl", sslEnabled ? "enabled" : "disabled");
    assertCustomData(auditDTO, "smtpStartTls", startTlsEnabled ? "enabled" : "disabled");
    assertCustomData(auditDTO, "smtpSystemEmail", systemEmail);
  }

  @Test
  void testSetConfiguration() throws Exception {
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "audittest";
    configurationDTO.port = 58285;
    configurationDTO.username = "audituser";
    configurationDTO.password = "auditpass".toCharArray();
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "audit@test";

    restRequest().body(configurationDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_MAIL, null);
    assertAuditData(auditDTO, configurationDTO.hostname, configurationDTO.port, configurationDTO.username,
        configurationDTO.password, configurationDTO.sslEnabled, configurationDTO.startTlsEnabled,
        configurationDTO.systemEmail);
  }

  @Test
  void testSetConfiguration_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(new ApiMailConfigurationDTO()).put();

    assertAuditLog(AuditEvent.CONFIGURE_MAIL, "unauthorized");
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("audittest");
    mailConfiguration.setPort(58285);
    mailConfiguration.setUsername("audituser");
    mailConfiguration.setPassword("auditpass".toCharArray());
    mailConfiguration.setSystemEmail("audit@test");
    mailConfigurationDAO.set(mailConfiguration);

    restRequest().delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_MAIL, null);
    assertAuditData(auditDTO, mailConfiguration.getHostname(), mailConfiguration.getPort(),
        mailConfiguration.getUsername(), mailConfiguration.getPassword(), mailConfiguration.isSslEnabled(),
        mailConfiguration.isStartTlsEnabled(), mailConfiguration.getSystemEmail());
  }

  @Test
  void testDeleteConfiguration_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_MAIL, "unauthorized");
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
