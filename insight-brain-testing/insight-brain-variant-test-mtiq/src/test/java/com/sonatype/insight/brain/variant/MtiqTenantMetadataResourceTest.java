/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.dto.TenantMetadataDTO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_METADATA_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MTIQ variant conversion of {@code TenantMetadataResourceTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationTest}). No base class; an injected {@link MtiqTestContext} supplies the
 * reused multi-tenant server, a fresh per-test tenant, and admin REST/lookup access.
 */
@MtiqTest
class MtiqTenantMetadataResourceTest
{
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final TenantMetadataDTO metadata =
      new TenantMetadataDTO("appId1", "appName1", "connId1", "connName1", "encKeyName1", "orgId", "orgName");

  // Injected by MtiqServerExtension: the reused multi-tenant server + a fresh per-test tenant context.
  private MtiqTestContext ctx;

  private TenantMetadataDAO tenantMetadataDAO;

  @BeforeEach
  void before() {
    tenantMetadataDAO = ctx.lookup(TenantMetadataDAO.class);
  }

  @Test
  void shouldSetTenantMetadata_whenTenantExists() throws Exception {
    HttpResponse response = updateTenantMetadata(ctx.getTestTenant().tenantSlug, metadata).put();

    ctx.assertResponseStatus(204, response);
    assertMetadataResult(tenantMetadataDAO.get(), TenantMetadataDTO.fromDTO(metadata));
  }

  @Test
  void shouldUpdateTenantMetadata_canBeCalledMultipleTimes_whenTenantExists() throws Exception {
    HttpResponse response1 = updateTenantMetadata(ctx.getTestTenant().tenantSlug, metadata).put();

    ctx.assertResponseStatus(204, response1);
    assertMetadataResult(tenantMetadataDAO.get(), TenantMetadataDTO.fromDTO(metadata));

    TenantMetadataDTO payload2 =
        new TenantMetadataDTO("appId1", "appName1", "connId1", "connName1", "encKeyName2", "orgId", "orgName");

    HttpResponse response2 = updateTenantMetadata(ctx.getTestTenant().tenantSlug, payload2).put();

    ctx.assertResponseStatus(204, response2);

    assertMetadataResult(tenantMetadataDAO.get(), TenantMetadataDTO.fromDTO(payload2));
  }

  @Test
  void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateTenantMetadata("global", metadata).put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Operation not supported for global tenant");
  }

  @Test
  void shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateTenantMetadata("tenant4", metadata).put();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant tenant4 doesn't exist");
  }

  @Test
  void shouldGetTenantMetadata_whenTenantExists() throws Exception {
    // Pre-populate metadata via the existing PUT endpoint so the GET has something to read.
    HttpResponse putResponse = updateTenantMetadata(ctx.getTestTenant().tenantSlug, metadata).put();
    ctx.assertResponseStatus(204, putResponse);

    HttpResponse getResponse = getTenantMetadata(ctx.getTestTenant().tenantSlug).get();

    ctx.assertResponseStatus(200, getResponse);
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
  void shouldGet404_whenGettingMetadataAndNotConfigured() throws Exception {
    HttpResponse response = getTenantMetadata(ctx.getTestTenant().tenantSlug).get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant metadata not set");
  }

  @Test
  void shouldGet400_whenGettingMetadataAndTenantIsGlobal() throws Exception {
    HttpResponse response = getTenantMetadata("global").get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Operation not supported for global tenant");
  }

  @Test
  void shouldGet404_whenGettingMetadataAndTenantDoesntExist() throws Exception {
    HttpResponse response = getTenantMetadata("tenant4").get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant tenant4 doesn't exist");
  }

  private HttpRequest updateTenantMetadata(String tenant, final TenantMetadataDTO metadata) throws Exception {
    return ctx.adminRestRequest(ADMIN_TENANT_METADATA_PATH)
        .parameter(tenant)
        .body(objectMapper.writeValueAsString(metadata));
  }

  private HttpRequest getTenantMetadata(String tenant) {
    return ctx.adminRestRequest(ADMIN_TENANT_METADATA_PATH).parameter(tenant);
  }

  private void assertMetadataResult(TenantMetadata tenantMetadata1, TenantMetadata tenantMetadata2) {
    ctx.testAsTestTenant(tenant -> {
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
