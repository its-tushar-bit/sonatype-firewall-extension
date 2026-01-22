/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class MultiTenantActiveRequestCounterFilterTest
    extends AbstractComponentTest
{
  @Inject
  private MultiTenantActiveRequestCounterFilter multiTenantActiveRequestCounterFilter;

  @Test
  public void testIsShutdownPath_DoesNotMatch() {
    assertThat(multiTenantActiveRequestCounterFilter.isShutdownPath("/other")).isFalse();
    assertThat(multiTenantActiveRequestCounterFilter.isShutdownPath("/tasks/shutdown")).isFalse();
    assertThat(multiTenantActiveRequestCounterFilter.isShutdownPath("/api/tasks/shutdown")).isFalse();
    assertThat(multiTenantActiveRequestCounterFilter.isShutdownPath("/api/admin/tasks/shutdown")).isFalse();
    assertThat(multiTenantActiveRequestCounterFilter.isShutdownPath("/api/admin/tenants/tasks/shutdown")).isFalse();
    assertThat(
        multiTenantActiveRequestCounterFilter.isShutdownPath("/api/admin/tenants/global/foo/tasks/shutdown")).isFalse();
    assertThat(multiTenantActiveRequestCounterFilter.isShutdownPath(
        "/api/admin/tenants/global/foo/bar/tasks/shutdown")).isFalse();
    assertThat(
        multiTenantActiveRequestCounterFilter.isShutdownPath("/api/admin/tenants/other/foo/tasks/shutdown")).isFalse();
    assertThat(multiTenantActiveRequestCounterFilter.isShutdownPath(
        "/api/admin/tenants/other/foo/bar/tasks/shutdown")).isFalse();
  }

  @Test
  public void testIsShutdownPath_Matches() {
    assertThat(
        multiTenantActiveRequestCounterFilter.isShutdownPath("/api/admin/tenants/global/tasks/shutdown")).isTrue();
    assertThat(
        multiTenantActiveRequestCounterFilter.isShutdownPath("/api/admin/tenants/other/tasks/shutdown")).isTrue();
  }
}
