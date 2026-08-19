/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;

import org.junit.rules.ExternalResource;

/**
 * Restores the JVM-wide singletons used for updating the datamart to their original values after a test to avoid
 * unintended consequences on later tests.
 */
public class DatamartUpdaterState
    extends ExternalResource
{
  private LicenseDataUpdater licenseDataUpdater;

  private AbstractComponentCategoryUpdater componentCategoryUpdater;

  @Override
  protected void before() {
    licenseDataUpdater = LicenseDataUpdater.getUpdater();
    componentCategoryUpdater = AbstractComponentCategoryUpdater.getUpdater();
  }

  @Override
  protected void after() {
    LicenseDataUpdater.setUpdater(licenseDataUpdater);
    AbstractComponentCategoryUpdater.setUpdater(componentCategoryUpdater);
  }
}
