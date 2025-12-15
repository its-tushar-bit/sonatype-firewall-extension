/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.PolicyEvaluationHelper;

import com.google.inject.AbstractModule;

/**
 * Guice module providing explicit bindings for test helper components.
 * This module binds test-only @Named components that cannot be bound in production modules.
 */
public class TestHelperModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // Test helper components
    bind(PolicyEvaluationHelper.class);
  }
}
