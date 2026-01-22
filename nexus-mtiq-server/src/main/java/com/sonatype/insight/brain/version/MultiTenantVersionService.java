/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import jakarta.annotation.Priority;
import jakarta.inject.Named;

import ru.vyarus.dropwizard.guice.module.installer.order.Order;

@Named
@Priority(MultiTenantVersionService.PRIORITY)
@Order(Integer.MAX_VALUE - MultiTenantVersionService.PRIORITY)
public class MultiTenantVersionService
    extends DefaultVersionService
{
  public static final int PRIORITY = 1;

  @Override
  public String getShortVersion() {
    return getBuild();
  }

  @Override
  public String getFullVersion() {
    return getBuild();
  }
}
