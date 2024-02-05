/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.ClassUtils;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.dropwizard.guice.module.installer.InstallersOptions;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

/**
 * What started as a copy of SisuService from https://github.com/tesla/dropwizard-sisu with various tweaks
 * for IQ and inclusion of dropwizard-guicey rather than manual registration of beans into dropwizard
 */
public abstract class SisuApplication<T extends Configuration>
    extends Application<T>
{
  private static final Logger logger = LoggerFactory.getLogger(SisuApplication.class);

  private GuiceBundle guiceBundle;

  private boolean initialized = false;

  public Injector getInjector() {
    return guiceBundle.getInjector();
  }

  public <C> C getInstance(Class<C> type) {
    return getInjector().getInstance(type);
  }

  @Override
  public void initialize(final Bootstrap<T> bootstrap) {
    guiceBundle = customizeGuiceBundle(
        GuiceBundle.builder()
          .modules(wire(modules()))
          .option(InstallersOptions.ForceSingletonForJerseyExtensions, false)
    ).build();

    bootstrap.addBundle(guiceBundle);

    super.initialize(bootstrap);
  }

  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public void run(T configuration, Environment environment) throws Exception {
    customize(configuration, environment);
    BeanLocator locator = getInjector().getInstance(BeanLocator.class);
    addAdminHealthCheckEndpoints(environment, locator);

    // NOTE: dropwizard-guicey automatically registers all our jersey and dropwizard relevant classes which
    // used to be manually registered here

    initialized = true;
  }

  protected List<Module> modules() {
    return baseModules();
  }

  protected List<Module> baseModules() {
    List<Module> modules = new ArrayList<>();

    // This module registers the config object by its class name and ancestor class names. Although dropwizard-guicey
    // does the same thing automatically, it appears that we need to do it ourselves here, before Sisu runs,
    // to keep Sisu from automatically doing it incorrectly
    modules.add(new DropwizardAwareModule<T>()
    {
      private void bindConfig(Class<? super T> cls) {
        bind(cls).toInstance(configuration());
      }

      @Override
      @SuppressWarnings("unchecked")
      public void configure() {
        Class<T> configClass = getConfigurationClass();
        bindConfig(configClass);

        for (Class<?> cls : ClassUtils.getAllSuperclasses(configClass)) {
          bindConfig((Class<? super T>)cls);
        }
        for (Class<?> cls : ClassUtils.getAllInterfaces(configClass)) {
          bindConfig((Class<? super T>)cls);
        }
      }
    });

    modules.add(getSpaceModule());

    modules.add(new DropwizardAwareModule<T>()
    {
      @Override
      public void configure() {
        bind(ObjectMapper.class).toInstance(bootstrap().getObjectMapper());
      }
    });

    return modules;
  }

  /**
   * Visible so it can be used by BrainInjectedTest to get the same SpaceModule in testing as in the app
   */
  @VisibleForTesting
  public static SpaceModule getSpaceModule() {
    ClassSpace space = new URLClassSpace(SisuApplication.class.getClassLoader());
    return new SpaceModule(space, new MultiPackageClassFinder("org.sonatype.*", "com.sonatype.*"));
  }

  protected DropwizardAwareModule<T> wire(final List<Module> modules) {
    return new DropwizardAwareWireModule<>(modules);
  }

  protected GuiceBundle.Builder customizeGuiceBundle(GuiceBundle.Builder builder) {
    // no-op, for overridding
    return builder;
  }

  //
  // Allow the application to customize the environment
  //
  protected void customize(T configuration, Environment environment) {
  }

  protected  <C> Iterable<BeanEntry<Annotation, C>> locate(BeanLocator locator, Class<C> type) {
    List<BeanEntry<Annotation, C>> components = new ArrayList<>();
    for (BeanEntry<Annotation, C> entry : locator.locate(Key.get(type))) {
      components.add(entry);
    }
    return components;
  }

  private void addAdminHealthCheckEndpoints(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, AdminHealthCheckEndpoint> entry : locate(locator, AdminHealthCheckEndpoint.class)) {
      AdminHealthCheckEndpoint adminHealthCheckEndpoint = entry.getValue();
      AdminHealthCheckEndpoint.addAdminHealthCheckEndpoint(environment.admin(), adminHealthCheckEndpoint);
      logger.debug("Added AdminHealthCheckEndpoint {} at {}.", adminHealthCheckEndpoint.getName(),
          adminHealthCheckEndpoint.getPath());
    }
  }
}
