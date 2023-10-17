/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;

import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.ResourceInstaller;

/**
 * ResourceInstaller is the part of dropwizard-guicey that detects jersey resource classes and registers
 * them in Jersey. This subclass avoids automatically registering MTIQ admin endpoints in the main jersey bundle
 */
public class MtiqResourceInstaller
    extends ResourceInstaller
{
  public static boolean isAdminResource(final Class<?> type) {
    return type.isAnnotationPresent(MtiqAdminEndpoint.class);
  }

  @Override
  public boolean matches(final Class<?> type) {
    return super.matches(type) && !isAdminResource(type);
  }
}
