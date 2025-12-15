/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.google.inject.AbstractModule;

import com.sonatype.insight.brain.component.ComponentDetailService;
import com.sonatype.insight.brain.component.ComponentHelper;
import com.sonatype.insight.brain.component.HashComponentIdentifierService;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCacheLoader;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentPurger;

/**
 * Guice module providing explicit bindings for Component components.
 * This replaces Sisu's automatic @Named component discovery.
 */
public class ComponentModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(ComponentDetailService.class);
    bind(ComponentHelper.class);
    bind(HashComponentIdentifierService.class);
    bind(RepositoryIdentifiedComponentCache.class);
    bind(RepositoryIdentifiedComponentCacheLoader.class);
    bind(RepositoryIdentifiedComponentPurger.class);
  }
}
