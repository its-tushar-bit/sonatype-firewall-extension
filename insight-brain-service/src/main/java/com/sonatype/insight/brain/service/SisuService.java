/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.core.spi.component.ioc.IoCInstantiatedComponentProvider;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.Managed;
import com.yammer.dropwizard.tasks.Task;
import com.yammer.metrics.core.HealthCheck;
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
public abstract class SisuService<T extends Configuration>
    extends Service<T>
{
  private static final Logger logger = LoggerFactory.getLogger(SisuService.class);

  private final List<Module> initModules = new ArrayList<Module>();

  private Injector injector = null;

  public Injector getInjector() {
    return injector;
  }

  @Override
  public void run(T configuration, Environment environment) throws Exception {
    injector = createInjector(configuration);
    injector.injectMembers(this);
    runWithInjector(configuration, environment, injector);
  }

  private Injector createInjector(final T configuration) {
    List<Module> modules = new ArrayList<Module>();

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

    return Guice.createInjector(new WireModule(modules));
  }

  //
  // Allow the application to customize the scanning
  //
  protected BeanScanning scanning(T configuration) {
    return BeanScanning.ON;
  }

  //
  // Allow the application to customize the modules
  //
  protected List<Module> modules(T configuration) {
    return Collections.emptyList();
  }

  //
  // Allow the application to customize the environment
  //
  protected void customize(T configuration, Environment environment) {
  }

  private void runWithInjector(T configuration, Environment environment, Injector injector) {
    customize(configuration, environment);
    BeanLocator locator = injector.getInstance(BeanLocator.class);
    environment.addProvider(new SisuComponentProviderFactory(locator));
    addHealthChecks(environment, locator);
    addProviders(environment, locator);
    addInjectableProviders(environment, locator);
    addResources(environment, locator);
    addResourceFilterFactories(environment, locator);
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

  private static void addManaged(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, Managed> managedBeanEntry : locator.locate(Key.get(Managed.class))) {
      Managed managed = managedBeanEntry.getValue();
      environment.manage(managed);
      logger.debug("Added managed: {}", managed);
    }
  }

  private static void addTasks(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, Task> taskBeanEntry : locator.locate(Key.get(Task.class))) {
      Task task = taskBeanEntry.getValue();
      environment.addTask(task);
      logger.debug("Added task: {}", task);
    }
  }

  private static void addHealthChecks(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, HealthCheck> healthCheckBeanEntry : locator.locate(Key.get(HealthCheck.class))) {
      HealthCheck healthCheck = healthCheckBeanEntry.getValue();
      environment.addHealthCheck(healthCheck);
      logger.debug("Added healthCheck: {}", healthCheck);
    }
  }

  private static void addInjectableProviders(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, InjectableProvider> injectableProviderBeanEntry : locator.locate(Key
        .get(InjectableProvider.class))) {
      InjectableProvider injectableProvider = injectableProviderBeanEntry.getValue();
      environment.addProvider(injectableProvider);
      logger.debug("Added injectableProvider: {}", injectableProvider);
    }
  }

  private static void addProviders(Environment environment, BeanLocator locator) {
    for (BeanEntry<Annotation, Provider> providerBeanEntry : locator.locate(Key.get(Provider.class))) {
      Provider provider = providerBeanEntry.getValue();
      environment.addProvider(provider);
      logger.debug("Added provider: {}", provider);
    }
  }

  private static void addResources(Environment environment, BeanLocator locator) {
    //
    // Unfortunately @Path is not a qualifier in JSR330, so we need to check all known bindings.
    // (In practice this isn't that slow because of various caches in Sisu to optimize lookups.)
    // We could always optimize this by introducing a marker interface for injectable resources.
    //
    for (BeanEntry<Annotation, Object> resourceBeanEntry : locator.locate(Key.get(Object.class))) {
      Class<?> impl = resourceBeanEntry.getImplementationClass();
      if (impl != null && impl.isAnnotationPresent(Path.class)) {
        try {
          /*
           * NOTE: Not using addResource(Object) to avoid https://java.net/jira/browse/JERSEY-692 and not using explicit
           * root resources to avoid https://java.net/jira/browse/JERSEY-2141. Instead, SisuComponentProviderFactory
           * teaches Jersey how to instantiante the resource.
           */
          environment.addResource(impl);
          logger.debug("Added resource: {}", impl);
        }
        catch (Exception e) {
          logger.warn("Unable to add resource: {}", impl, e);
        }
      }
    }
  }

  private static void addResourceFilterFactories(Environment environment, BeanLocator locator) {
    List<ResourceFilterFactory> resourceFilterFactories = new ArrayList<ResourceFilterFactory>();
    for (BeanEntry<Annotation, ResourceFilterFactory> beanEntry : locator.locate(Key.get(ResourceFilterFactory.class))) {
      ResourceFilterFactory resourceFilterFactory = beanEntry.getValue();
      logger.debug("Added resource filter factory: {}", resourceFilterFactory);
      resourceFilterFactories.add(resourceFilterFactory);
    }
    if (!resourceFilterFactories.isEmpty()) {
      environment.setJerseyProperty(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, resourceFilterFactories);
    }
  }

  private static class SisuComponentProviderFactory
      implements IoCComponentProviderFactory
  {
    private final BeanLocator container;

    public SisuComponentProviderFactory(final BeanLocator container) {
      this.container = container;
    }

    @Override
    public IoCComponentProvider getComponentProvider(final Class<?> type) {
      IoCComponentProvider provider = null;

      Iterator<BeanEntry<Annotation, ?>> iter = container.locate(Key.get((Class) type)).iterator();
      if (iter.hasNext()) {
        final BeanEntry entry = iter.next();

        provider = new IoCInstantiatedComponentProvider()
        {
          @Override
          public Object getInjectableInstance(final Object obj) {
            return obj;
          }

          @Override
          public Object getInstance() {
            return entry.getValue();
          }
        };
      }

      return provider;
    }

    @Override
    public IoCComponentProvider getComponentProvider(final ComponentContext context, final Class<?> type) {
      return getComponentProvider(type);
    }
  }
}
