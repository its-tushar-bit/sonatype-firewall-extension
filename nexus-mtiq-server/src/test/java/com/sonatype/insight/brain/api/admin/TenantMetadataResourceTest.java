/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import javax.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.api.admin.dto.TenantMetadataDTO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;

import org.junit.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_METADATA_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class TenantMetadataResourceTest
    extends AbstractMultiTenantResourceTest
{
  private static final TenantMetadataDTO metadata =
      new TenantMetadataDTO("appId1", "appName1", "connId1", "connName1", "encKeyName1");

  private TenantMetadataDAO tenantMetadataDAO = new TenantMetadataDAO();

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

    TenantMetadataDTO payload2 = new TenantMetadataDTO("appId1", "appName1", "connId1", "connName1", "encKeyName2");

    HttpResponse response2 = updateTenantMetadata(getTestTenant().tenantSlug, payload2).put();

    assertResponseStatus(204, response2);

    assertMetadataResult(tenantMetadataDAO.get(), TenantMetadataDTO.fromDTO(payload2));
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateTenantMetadata("global", metadata).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateTenantMetadata("tenant4", metadata).put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  private HttpRequest restRequest(String path) {
    return super.adminRequest().path("api/").path(path);
  }

  private HttpRequest updateTenantMetadata(String tenant, final TenantMetadataDTO metadata) throws Exception {
    HttpRequest request;

    ObjectMapper objectMapper = new ObjectMapper();
    if (tenant != null) {
      request = restRequest(ADMIN_TENANT_METADATA_PATH)
          .parameter(tenant)
          .body(objectMapper.writeValueAsString(metadata))
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt());
    }
    else {
      request = restRequest();
    }
    return request;
  }

  private void assertMetadataResult(TenantMetadata tenantMetadata1, TenantMetadata tenantMetadata2) {
    testAsTestTenant(tenant -> {
      assertEquals(tenantMetadata1.getApplicationId(), tenantMetadata2.getApplicationId());
      assertEquals(tenantMetadata1.getApplicationName(), tenantMetadata2.getApplicationName());
      assertEquals(tenantMetadata1.getConnectionId(), tenantMetadata2.getConnectionId());
      assertEquals(tenantMetadata1.getConnectionName(), tenantMetadata2.getConnectionName());
      assertEquals(tenantMetadata1.getEncryptionKeyName(), tenantMetadata2.getEncryptionKeyName());
    });
  }
}
