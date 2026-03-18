/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.apache.commons.lang3.exception.UncheckedException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_PATH;
import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_PROVISIONING_PATH;
import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class TenantProvisioningResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void shouldProvisionTenant() throws Exception {
    HttpResponse response = provisionTenantViaRest(generateTestTenantName());

    assertResponseStatus(204, response);
  }

  @Test
  public void shouldSend400_whenTenantAlreadyExists() throws Exception {
    String tenantSlug = generateTestTenantName();
    HttpResponse response = provisionTenantViaRest(tenantSlug);
    assertResponseStatus(204, response);

    response = provisionTenantViaRest(tenantSlug);
    assertResponseStatus(409, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant already exists");
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = provisionTenantViaRest("global");

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldDeleteTenant() throws Exception {
    String tenantName = generateTestTenantName();

    HttpResponse createResponse = provisionTenantViaRest(tenantName);
    assertResponseStatus(204, createResponse);

    DeletedTenantDAO deletedTenantDAO = getCLMServer().getInstance(DeletedTenantDAO.class);
    assertThat(deletedTenantDAO.getTenantBySlug(tenantName)).isNull();

    HttpResponse deleteResponse = deleteTenant(tenantName);
    assertResponseStatus(204, deleteResponse);

    // Deletion records should be stored globally and not per-tenant
    testAsGlobal(t -> assertThat(deletedTenantDAO.getTenantBySlug(tenantName)).isNotNull());
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

    HttpResponse createResponse = provisionTenantViaRest(tenantName);
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

  protected HttpResponse provisionTenantViaRest(final String tenantName) {
    setTenantSlug(tenantName);

    try {
      HttpResponse httpResponse = adminRestRequest(ADMIN_TENANT_PROVISIONING_PATH)
          .parameter(tenantName)
          .post();

      TenantTestHelper.testAsNewTenant(tenantName, tenant -> {
        testProductLicenseRule.insertLicenseIfNeeded();
      });

      // This just makes an endpoint call which indirectly will ensure the tenant registration process is invoked
      adminRestRequest(ADMIN_CONFIG_PATH)
          .parameter(tenantName)
          .get();

      return httpResponse;
    }
    catch (Exception e) {
      throw new UncheckedException(e);
    }
  }
}
