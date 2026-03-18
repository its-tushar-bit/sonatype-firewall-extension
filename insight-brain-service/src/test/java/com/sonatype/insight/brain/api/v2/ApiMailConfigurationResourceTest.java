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
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightMail;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiMailConfigurationResourceTest
    extends AbstractResourceTest
{
  private MailConfigurationDAO mailConfigurationDAO;

  @Before
  public void setUp() {
    mailConfigurationDAO = lookup(MailConfigurationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.MAIL_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void testGetConfiguration() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("resttest");
    mailConfiguration.setPort(58285);
    mailConfiguration.setUsername("smtpuser");
    mailConfiguration.setPassword("smtppass".toCharArray());
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setSystemEmail("nxiq@test");
    mailConfigurationDAO.set(mailConfiguration);

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

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
  public void testSetConfiguration() throws Exception {
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "resttest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "smtppass".toCharArray();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    assertResponseStatus(204, restRequest().body(configurationDTO).put());

    InsightMail insightMail = getCLMServer().getInstance(InsightMail.class);
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
  public void testDeleteConfiguration() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("resttest");
    mailConfiguration.setPort(58285);
    mailConfiguration.setSystemEmail("nxiq@test");
    mailConfigurationDAO.set(mailConfiguration);

    assertResponseStatus(204, restRequest().delete());

    assertThat(mailConfigurationDAO.get()).isNull();
  }

  @Test
  public void testTestConfiguration() throws Exception {
    ApiMailConfigurationDTO configurationDTO = new ApiMailConfigurationDTO();
    configurationDTO.hostname = "resttest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "smtppass".toCharArray();
    configurationDTO.sslEnabled = true;
    configurationDTO.systemEmail = "nxiq@test";

    assertResponseStatus(204,
        restRequest().path(ApiMailConfigurationResource.TEST_CONFIGURATION)
            .parameter("user@test")
            .body(configurationDTO)
            .post());
  }
}
