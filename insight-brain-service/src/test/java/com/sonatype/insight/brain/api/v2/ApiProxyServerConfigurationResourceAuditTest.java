/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiProxyServerConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiProxyServerConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  private ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  @Before
  public void setUp() {
    proxyServerConfigurationDAO = lookup(ProxyServerConfigurationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.PROXY_SERVER_CONFIG_PATH_V2);
  }

  private void assertAuditData(
      AuditDTO auditDTO,
      String hostname,
      int port,
      String username,
      char[] password,
      List<String> excludeHosts)
  {
    assertCustomData(auditDTO, "proxyServerHostname", hostname);
    assertCustomData(auditDTO, "proxyServerPort", port);
    assertCustomData(auditDTO, "proxyServerUsername", username);
    assertThat(auditDTO.data).doesNotContainValue(password);
    assertThat(auditDTO.data).doesNotContainValue(String.valueOf(password));
    assertCustomData(auditDTO, "proxyServerExcludeHosts", excludeHosts);
  }

  @Test
  public void testSetConfiguration() throws Exception {
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "audittest";
    configurationDTO.port = 58285;
    configurationDTO.username = "audituser";
    configurationDTO.password = "auditpass".toCharArray();
    configurationDTO.excludeHosts = Arrays.asList("host1", "host2");

    restRequest().body(configurationDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROXY, null);
    assertAuditData(auditDTO, configurationDTO.hostname, configurationDTO.port, configurationDTO.username,
        configurationDTO.password, configurationDTO.excludeHosts);
  }

  @Test
  public void testSetConfiguration_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(new ApiProxyServerConfigurationDTO()).put();

    assertAuditLog(AuditEvent.CONFIGURE_PROXY, "unauthorized");
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    ProxyServerConfiguration configuration = new ProxyServerConfiguration();
    configuration.setHostname("audittest");
    configuration.setPort(58285);
    configuration.setUsername("audituser");
    configuration.setPassword("auditpass".toCharArray());
    configuration.setExcludeHosts("host1, host2");
    proxyServerConfigurationDAO.set(configuration);

    restRequest().delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_PROXY, null);
    assertAuditData(auditDTO, configuration.getHostname(), configuration.getPort(), configuration.getUsername(),
        configuration.getPassword(), configuration.getExcludeHostsList());
  }

  @Test
  public void testDeleteConfiguration_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_PROXY, "unauthorized");
  }
}
