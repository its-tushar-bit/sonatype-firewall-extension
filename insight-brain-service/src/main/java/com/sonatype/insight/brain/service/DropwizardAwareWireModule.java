/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;

import com.google.inject.Module;
import io.dropwizard.core.Configuration;
import org.eclipse.sisu.wire.WireModule;

/**
 * dropwizard-guicey's GuiceBundle looks for modules that are `instanceof DropwizardAwareModule` and sets their
 * special properties. We need to delegate that setting while also delegating to Sisu's WireModule which is
 * what allows dependencies to be automatically wired
 */
public class DropwizardAwareWireModule<C extends Configuration>
    extends DropwizardAwareAggregatingModule<C>
{
  private WireModule wireModule;

  public DropwizardAwareWireModule(List<Module> modules) {
    super(modules);
    wireModule = new WireModule(modules);
  }

  public DropwizardAwareWireModule(Module... modules) {
    super(modules);
    wireModule = new WireModule(modules);
  }

  @Override
  protected void configure() {
    wireModule.configure(binder());
  }

  public DropwizardAwareWireModule with(WireModule.Strategy strat) {
    wireModule.with(strat);
    return this;
  }
}
