/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;

/**
 * Pure Guice test base class that replaces Sisu's InjectedTest. Provides dependency injection for tests without
 * requiring Sisu's classpath scanning.
 *
 * <p>
 * This class separates production modules from test modules and supports overriding
 * bindings via Guice's Modules.override() mechanism. This prevents binding conflicts when tests need to replace
 * production bindings with test implementations.
 * </p>
 */
public abstract class GuiceInjectedTest
{
  private Injector injector;

  /**
   * Set up the Guice injector and inject dependencies into the test instance. Override this method in subclasses but
   * make sure to call super.setUp().
   */
  public void setUp() throws Exception {
    List<Module> productionModules = getProductionModulesForTest();
    Module testModule = getOverrideModule();

    // Create the combined module
    Module combinedModule;
    if (productionModules.isEmpty()) {
      // No production modules, just use test module
      combinedModule = testModule;
    }
    else if (testModule != null) {
      // Use Modules.override() to allow test module to replace production bindings
      combinedModule = Modules.override(productionModules).with(testModule);
    }
    else {
      // Only production modules
      combinedModule = Modules.combine(productionModules);
    }

    Set<Class<?>> extensions = new HashSet<>();
    List<Element> elements = Elements.getElements(combinedModule);
    for (Element element : elements) {
      element.acceptVisitor(new DefaultElementVisitor<Void>()
      {
        @Override
        public <T> Void visit(Binding<T> binding) {
          Key<T> key = binding.getKey();
          TypeLiteral<T> typeLiteral = key.getTypeLiteral();
          Class<? super T> rawType = typeLiteral.getRawType();

          extensions.add(rawType);

          return null;
        }
      });
    }

    Module multiBinderModule = getMultiBinderModule(extensions);
    if (multiBinderModule != null) {
      combinedModule = Modules.override(combinedModule).with(multiBinderModule);
    }

    // Create injector with the combined module
    // Use DEVELOPMENT stage for tests to avoid eager singleton initialization
    // This prevents instantiation of production singletons (like HdsClient) before test setup
    injector = Guice.createInjector(Stage.DEVELOPMENT, combinedModule);

    // Inject members into the test instance
    injector.injectMembers(this);
  }

  protected Module getMultiBinderModule(final Set<Class<?>> extensions) {
    return null;
  }

  /**
   * Tear down the test. Override in subclasses if cleanup is needed.
   */
  public void tearDown() throws Exception {
    // No cleanup needed by default
  }

  /**
   * Get production modules to install. Subclasses can override this to provide production modules that should be loaded
   * for the test.
   *
   * @return list of production modules, or empty list if none needed
   */
  protected List<Module> getProductionModulesForTest() {
    return new ArrayList<>();
  }

  /**
   * Get test-specific module with bindings that may override production bindings. This module will be applied using
   * Modules.override() if production modules exist.
   * <p>
   * Subclasses should override this to provide test-specific bindings.
   *
   * @return test module, or null if no test bindings needed
   */
  protected Module getOverrideModule() {
    return null;
  }

  /**
   * Look up a component from the injector by class.
   */
  protected final <T> T lookup(Class<T> type) {
    return injector.getInstance(type);
  }
}
