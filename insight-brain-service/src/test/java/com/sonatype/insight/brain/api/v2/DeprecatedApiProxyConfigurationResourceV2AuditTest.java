/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.DeprecatedApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.api.v2.service.DeprecatedApiProxyConfigurationServiceV2;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

/**
 * @deprecated The tested class is deprecated.
 */
@Deprecated
public class DeprecatedApiProxyConfigurationResourceV2AuditTest
    extends AbstractAuditTest
{
  @Before
  public void before() {
    tempEntity.setProxyServerConfiguration("localhost", 80);
  }

  @Override
  protected void afterDatabaseReset() {
    getCLMServer().getInstance(DeprecatedApiProxyConfigurationServiceV2.class).applyProxyServerConfigurationToClients();
  }

  @Test
  public void testUpdate() throws Exception {
    DeprecatedApiProxyConfigurationDTOV2 proxy =
        new DeprecatedApiProxyConfigurationDTOV2(Collections.singletonList("example.com"));
    proxyConfigurationRequest().body(proxy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROXY, null);
    assertCustomObject(auditDTO, "proxyConfiguration", proxy);
  }

  @Test
  public void testUpdate_Unauthorized() throws Exception {
    proxyConfigurationRequest().body(new DeprecatedApiProxyConfigurationDTOV2(Collections.singletonList("example.com")))
        .with(unauthorizedUser()).put();

    assertAuditLog(AuditEvent.CONFIGURE_PROXY, "unauthorized");
  }

  private HttpRequest proxyConfigurationRequest() {
    return restRequest().path(PublicApiPaths.DEPRECATED_PROXY_CONFIG_PATH_V2);
  }
}
