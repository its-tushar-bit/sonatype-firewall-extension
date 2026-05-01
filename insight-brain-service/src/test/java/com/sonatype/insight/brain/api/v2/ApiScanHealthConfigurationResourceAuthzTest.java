/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for ApiScanHealthConfigurationResource.
 * Tests verify that proper permissions (READ for GET, WRITE for PUT/DELETE) are required
 * for all Scan Health configuration operations.
 *
 * @since 1.209
 */
public class ApiScanHealthConfigurationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private ScanHealthConfigDAO scanHealthConfigDAO;

  @Before
  public void setUp() {
    scanHealthConfigDAO = lookup(ScanHealthConfigDAO.class);
  }

  @Test
  public void testGetConfiguration_Organization_AuthorizedUser() throws Exception {
    Organization org = tempEntity.newOrganization();
    saveConfig(org.getId(), ORGANIZATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testGetConfiguration_Application_AuthorizedUser() throws Exception {
    Application app = tempEntity.newApplication(org.getPublicId());
    saveConfig(app.getId(), APPLICATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .auth()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testGetConfiguration_Organization_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, "anyId")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGetConfiguration_Application_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, "anyId")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testSetConfiguration_Organization_AuthorizedUser() throws Exception {
    Organization org = tempEntity.newOrganization();
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
  public void testSetConfiguration_Application_AuthorizedUser() throws Exception {
    Application app = tempEntity.newApplication(org.getPublicId());
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
  public void testSetConfiguration_Organization_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, "anyId")
        .anon()
        .body(new ScanHealthConfigDTO())
        .put();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testSetConfiguration_Application_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, "anyId")
        .anon()
        .body(new ScanHealthConfigDTO())
        .put();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testDeleteConfiguration_Organization_AuthorizedUser() throws Exception {
    Organization org = tempEntity.newOrganization();
    saveConfig(org.getId(), ORGANIZATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth()
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testDeleteConfiguration_Application_AuthorizedUser() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getPublicId());
    saveConfig(app.getId(), APPLICATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .auth()
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testDeleteConfiguration_Organization_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, "anyId")
        .anon()
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testDeleteConfiguration_Application_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, "anyId")
        .anon()
        .delete();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGetConfiguration_Organization_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth(unauthorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testSetConfiguration_Organization_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth(unauthorized)
        .body(new ScanHealthConfigDTO(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testDeleteConfiguration_Organization_Unauthorized() throws Exception {
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
