/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.Provider;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Environment;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local copy of SisuService from https://github.com/tesla/dropwizard-sisu with various tweaks for CLM.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class SisuApplication<T extends Configuration>
    extends Application<T>
{
  private static final Logger logger = LoggerFactory.getLogger(SisuApplication.class);

  private final List<Module> initModules = new ArrayList<>();

  private Injector injector = null;

  public Injector getInjector() {
    return injector;
  }

  public <C> C getInstance(Class<C> type) {
    return getInjector().getInstance(type);
  }

  @Override
  public void run(T configuration, Environment environment) throws Exception {
    injector = createInjector(configuration);
    injector.injectMembers(this);
    runWithInjector(configuration, environment, injector);
  }

  private Injector createInjector(final T configuration) {
    List<Module> modules = new ArrayList<>();

    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind((Class) configuration.getClass()).toInstance(configuration);
      }
    });

    modules.addAll(initModules);

    modules.addAll(modules(configuration));

    ClassSpace space = new URLClassSpace(getClass().getClassLoader());
    modules.add(new SpaceModule(space, scanning(configuration)));

    return Guice.createInjector(wire(modules));
  }

  protected Module wire(final List<Module> modules) {
    return new WireModule(modules);
  }

  //
  // Allow the application to customize the scanning
  //
  protected BeanScanning scanning(@SuppressWarnings("unused") T configuration) {
    return BeanScanning.ON;
  }

  //
  // Allow the application to customize the modules
  //
  protected List<Module> modules(@SuppressWarnings("unused") T configuration) {
    return Collections.emptyList();
  }

  //
  // Allow the application to customize the environment
  //
  protected void customize(@SuppressWarnings("unused") T configuration,
                           @SuppressWarnings("unused") Environment environment)
  {
  }

  private void runWithInjector(T configuration, Environment environment, Injector injector) {
    customize(configuration, environment);
    BeanLocator locator = injector.getInstance(BeanLocator.class);
    addHealthChecks(environment, locator);
    addAdminHealthCheckEndpoints(environment, locator);
    addRestComponents(environment, locator);
    addDynamicFeatures(environment, locator);
    addTasks(environment, locator);
    addManaged(environment, locator);
  }

  // Allow modules to be added manually
  public void addModule(Module module) {
    this.initModules.add(module);
  }

  public void addModules(Collection<Module> modules) {
    this.initModules.addAll(modules);
  }

  /**
   * Allows subclasses to exclude components that are present on their classpath but not meant to be used by the
   * application. Especially useful for tests where the classpath is not specific to just one app.
   */
  protected boolean acceptComponent(@SuppressWarnings("unused") Class<?> type) {
    return true;
  }

  protected  <C> Iterable<BeanEntry<Annotation, C>> locate(BeanLocator locator, Class<C> type) {
    List<BeanEntry<Annotation, C>> components = new ArrayList<>();
    for (BeanEntry<Annotation, C> entry : locator.locate(Key.get(type))) {
      if (acceptComponent(entry.getImplementationClass())) {
        components.add(entry);
      }
    }
    return components;
  }

  private void addManaged(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, Managed> managedBeanEntry : locate(locator, Managed.class)) {
      Managed managed = managedBeanEntry.getValue();
      environment.lifecycle().manage(managed);
      logger.debug("Added managed: {}", managed);
    }
  }

  private void addTasks(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, Task> taskBeanEntry : locate(locator, Task.class)) {
      Task task = taskBeanEntry.getValue();
      environment.admin().addTask(task);
      logger.debug("Added task: {}", task);
    }
  }

  private void addHealthChecks(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, HealthCheck> healthCheckBeanEntry : locate(locator, HealthCheck.class)) {
      HealthCheck healthCheck = healthCheckBeanEntry.getValue();
      environment.healthChecks().register(healthCheck.toString(), healthCheck);
      logger.debug("Added healthCheck: {}", healthCheck);
    }
  }

  private void addAdminHealthCheckEndpoints(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, AdminHealthCheckEndpoint> entry : locate(locator, AdminHealthCheckEndpoint.class)) {
      AdminHealthCheckEndpoint adminHealthCheckEndpoint = entry.getValue();
      AdminHealthCheckEndpoint.addAdminHealthCheckEndpoint(environment.admin(), adminHealthCheckEndpoint);
      logger.debug("Added AdminHealthCheckEndpoint {} at {}.", adminHealthCheckEndpoint.getName(),
          adminHealthCheckEndpoint.getPath());
    }
  }

  private void addRestComponents(Environment environment, BeanLocator locator) {
    //
    // Unfortunately JAX-RS annotations are not a qualifier in JSR-330, so we need to check all known bindings.
    // (In practice this isn't that slow because of various caches in Sisu to optimize lookups.)
    // We could always optimize this by introducing a marker interface for injectable resources.
    //
    for (BeanEntry<Annotation, Object> resourceBeanEntry : locate(locator, Object.class)) {
      Class<?> impl = resourceBeanEntry.getImplementationClass();
      if (impl != null && !impl.isAnnotationPresent(MtiqAdminEndpoint.class) &&
          (impl.isAnnotationPresent(Path.class) || impl.isAnnotationPresent(Provider.class))) {
        try {
          Object component = resourceBeanEntry.getValue();
          environment.jersey().register(component);
          logger.debug("Added REST component: {}", component);
        }
        catch (Exception e) {
          logger.warn("Unable to add REST component: {}", impl, e);
        }
      }
    }
  }

  private void addDynamicFeatures(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, DynamicFeature> dynamicFeatureBeanEntry : locate(locator, DynamicFeature.class)) {
      DynamicFeature dynamicFeature = dynamicFeatureBeanEntry.getValue();
      environment.jersey().register(dynamicFeature);
      logger.debug("Added dynamic feature: {}", dynamicFeature);
    }
  }
}
