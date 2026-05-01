/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ApiScanHealthConfigurationResource REST endpoints.
 *
 * @since 1.209
 */
public class ApiScanHealthConfigurationResourceTest
    extends AbstractResourceTest
{
  private ScanHealthConfigDAO scanHealthConfigDAO;

  @Before
  public void setup() {
    scanHealthConfigDAO = lookup(ScanHealthConfigDAO.class);
  }

  @Test
  public void testGetConfiguration_success() throws Exception {
    Organization org = tempEntity.newOrganization();
    saveConfig(org.getId(), ORGANIZATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .get();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("\"failOnZeroComponents\":true");
  }

  @Test
  public void testGetConfiguration_returnsDefaultWhenNotConfigured() throws Exception {
    Organization org = tempEntity.newOrganization();

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .get();

    assertResponseStatus(200, response);
  }

  @Test
  public void testGetConfiguration_notFound() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, "nonExistentId123")
        .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testSetConfiguration_success() throws Exception {
    Organization org = tempEntity.newOrganization();
    ScanHealthConfigDTO config = new ScanHealthConfigDTO(true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .body(config)
        .put();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("\"failOnZeroComponents\":true");
  }

  @Test
  public void testSetConfiguration_disableFeature() throws Exception {
    Organization org = tempEntity.newOrganization();
    saveConfig(org.getId(), ORGANIZATION, true);

    ScanHealthConfigDTO config = new ScanHealthConfigDTO(false);
    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .body(config)
        .put();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("\"failOnZeroComponents\":false");
  }

  @Test
  public void testDeleteConfiguration_success() throws Exception {
    Organization org = tempEntity.newOrganization();
    saveConfig(org.getId(), ORGANIZATION, true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .delete();

    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteConfiguration_notFound() throws Exception {
    Organization org = tempEntity.newOrganization();

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .delete();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains("not found");
  }

  @Test
  public void testGetConfiguration_applicationOwnerType() throws Exception {
    Organization org = tempEntity.newOrganization();
    var app = tempEntity.newApplication(org.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .get();

    assertResponseStatus(200, response);
  }

  @Test
  public void testSetConfiguration_applicationOwnerType() throws Exception {
    Organization org = tempEntity.newOrganization();
    var app = tempEntity.newApplication(org.getId());
    ScanHealthConfigDTO config = new ScanHealthConfigDTO(true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .body(config)
        .put();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("\"failOnZeroComponents\":true");
  }

  private void saveConfig(final String ownerId, final OwnerType ownerType, final Boolean failOnZeroComponents) {
    String json = "{\"failOnZeroComponents\":" + failOnZeroComponents + "}";
    ScanHealthConfig config = new ScanHealthConfig(ownerId, ownerType.toString(), json);
    scanHealthConfigDAO.save(config);
  }
}
