/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.test.MailboxTestUtil;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiMailConfigurationResourceTest
{
  private IqTestContext ctx;

  private MailConfigurationDAO mailConfigurationDAO;

  @BeforeEach
  void setUp() {
    mailConfigurationDAO = ctx.lookup(MailConfigurationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.MAIL_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testGetConfiguration() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("resttest");
    mailConfiguration.setPort(58285);
    mailConfiguration.setUsername("smtpuser");
    mailConfiguration.setPassword("smtppass".toCharArray());
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setSystemEmail("nxiq@test");
    mailConfigurationDAO.set(mailConfiguration);

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    ApiMailConfigurationDTO configurationDTO = response.getBody(ApiMailConfigurationDTO.class);
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
  void testSetConfiguration() throws Exception {
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "resttest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "smtppass".toCharArray();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    ctx.assertResponseStatus(204, restRequest().body(configurationDTO).put());

    InsightMail insightMail = ctx.lookup(InsightMail.class);
    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(mailConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(mailConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(insightMail.decryptPassword(mailConfiguration.getPassword())).isEqualTo(configurationDTO.password);
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(configurationDTO.sslEnabled);
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(configurationDTO.startTlsEnabled);
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(configurationDTO.systemEmail);
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("resttest");
    mailConfiguration.setPort(58285);
    mailConfiguration.setSystemEmail("nxiq@test");
    mailConfigurationDAO.set(mailConfiguration);

    ctx.assertResponseStatus(204, restRequest().delete());

    assertThat(mailConfigurationDAO.get()).isNull();
  }

  @Test
  void testTestConfiguration() throws Exception {
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "resttest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "smtppass".toCharArray();
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    MailboxTestUtil.clearAll();

    ctx.assertResponseStatus(204,
        restRequest().path(ApiMailConfigurationResource.TEST_CONFIGURATION)
            .parameter("user@test")
            .body(configurationDTO)
            .post());

    assertThat(MailboxTestUtil.get("user@test")).hasSize(1);
  }
}
