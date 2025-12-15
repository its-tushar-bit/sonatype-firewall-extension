/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.google.inject.AbstractModule;

/**
 * Adding this module prevents JIT binding
 */
public class RequireExplicitBindingsModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // Turn off JIT bindings globally
    binder().requireExplicitBindings();
  }
}
