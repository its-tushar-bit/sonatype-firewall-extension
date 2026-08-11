/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * IQ Server on PostgreSQL — authorization tests for ApiScanHealthConfigurationResource.
 * Tests verify that proper permissions (READ for GET, WRITE for PUT/DELETE) are required
 * for all Scan Health configuration operations.
 */
@IqPostgresTest
class IqPostgresApiScanHealthConfigurationResourceAuthzTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private ScanHealthConfigDAO scanHealthConfigDAO;

  private Organization org;

  private User unauthorized;

  @BeforeEach
  void setUp() {
    scanHealthConfigDAO = ctx.lookup(ScanHealthConfigDAO.class);
    org = ctx.tempEntity().newOrganization();
    unauthorized = ctx.tempEntity().newUser();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon();
  }

  @Test
  void testGetConfiguration_Organization_AuthorizedUser() throws Exception {
    saveConfig(org.getId(), ORGANIZATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testGetConfiguration_Application_AuthorizedUser() throws Exception {
    Application app = ctx.tempEntity().newApplication(org.getPublicId());
    saveConfig(app.getId(), APPLICATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .auth()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testGetConfiguration_Organization_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, "anyId")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testGetConfiguration_Application_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, "anyId")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testSetConfiguration_Organization_AuthorizedUser() throws Exception {
    ScanHealthConfigDTO config = new ScanHealthConfigDTO(true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth()
        .body(config)
        .put();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testSetConfiguration_Application_AuthorizedUser() throws Exception {
    Application app = ctx.tempEntity().newApplication(org.getPublicId());
    ScanHealthConfigDTO config = new ScanHealthConfigDTO(true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .auth()
        .body(config)
        .put();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testSetConfiguration_Organization_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, "anyId")
        .anon()
        .body(new ScanHealthConfigDTO())
        .put();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testSetConfiguration_Application_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, "anyId")
        .anon()
        .body(new ScanHealthConfigDTO())
        .put();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testDeleteConfiguration_Organization_AuthorizedUser() throws Exception {
    saveConfig(org.getId(), ORGANIZATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth()
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testDeleteConfiguration_Application_AuthorizedUser() throws Exception {
    Application app = ctx.tempEntity().newApplication(org.getPublicId());
    saveConfig(app.getId(), APPLICATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .auth()
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  void testDeleteConfiguration_Organization_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, "anyId")
        .anon()
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testDeleteConfiguration_Application_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, "anyId")
        .anon()
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testGetConfiguration_Organization_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth(unauthorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testSetConfiguration_Organization_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth(unauthorized)
        .body(new ScanHealthConfigDTO(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testDeleteConfiguration_Organization_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth(unauthorized)
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  private void saveConfig(final String ownerId, final OwnerType ownerType, final Boolean failOnZeroComponents) {
    String json = "{\"failOnZeroComponents\":" + failOnZeroComponents + "}";
    ScanHealthConfig config = new ScanHealthConfig(ownerId, ownerType.toString(), json);
    scanHealthConfigDAO.save(config);
  }
}
