/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.dto.TenantMetadataDTO;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_METADATA_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class TenantMetadataResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private static final TenantMetadataDTO metadata =
      new TenantMetadataDTO("appId1", "appName1", "connId1", "connName1", "encKeyName1", "orgId", "orgName");

  private TenantMetadataDAO tenantMetadataDAO;

  @Before
  public void before() {
    tenantMetadataDAO = lookup(TenantMetadataDAO.class);
  }

  @Test
  public void shouldSetTenantMetadata_whenTenantExists() throws Exception {
    HttpResponse response = updateTenantMetadata(getTestTenant().tenantSlug, metadata).put();

    assertResponseStatus(204, response);
    assertMetadataResult(tenantMetadataDAO.get(), TenantMetadataDTO.fromDTO(metadata));
  }

  @Test
  public void shouldUpdateTenantMetadata_canBeCalledMultipleTimes_whenTenantExists() throws Exception {
    HttpResponse response1 = updateTenantMetadata(getTestTenant().tenantSlug, metadata).put();

    assertResponseStatus(204, response1);
    assertMetadataResult(tenantMetadataDAO.get(), TenantMetadataDTO.fromDTO(metadata));

    TenantMetadataDTO payload2 =
        new TenantMetadataDTO("appId1", "appName1", "connId1", "connName1", "encKeyName2", "orgId", "orgName");

    HttpResponse response2 = updateTenantMetadata(getTestTenant().tenantSlug, payload2).put();

    assertResponseStatus(204, response2);

    assertMetadataResult(tenantMetadataDAO.get(), TenantMetadataDTO.fromDTO(payload2));
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateTenantMetadata("global", metadata).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Operation not supported for global tenant");
  }

  @Test
  public void shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateTenantMetadata("tenant4", metadata).put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant tenant4 doesn't exist");
  }

  @Test
  public void shouldGetTenantMetadata_whenTenantExists() throws Exception {
    // Pre-populate metadata via the existing PUT endpoint so the GET has something to read.
    HttpResponse putResponse = updateTenantMetadata(getTestTenant().tenantSlug, metadata).put();
    assertResponseStatus(204, putResponse);

    HttpResponse getResponse = getTenantMetadata(getTestTenant().tenantSlug).get();

    assertResponseStatus(200, getResponse);
    TenantMetadataDTO body = objectMapper.readValue(getResponse.getBodyText(), TenantMetadataDTO.class);
    assertThat(body.getApplicationId()).isEqualTo(metadata.getApplicationId());
    assertThat(body.getApplicationName()).isEqualTo(metadata.getApplicationName());
    assertThat(body.getConnectionId()).isEqualTo(metadata.getConnectionId());
    assertThat(body.getConnectionName()).isEqualTo(metadata.getConnectionName());
    assertThat(body.getEncryptionKeyName()).isEqualTo(metadata.getEncryptionKeyName());
    assertThat(body.getOrganizationId()).isEqualTo(metadata.getOrganizationId());
    assertThat(body.getOrganizationName()).isEqualTo(metadata.getOrganizationName());
  }

  @Test
  public void shouldGet404_whenGettingMetadataAndNotConfigured() throws Exception {
    HttpResponse response = getTenantMetadata(getTestTenant().tenantSlug).get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant metadata not set");
  }

  @Test
  public void shouldGet400_whenGettingMetadataAndTenantIsGlobal() throws Exception {
    HttpResponse response = getTenantMetadata("global").get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Operation not supported for global tenant");
  }

  @Test
  public void shouldGet404_whenGettingMetadataAndTenantDoesntExist() throws Exception {
    HttpResponse response = getTenantMetadata("tenant4").get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant tenant4 doesn't exist");
  }

  private HttpRequest updateTenantMetadata(String tenant, final TenantMetadataDTO metadata) throws Exception {
    return adminRestRequest(ADMIN_TENANT_METADATA_PATH)
        .parameter(tenant)
        .body(objectMapper.writeValueAsString(metadata));
  }

  private HttpRequest getTenantMetadata(String tenant) {
    return adminRestRequest(ADMIN_TENANT_METADATA_PATH).parameter(tenant);
  }

  private void assertMetadataResult(TenantMetadata tenantMetadata1, TenantMetadata tenantMetadata2) {
    testAsTestTenant(tenant -> {
      assertEquals(tenantMetadata1.getApplicationId(), tenantMetadata2.getApplicationId());
      assertEquals(tenantMetadata1.getApplicationName(), tenantMetadata2.getApplicationName());
      assertEquals(tenantMetadata1.getConnectionId(), tenantMetadata2.getConnectionId());
      assertEquals(tenantMetadata1.getConnectionName(), tenantMetadata2.getConnectionName());
      assertEquals(tenantMetadata1.getEncryptionKeyName(), tenantMetadata2.getEncryptionKeyName());
      assertEquals(tenantMetadata1.getOrganizationId(), tenantMetadata2.getOrganizationId());
      assertEquals(tenantMetadata1.getOrganizationName(), tenantMetadata2.getOrganizationName());
    });
  }
}
