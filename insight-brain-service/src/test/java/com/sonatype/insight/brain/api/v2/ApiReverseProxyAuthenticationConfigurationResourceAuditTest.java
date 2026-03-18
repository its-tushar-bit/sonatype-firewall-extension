/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReverseProxyAuthenticationConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApiReverseProxyAuthenticationConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.REVERSE_PROXY_AUTHENTICATION_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void testSetConfiguration() throws Exception {
    ApiReverseProxyAuthenticationConfigurationDTO dto = new ApiReverseProxyAuthenticationConfigurationDTO();
    dto.enabled = true;
    dto.usernameHeader = "usernameHeader";
    dto.csrfProtectionDisabled = true;
    dto.logoutUrl = "logoutUrl";

    HttpResponse response = restRequest().body(dto).put();

    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REVERSE_PROXY_AUTHENTICATION, null);
    assertAuditData(auditDTO, dto.enabled, dto.usernameHeader, dto.csrfProtectionDisabled, dto.logoutUrl);
  }

  @Test
  public void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_REVERSE_PROXY_AUTHENTICATION, "bad-request");
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    ReverseProxyAuthenticationConfiguration config = tempEntity.newReverseProxyAuthenticationConfiguration();

    HttpResponse response = restRequest().delete();

    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REVERSE_PROXY_AUTHENTICATION, null);
    assertAuditData(auditDTO, config.isEnabled(), config.getUsernameHeader(), config.isCsrfProtectionDisabled(),
        config.getLogoutUrl());
  }

  @Test
  public void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_REVERSE_PROXY_AUTHENTICATION, "not-found");
  }

  private void assertAuditData(
      AuditDTO auditDTO,
      boolean enabled,
      String usernameHeader,
      boolean csrfProtectionDisabled,
      String logoutUrl)
  {
    assertCustomData(auditDTO, "enabled", enabled);
    assertCustomData(auditDTO, "usernameHeader", usernameHeader);
    assertCustomData(auditDTO, "csrfProtectionDisabled", csrfProtectionDisabled);
    assertCustomData(auditDTO, "logoutUrl", logoutUrl);
  }
}
