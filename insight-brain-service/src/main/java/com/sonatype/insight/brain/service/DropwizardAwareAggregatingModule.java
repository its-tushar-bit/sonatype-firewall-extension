/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.google.inject.Binder;
import com.google.inject.Module;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

/**
 * dropwizard-guicey's GuiceBundle looks for modules that are `instanceof DropwizardAwareModule` and sets their
 * special properties. We have some module classes that aggregate child modules and which need to propagate
 * that "dropwizard awareness" down. This class facilitates that and can be subclassed by those other module types.
 */
public class DropwizardAwareAggregatingModule<C extends Configuration>
    extends DropwizardAwareModule<C>
{
  protected Collection<Module> modules;

  public DropwizardAwareAggregatingModule(List<Module> modules) {
    this.modules = modules;
  }

  public DropwizardAwareAggregatingModule(Module... modules) {
    this(Arrays.asList(modules));
  }

  @Override
  protected void configure() {
    Binder binder = binder();
    for (Module module : modules) {
      binder.install(module);
    }
  }

  @Override
  public void setBootstrap(Bootstrap<C> bootstrap) {
    forAllDropwizardAwareModules(m -> m.setBootstrap(bootstrap));
  }

  @Override
  public void setConfiguration(C configuration) {
    forAllDropwizardAwareModules(m -> m.setConfiguration(configuration));
  }

  @Override
  public void setEnvironment(Environment env) {
    forAllDropwizardAwareModules(m -> m.setEnvironment(env));
  }

  @SuppressWarnings("unchecked")
  private void forAllDropwizardAwareModules(Consumer<DropwizardAwareModule<C>> fn) {
    for (Module module : modules) {
      if (module instanceof DropwizardAwareModule) {
        fn.accept((DropwizardAwareModule<C>) module);
      }
    }
  }
}
