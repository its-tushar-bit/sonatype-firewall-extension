/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.ArrayList;
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

  private final List<Class<?>> bannedClasses;

  public DefaultBannedImplementation(Class<?>... extraToBan) {
    bannedClasses = new ArrayList<>(DEFAULT_BANNED_CLASSES);
    bannedClasses.addAll(Arrays.asList(extraToBan));
  }

  @Override
  public boolean isBanned(Class<?> clazz) {
    return bannedClasses.contains(clazz);
  }
}
