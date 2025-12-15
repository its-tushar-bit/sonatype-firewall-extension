/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.IqOnlyEndpoint;

import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.ResourceInstaller;

/**
 * ResourceInstaller is the part of dropwizard-guicey that detects jersey resource classes and registers
 * them in Jersey. This subclass avoids automatically registering:
 * <ul>
 *   <li>MTIQ admin endpoints (marked with @MtiqAdminEndpoint) - these go in the admin bundle</li>
 *   <li>IQ-only endpoints (marked with @IqOnlyEndpoint) - these should not be available in MTIQ</li>
 * </ul>
 */
public class MtiqResourceInstaller
    extends ResourceInstaller
{
  public static boolean isAdminResource(final Class<?> type) {
    return type.isAnnotationPresent(MtiqAdminEndpoint.class);
  }

  public static boolean isIqOnlyResource(final Class<?> type) {
    return type.isAnnotationPresent(IqOnlyEndpoint.class);
  }

  @Override
  public boolean matches(final Class<?> type) {
    // Exclude both admin resources AND IQ-only resources from main jersey bundle
    return super.matches(type) && !isAdminResource(type) && !isIqOnlyResource(type);
  }
}
