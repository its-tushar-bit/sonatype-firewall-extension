/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_PROVISIONING_PATH;
import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantProvisioningResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void shouldProvisionTenant() throws Exception {
    HttpResponse response = provisionTenant(generateTestTenantName());

    assertResponseStatus(204, response);
  }

  @Test
  public void shouldSend400_whenTenantAlreadyExists() throws Exception {
    String tenantSlug = generateTestTenantName();
    HttpResponse response = provisionTenant(tenantSlug);
    assertResponseStatus(204, response);

    response = provisionTenant(tenantSlug);
    assertResponseStatus(409, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant already exists");
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = provisionTenant("global");

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldDeleteTenant() throws Exception {
    String tenantName = generateTestTenantName();

    HttpResponse createResponse = provisionTenant(tenantName);
    assertResponseStatus(204, createResponse);

    DeletedTenantDAO deletedTenantDAO = getCLMServer().getInstance(DeletedTenantDAO.class);
    assertThat(deletedTenantDAO.getTenantBySlug(tenantName)).isNull();

    HttpResponse deleteResponse = deleteTenant(tenantName);
    assertResponseStatus(204, deleteResponse);

    //Deletion records should be stored globally and not per-tenant
    testAs(GLOBAL_TENANT, t -> assertThat(deletedTenantDAO.getTenantBySlug(tenantName)).isNotNull());
  }

  @Test
  public void shouldReturn400WhenTenantDoesntExist() throws Exception {
    String tenantName = generateTestTenantName();

    DeletedTenantDAO deletedTenantDAO = getCLMServer().getInstance(DeletedTenantDAO.class);
    assertThat(deletedTenantDAO.getTenantBySlug(tenantName)).isNull();

    HttpResponse deleteResponse = deleteTenant(tenantName);
    assertResponseStatus(400, deleteResponse);
  }

  @Test
  public void shouldReturn400WhenGlobalTenantPassed() throws Exception {
    HttpResponse deleteResponse = deleteTenant(GLOBAL_TENANT.tenantSlug);
    assertResponseStatus(400, deleteResponse);
  }

  @Test
  public void shouldReturn400_whenTenantAlreadyMarkedForDeletion() throws Exception {
    String tenantName = generateTestTenantName();

    HttpResponse createResponse = provisionTenant(tenantName);
    assertResponseStatus(204, createResponse);

    assertResponseStatus(204, deleteTenant(tenantName));
    assertResponseStatus(400, deleteTenant(tenantName));
  }

  public HttpResponse deleteTenant(String tenantName) throws Exception {
    setTenantSlug(tenantName);
    return adminRestRequest(ADMIN_TENANT_PROVISIONING_PATH)
        .parameter(tenantName)
        .delete();
  }
}
