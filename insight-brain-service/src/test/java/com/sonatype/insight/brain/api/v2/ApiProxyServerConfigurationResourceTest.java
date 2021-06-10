/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiProxyServerConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiProxyServerConfigurationResourceTest
    extends AbstractResourceTest
{
  private final ProxyServerConfigurationDAO proxyServerConfigurationDAO = new ProxyServerConfigurationDAO();

  @Override
  protected void afterDatabaseReset() {
    getCLMServer().getInstance(ApiProxyServerConfigurationService.class).applyProxyServerConfigurationToClients();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.PROXY_SERVER_CONFIG_PATH_V2);
  }

  @Test
  public void testGetConfiguration() throws Exception {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("resttest");
    proxyServerConfiguration.setPort(58285);
    proxyServerConfiguration.setUsername("smtpuser");
    proxyServerConfiguration.setPassword("smtppass".toCharArray());
    proxyServerConfiguration.setExcludeHosts("host1, host2");
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    ApiProxyServerConfigurationDTO configurationDTO = response.getBody(ApiProxyServerConfigurationDTO.class);
    assertThat(configurationDTO.hostname).isEqualTo(proxyServerConfiguration.getHostname());
    assertThat(configurationDTO.port).isEqualTo(proxyServerConfiguration.getPort());
    assertThat(configurationDTO.username).isEqualTo(proxyServerConfiguration.getUsername());
    assertThat(configurationDTO.password).isNull();
    assertThat(configurationDTO.passwordIsIncluded).isFalse();
    assertThat(configurationDTO.excludeHosts).isEqualTo(proxyServerConfiguration.getExcludeHostsList());
  }

  @Test
  public void testGetConfiguration_Unlicensed() throws Exception {
    uninstallLicense();

    tempEntity.setProxyServerConfiguration("resttest", 58285);

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testSetConfiguration() throws Exception {
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "resttest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "smtppass".toCharArray();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Arrays.asList("host1", "host2");

    assertResponseStatus(204, restRequest().body(configurationDTO).put());

    PasswordHandler passwordHandler = getCLMServer().getInstance(PasswordHandler.class);
    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(passwordHandler.decryptPassword(proxyServerConfiguration.getPassword()))
        .isEqualTo(configurationDTO.password);
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1, host2");
  }

  @Test
  public void testSetConfiguration_Unlicensed() throws Exception {
    uninstallLicense();

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "resttest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "smtppass".toCharArray();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Collections.singletonList("localhost");

    assertResponseStatus(204, restRequest().body(configurationDTO).put());
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    tempEntity.setProxyServerConfiguration("resttest", 58285);

    assertResponseStatus(204, restRequest().delete());

    assertThat(proxyServerConfigurationDAO.get()).isNull();
  }

  @Test
  public void testDeleteConfiguration_Unlicensed() throws Exception {
    uninstallLicense();

    tempEntity.setProxyServerConfiguration("resttest", 58285);

    assertResponseStatus(204, restRequest().delete());
  }
}
