/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ApiScanHealthConfigurationResource REST endpoints.
 *
 * @since 1.209
 */
@IqH2Test
class IqH2ApiScanHealthConfigurationResourceTest
{
  private IqTestContext ctx;

  private ScanHealthConfigDAO scanHealthConfigDAO;

  @BeforeEach
  void setup() {
    scanHealthConfigDAO = ctx.lookup(ScanHealthConfigDAO.class);
  }

  @Test
  void testGetConfiguration_success() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    saveConfig(org.getId(), ORGANIZATION, true);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .get();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("\"failOnZeroComponents\":true");
  }

  @Test
  void testGetConfiguration_returnsDefaultWhenNotConfigured() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .get();

    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testGetConfiguration_notFound() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, "nonExistentId123")
        .get();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testSetConfiguration_success() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ScanHealthConfigDTO config = new ScanHealthConfigDTO(true);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .body(config)
        .put();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("\"failOnZeroComponents\":true");
  }

  @Test
  void testSetConfiguration_disableFeature() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    saveConfig(org.getId(), ORGANIZATION, true);

    ScanHealthConfigDTO config = new ScanHealthConfigDTO(false);
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .body(config)
        .put();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("\"failOnZeroComponents\":false");
  }

  @Test
  void testDeleteConfiguration_success() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    saveConfig(org.getId(), ORGANIZATION, true);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testDeleteConfiguration_notFound() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .delete();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains("not found");
  }

  @Test
  void testGetConfiguration_applicationOwnerType() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    var app = ctx.tempEntity().newApplication(org.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .get();

    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testSetConfiguration_applicationOwnerType() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    var app = ctx.tempEntity().newApplication(org.getId());
    ScanHealthConfigDTO config = new ScanHealthConfigDTO(true);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .body(config)
        .put();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("\"failOnZeroComponents\":true");
  }

  private void saveConfig(final String ownerId, final OwnerType ownerType, final Boolean failOnZeroComponents) {
    String json = "{\"failOnZeroComponents\":" + failOnZeroComponents + "}";
    ScanHealthConfig config = new ScanHealthConfig(ownerId, ownerType.toString(), json);
    scanHealthConfigDAO.save(config);
  }
}
