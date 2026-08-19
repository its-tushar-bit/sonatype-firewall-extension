/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import org.springframework.context.annotation.Primary;

@Named
@Singleton
@Primary
public class MultiTenantActiveRequestCounterFilter
    extends ActiveRequestCounterFilter
{
  private static final Pattern SHUTDOWN_PATH_REGEX = Pattern.compile("^/api/admin/tenants/[^/]+/tasks/shutdown$");

  @Inject
  public MultiTenantActiveRequestCounterFilter(final ShutdownHandler shutdownHandler) {
    super(shutdownHandler);
  }

  @Override
  public boolean isShutdownPath(final String path) {
    return SHUTDOWN_PATH_REGEX.matcher(path).matches();
  }
}
