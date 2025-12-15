/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.apache.shiro.realm.Realm;

/**
 * Automatically create multi bindings to avoid needing to explicitly define lists. Explicitly listing them could easily
 * lead to missed background Jobs.
 */
public class MultiBinderModule
    extends AbstractModule
{
  // jakarta.inject.Provider.class is excluded but commented out
  private static final Set<Class<?>> EXCLUDED_CLASSES = ImmutableSet.of(Provider.class, Realm.class);

  private Set<Class<?>> scannedClasses;

  public MultiBinderModule(final Set<Class<?>> scannedClasses) {
    this.scannedClasses = scannedClasses;
  }

  @Override
  protected void configure() {
    // Automatically create multibindings
    // Note that @InvisibleForScanner classes need their multibindings setup some other way
    Set<Class<?>> fromClasses = new HashSet<>();
    for (Class<?> clazz : scannedClasses) {
      addClassAndInterfaces(clazz, fromClasses);
    }

    fromClasses.removeAll(EXCLUDED_CLASSES);

    for (Class<?> from : fromClasses) {
      if (shouldSkip(from)) {
        continue;
      }

      for (Class<?> to : scannedClasses) {
        if (!from.equals(to) && from.isAssignableFrom(to) && isMultiBinderTarget(to)) {
          if (Provider.class.isAssignableFrom(to) /*|| jakarta.inject.Provider.class.isAssignableFrom(to)*/) {
            continue;
          }
          Multibinder<?> multibinder = Multibinder.newSetBinder(binder(), from);
          multibinder.permitDuplicates();

          multibinder.addBinding().to((Class) to);
        }
      }
    }
    scannedClasses = null;
  }

  private boolean isMultiBinderTarget(final Class<?> clazz) {
    if (isInterfaceOrAbstract(clazz)) {
      return false;
    }
    if (clazz.isAnnotationPresent(Named.class) || clazz.isAnnotationPresent(Singleton.class)) {
      return true;
    }
    for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
      if (constructor.isAnnotationPresent(Inject.class)) {
        return true;
      }
    }
    return false;
  }

  private boolean isInterfaceOrAbstract(final Class<?> clazz) {
    return clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers());
  }

  private boolean shouldSkip(final Class<?> from) {
    for (Class<?> excludedClass : EXCLUDED_CLASSES) {
      if (excludedClass.isAssignableFrom(from)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Recursively add a class and all its superclasses and interfaces (including parent interfaces) to the set.
   */
  private void addClassAndInterfaces(Class<?> clazz, Set<Class<?>> result) {
    if (clazz == null || clazz == Object.class || !result.add(clazz)) {
      // Already processed or reached the end
      return;
    }

    // Add all directly implemented interfaces and their parent interfaces
    for (Class<?> iface : clazz.getInterfaces()) {
      addClassAndInterfaces(iface, result);
    }

    // Walk up the superclass hierarchy
    addClassAndInterfaces(clazz.getSuperclass(), result);
  }
}
