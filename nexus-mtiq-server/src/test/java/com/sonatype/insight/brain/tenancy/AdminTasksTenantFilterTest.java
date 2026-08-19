/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.PrintWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter.TASKS_API_ERROR_MGS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminTasksTenantFilterTest
    extends AbstractMultiTenantTest
{
  private static final String TENANT_NAME = "tenant1";

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private PrintWriter printWriter;

  @Mock
  private FilterChain chain;

  @Mock
  private RequestDispatcher dispatcher;

  @Test
  public void shouldRewriteTasksUrl_whenTenantTasksUrlIsPassed() throws Exception {
    final AdminTasksTenantFilter adminTasksTenantFilter = new AdminTasksTenantFilter();

    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s/tasks/theTask", TENANT_NAME));
    when(request.getRequestDispatcher("/tasks/theTask")).thenReturn(dispatcher);

    adminTasksTenantFilter.doFilter(request, response, chain);

    verify(dispatcher).forward(request, response);
  }

  @Test
  public void shouldNotRewriteTenantUrl_whenNotTasksUrl() throws Exception {
    final AdminTasksTenantFilter adminTasksTenantFilter = new AdminTasksTenantFilter();

    when(request.getRequestURI()).thenReturn(String.format("/api/admin/tenants/%s/foo/bar", TENANT_NAME));

    adminTasksTenantFilter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldReturnBadRequest_whenNoneTenantTasksUrl() throws Exception {
    final AdminTasksTenantFilter adminTasksTenantFilter = new AdminTasksTenantFilter();

    when(response.getWriter()).thenReturn(printWriter);
    when(request.getRequestURI()).thenReturn("/tasks/someTask");

    adminTasksTenantFilter.doFilter(request, response, chain);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType(ContentType.TEXT_PLAIN.getMimeType());
    verify(response).getWriter();
    verify(printWriter).print(TASKS_API_ERROR_MGS);
  }
}
