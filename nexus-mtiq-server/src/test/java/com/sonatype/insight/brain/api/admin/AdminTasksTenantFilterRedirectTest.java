/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter.TASKS_API_ERROR_MGS;
import static org.assertj.core.api.Assertions.assertThat;

public class AdminTasksTenantFilterRedirectTest
    extends AbstractMultiTenantResourceTest
{
  private String tenantName;

  @Before
  public void setUp() throws Exception {
    tenantName = generateTestTenantName();
    provisionTenant(tenantName);
  }

  @Test
  public void testAdminTasksTenantFilter_redirectsTenantTasksUrls() throws Exception {
    assertThat(adminRequest().path("api", "admin", "tenants", tenantName, "tasks", "log-level").post()
        .getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testAdminTasksTenantFilter_tasksUrlReturnsError() throws Exception {
    HttpResponse request = adminRequest().path("tasks", "log-level").post();
    assertThat(request.getStatusCode()).isEqualTo(400);
    assertThat(request.getBodyText()).isEqualTo(TASKS_API_ERROR_MGS);
  }
}
