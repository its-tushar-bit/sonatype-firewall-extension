/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;

/**
 * Audit tests for ApiScanHealthConfigurationResource.
 * Tests verify that CONFIGURE_SCAN_HEALTH and DELETE_SCAN_HEALTH audit events
 * are properly logged for all scan health configuration operations.
 */
public class ApiScanHealthConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  private ScanHealthConfigDAO scanHealthConfigDAO;

  private Organization testOrg;

  private Application testApp;

  @Before
  public void setUp() {
    scanHealthConfigDAO = lookup(ScanHealthConfigDAO.class);
    testOrg = tempEntity.newOrganization();
    testApp = tempEntity.newApplication(testOrg.getId());
  }

  @Test
  public void testSetConfiguration_Organization() throws Exception {
    ScanHealthConfigDTO dto = new ScanHealthConfigDTO(true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .body(dto)
        .put();

    assertResponseStatus(200, response);
    assertAuditLog(AuditEvent.CONFIGURE_SCAN_HEALTH, null);

    // Cleanup
    scanHealthConfigDAO.delete(ORGANIZATION.toString(), testOrg.getId());
  }

  @Test
  public void testSetConfiguration_Application() throws Exception {
    ScanHealthConfigDTO dto = new ScanHealthConfigDTO(true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, testApp.getId())
        .body(dto)
        .put();

    assertResponseStatus(200, response);
    assertAuditLog(AuditEvent.CONFIGURE_SCAN_HEALTH, null);

    // Cleanup
    scanHealthConfigDAO.delete(APPLICATION.toString(), testApp.getId());
  }

  @Test
  public void testDeleteConfiguration_Organization() throws Exception {
    scanHealthConfigDAO.save(new ScanHealthConfig(
        testOrg.getId(), ORGANIZATION.toString(), "{\"failOnZeroComponents\":true}"));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .delete();

    assertResponseStatus(204, response);
    assertAuditLog(AuditEvent.DELETE_SCAN_HEALTH, null);
  }

  @Test
  public void testDeleteConfiguration_Application() throws Exception {
    scanHealthConfigDAO.save(new ScanHealthConfig(
        testApp.getId(), APPLICATION.toString(), "{\"failOnZeroComponents\":true}"));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, testApp.getId())
        .delete();

    assertResponseStatus(204, response);
    assertAuditLog(AuditEvent.DELETE_SCAN_HEALTH, null);
  }

  @Test
  public void testDeleteConfiguration_NotFound() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .delete();

    assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_SCAN_HEALTH, "not-found");
  }
}
