/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApiConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  private final SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void testSetConfiguration() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, true);

    HttpResponse response = restRequest().body(properties).put();

    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPERTIES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    assertCustomData(auditDTO, SystemConfigurationProperty.FORCE_BASE_URL, true);
  }

  @Test
  public void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_PROPERTIES, "bad-request");
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    HttpResponse response = restRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.FORCE_BASE_URL).delete();

    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_PROPERTIES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    assertCustomData(auditDTO, SystemConfigurationProperty.FORCE_BASE_URL, true);
  }

  @Test
  public void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.DELETE_PROPERTIES, "bad-request");
  }
}
