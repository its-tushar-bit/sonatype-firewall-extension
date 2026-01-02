/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.TestShutdownHandler;

import com.google.inject.Binder;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter.TASKS_API_ERROR_MGS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class AdminTasksTenantFilterRedirectTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Override
  public void configure(final Binder binder) {
    super.configure(binder);
    binder.bind(ShutdownHandler.class).toInstance(spy(new TestShutdownHandler()));
  }

  @Test
  public void testAdminTasksTenantFilter_redirectsTenantTasksUrls() throws Exception {
    assertThat(adminRequest().path("api", "admin", "tenants", getTestTenant().tenantSlug, "tasks", "log-level").post()
        .getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testAdminTasksTenantFilter_tasksUrlReturnsError() throws Exception {
    HttpResponse request = adminRequest().path("tasks", "log-level").post();
    assertThat(request.getStatusCode()).isEqualTo(400);
    assertThat(request.getBodyText()).isEqualTo(TASKS_API_ERROR_MGS);
  }

  @Test
  public void testTasksShutdown_tasksUrlReturnsError() throws Exception {
    HttpResponse request = adminRequest().path("tasks", "shutdown").post();
    assertThat(request.getStatusCode()).isEqualTo(400);
    assertThat(request.getBodyText()).isEqualTo(TASKS_API_ERROR_MGS);
  }

  @Test
  public void testTasksShutdown() throws Exception {
    try {
      HttpResponse httpResponse =
          adminRequest().path("api", "admin", "tenants", getTestTenant().tenantSlug, "tasks", "shutdown").post();

      assertThat(httpResponse.getStatusCode()).isEqualTo(200);
      TestShutdownHandler spyTestShutdownHandler =
          (TestShutdownHandler) getCLMServer().getInstance(ShutdownHandler.class);
      verify(spyTestShutdownHandler, timeout(10000)).exit(0);
    }
    finally {
      stopClmServer();
    }
  }
}
