/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.service.Configuration;

@Named
@Singleton
public class ComponentDetailsLoaderFactory
{
  private final ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  private final Configuration configuration;

  @Inject
  public ComponentDetailsLoaderFactory(
      ProprietaryComponentNameDetector proprietaryComponentNameDetector,
      Configuration configuration)
  {
    this.proprietaryComponentNameDetector = proprietaryComponentNameDetector;
    this.configuration = configuration;
  }

  public ComponentDetailsLoader newInstance(Owner owner) {
    return new DefaultComponentDetailsLoader(owner, proprietaryComponentNameDetector, configuration);
  }
}
