/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sonatype.insight.brain.operational.check.AdminHealthCheckEndpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.dropwizard.guice.GuiceBundle.Builder;
import ru.vyarus.dropwizard.guice.module.installer.FeatureInstaller;
import ru.vyarus.dropwizard.guice.module.installer.InstallersOptions;
import ru.vyarus.dropwizard.guice.module.installer.order.Order;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

public abstract class GuiceApplication<T extends Configuration>
    extends Application<T>
{
  private static final Logger logger = LoggerFactory.getLogger(GuiceApplication.class);

  private GuiceBundle guiceBundle;

  private boolean initialized = false;

  public Injector getInjector() {
    return guiceBundle.getInjector();
  }

  public <C> C getInstance(Class<C> type) {
    return getInjector().getInstance(type);
  }

  private static Set<Class<?>> extensions;

  public static final Set<String> PACKAGES_TO_SCAN = ImmutableSet.of("com.sonatype.insight.brain");

  @Override
  public void initialize(final Bootstrap<T> bootstrap) {
    extensions = ConcurrentHashMap.newKeySet();
    Builder builder = customizeGuiceBundle(
        GuiceBundle.builder()
            .enableAutoConfig(PACKAGES_TO_SCAN.toArray(new String[0]))
            .installers(NoMatchInstaller.class)
            .modules(wire(modules()))
            .modulesOverride(new MultiBinderModule(extensions))
            .option(InstallersOptions.ForceSingletonForJerseyExtensions, false)
    );

    // Allow tests to override any existing module
    if (!overrideModules().isEmpty()) {
      builder.modulesOverride(overrideModules().toArray(new Module[0]));
    }

    guiceBundle = builder.build();

    bootstrap.addBundle(guiceBundle);

    super.initialize(bootstrap);
  }

  /**
   * Allows adding override modules in tests to replace bindings.
   *
   * @return
   */
  protected List<Module> overrideModules() {
    return new ArrayList<>();
  }

  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public void run(T configuration, Environment environment) throws Exception {
    customize(configuration, environment);

    // Register admin health check endpoints using Guice Multibinder
    addAdminHealthCheckEndpoints(environment);

    initialized = true;
    extensions = null;
  }

  protected List<Module> modules() {
    return baseModules();
  }

  protected List<Module> baseModules() {
    List<Module> modules = new ArrayList<>();

    // Forces all bindings to be declared in a Module class rather than injected JIT. This module registers the config
    // object by its class name and ancestor class names. Although dropwizard-guicey does the same thing automatically,
    // it appears that we need to do it manually, before Sisu runs, to keep Sisu from automatically doing it incorrectly
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
          bindConfig((Class<? super T>) cls);
        }
        for (Class<?> cls : ClassUtils.getAllInterfaces(configClass)) {
          bindConfig((Class<? super T>) cls);
        }
      }
    });

    // Explicit Guice modules to replace Sisu's automatic @Named discovery
    modules.addAll(getAppModules());

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
   * Returns explicit Guice modules that provide bindings for all @Named components. This replaces Sisu's automatic
   * component discovery via SpaceModule.
   */
  protected abstract List<Module> getAppModules();

  protected DropwizardAwareModule<T> wire(final List<Module> modules) {
    return new DropwizardAwareAggregatingModule<>(modules);
  }

  protected GuiceBundle.Builder customizeGuiceBundle(GuiceBundle.Builder builder) {
    // no-op, for overridding
    return builder;
  }

  //
  // Allow the application to customize the environment
  //
  protected void customize(
      @SuppressWarnings("unused") T configuration,
      @SuppressWarnings("unused") Environment environment)
  {
  }

  /**
   * Register admin health check endpoints using Guice Multibinder. This replaces the previous BeanLocator-based
   * discovery.
   */
  private void addAdminHealthCheckEndpoints(Environment environment) {
    // Get all AdminHealthCheckEndpoint implementations from Guice Multibinder
    Set<AdminHealthCheckEndpoint> healthChecks = getInjector().getInstance(
        Key.get(new TypeLiteral<>() { })
    );

    for (AdminHealthCheckEndpoint endpoint : healthChecks) {
      AdminHealthCheckEndpoint.addAdminHealthCheckEndpoint(environment.admin(), endpoint);
      logger.debug("Added AdminHealthCheckEndpoint {} at {}.", endpoint.getName(), endpoint.getPath());
    }
  }

  /**
   * This installer matches nothing but allows us to capture the scanned classes.
   */
  @Order(0) // Run before any other installer
  public static class NoMatchInstaller
      implements FeatureInstaller
  {
    @Override
    public boolean matches(final Class<?> type) {
      extensions.add(type);
      return false;
    }

    @Override
    public void report() {
      // no-op
    }
  }
}
