/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ApiProxyConfigurationResourceV2AuditTest
    extends AbstractAuditTest
{
  @Before
  public void before() {
    tempEntity.setProxyServerConfiguration("localhost", 80);
  }

  @Test
  public void testUpdate() throws Exception {
    ApiProxyConfigurationDTOV2 proxy = new ApiProxyConfigurationDTOV2(Collections.singletonList("example.com"));
    proxyConfigurationRequest().body(proxy).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROXY, null);
    assertCustomObject(auditDTO, "proxyConfiguration", proxy);
  }

  @Test
  public void testUpdate_Unauthorized() throws Exception {
    proxyConfigurationRequest().body(new ApiProxyConfigurationDTOV2(Collections.singletonList("example.com")))
        .with(unauthorizedUser()).put();

    assertAuditLog(AuditEvent.CONFIGURE_PROXY, "unauthorized");
  }

  private HttpRequest proxyConfigurationRequest() {
    return restRequest().path(PublicApiPaths.PROXY_CONFIG_PATH_V2);
  }
}
