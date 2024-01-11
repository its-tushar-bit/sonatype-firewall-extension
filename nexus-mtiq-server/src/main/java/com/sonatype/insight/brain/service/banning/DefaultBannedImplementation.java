/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.repository.InactiveRepositoryViolationCleaner;
import com.sonatype.insight.brain.service.DefaultTenantManagedInitializer;
import com.sonatype.insight.brain.telemetry.DefaultTelemetryScheduler;
import com.sonatype.insight.brain.version.VersionService;

public class DefaultBannedImplementation
    implements BannedImplementation
{
  private static final List<Class<?>> DEFAULT_BANNED_CLASSES =
      Arrays.asList(
          DefaultTenantManagedInitializer.class,
          DefaultTelemetryScheduler.class,
          InactiveRepositoryViolationCleaner.class,
          VersionService.class
      );

  @Override
  public boolean isBanned(Class<?> clazz) {
    return DEFAULT_BANNED_CLASSES.contains(clazz);
  }
}
