/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.google.inject.AbstractModule;

import com.sonatype.insight.brain.operational.check.ExistingDbConnectionOperationalCheck;
import com.sonatype.insight.brain.operational.check.NewDbConnectionOperationalCheck;
import com.sonatype.insight.brain.operational.check.ProductLicenseOperationalCheck;
import com.sonatype.insight.brain.operational.check.ShutdownStateOperationalCheck;
import com.sonatype.insight.brain.operational.check.WorkDirectoriesOperationalCheck;

/**
 * Guice module providing explicit bindings for Operational components.
 * This replaces Sisu's automatic @Named component discovery.
 */
public class OperationalModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(ExistingDbConnectionOperationalCheck.class);
    bind(NewDbConnectionOperationalCheck.class);
    bind(ProductLicenseOperationalCheck.class);
    bind(ShutdownStateOperationalCheck.class);
    bind(WorkDirectoriesOperationalCheck.class);
  }
}
