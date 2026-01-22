/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.service.Configuration;

@Named
@Singleton
public class ComponentDetailsLoaderFactory
{
  private final ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  private final Configuration configuration;

  private final LicenseDAO licenseDAO;

  private final ComponentLoaderFactory componentLoaderFactory;

  @Inject
  public ComponentDetailsLoaderFactory(
      ProprietaryComponentNameDetector proprietaryComponentNameDetector,
      Configuration configuration,
      LicenseDAO licenseDAO,
      ComponentLoaderFactory componentLoaderFactory)
  {
    this.proprietaryComponentNameDetector = proprietaryComponentNameDetector;
    this.configuration = configuration;
    this.licenseDAO = licenseDAO;
    this.componentLoaderFactory = componentLoaderFactory;
  }

  public ComponentDetailsLoader newInstance(Owner owner) {
    return new ComponentDetailsLoader(owner, proprietaryComponentNameDetector, configuration, licenseDAO,
        componentLoaderFactory);
  }
}
