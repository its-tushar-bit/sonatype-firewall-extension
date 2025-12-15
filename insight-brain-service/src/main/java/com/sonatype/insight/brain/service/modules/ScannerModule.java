/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.scan.hash.Digester;
import com.sonatype.insight.scan.hash.internal.DefaultDigester;
import com.sonatype.insight.scan.hash.internal.JavaDigester;

import com.google.inject.AbstractModule;

/**
 * Guice module providing explicit bindings for insight scanner components.
 * This replaces Sisu's automatic @Named component discovery.
 */
public class ScannerModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(JavaDigester.class);
    bind(Digester.class).to(DefaultDigester.class);
  }
}
