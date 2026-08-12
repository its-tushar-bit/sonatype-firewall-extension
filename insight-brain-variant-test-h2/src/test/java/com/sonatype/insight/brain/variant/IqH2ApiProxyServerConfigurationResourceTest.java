/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiProxyServerConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiProxyServerConfigurationResourceTest
{
  private IqTestContext ctx;

  private ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  @BeforeEach
  void setUp() {
    proxyServerConfigurationDAO = ctx.lookup(ProxyServerConfigurationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.PROXY_SERVER_CONFIG_PATH_V2);
  }

  @Test
  void testGetConfiguration() throws Exception {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("resttest");
    proxyServerConfiguration.setPort(58285);
    proxyServerConfiguration.setUsername("smtpuser");
    proxyServerConfiguration.setPassword("smtppass".toCharArray());
    proxyServerConfiguration.setExcludeHosts("host1, host2");
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    ApiProxyServerConfigurationDTO configurationDTO = response.getBody(ApiProxyServerConfigurationDTO.class);
    assertThat(configurationDTO.hostname).isEqualTo(proxyServerConfiguration.getHostname());
    assertThat(configurationDTO.port).isEqualTo(proxyServerConfiguration.getPort());
    assertThat(configurationDTO.username).isEqualTo(proxyServerConfiguration.getUsername());
    assertThat(configurationDTO.password).isNull();
    assertThat(configurationDTO.passwordIsIncluded).isFalse();
    assertThat(configurationDTO.excludeHosts).isEqualTo(proxyServerConfiguration.getExcludeHostsList());
  }

  @Test
  void testGetConfiguration_Unlicensed() throws Exception {
    ctx.uninstallLicense();

    ctx.tempEntity().setProxyServerConfiguration("resttest", 58285);

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testSetConfiguration() throws Exception {
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "resttest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "smtppass".toCharArray();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Arrays.asList("host1", "host2");

    ctx.assertResponseStatus(204, restRequest().body(configurationDTO).put());

    PasswordHandler passwordHandler = ctx.lookup(PasswordHandler.class);
    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(passwordHandler.decryptPassword(proxyServerConfiguration.getPassword()))
        .isEqualTo(configurationDTO.password);
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1, host2");
  }

  @Test
  void testSetConfiguration_Unlicensed() throws Exception {
    ctx.uninstallLicense();

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "resttest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "smtppass".toCharArray();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Collections.singletonList("localhost");

    ctx.assertResponseStatus(204, restRequest().body(configurationDTO).put());
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    ctx.tempEntity().setProxyServerConfiguration("resttest", 58285);

    ctx.assertResponseStatus(204, restRequest().delete());

    assertThat(proxyServerConfigurationDAO.get()).isNull();
  }

  @Test
  void testDeleteConfiguration_Unlicensed() throws Exception {
    ctx.uninstallLicense();

    ctx.tempEntity().setProxyServerConfiguration("resttest", 58285);

    ctx.assertResponseStatus(204, restRequest().delete());
  }
}
