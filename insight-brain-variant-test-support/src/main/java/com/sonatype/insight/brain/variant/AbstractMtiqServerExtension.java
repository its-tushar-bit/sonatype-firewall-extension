/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.lang.reflect.Field;

import com.sonatype.insight.brain.db.rule.MultiTenantDatabaseContainerRule;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Multi-tenant (MTIQ) specialisation of {@link AbstractSpikeServerExtension}, the tenant-aware analog of
 * {@link AbstractIqServerExtension}. On top of the shared boot-once/reuse machinery it adds the per-test
 * setup/reset a real tenant-scoped resource test needs — provisioning a fresh tenant, running data setup
 * and the test body under that tenant, and restoring the global tenant on teardown — and hands the test an
 * {@link MtiqTestContext} by injection, so the test class needs <b>no base class</b>, only an
 * {@code MtiqTestContext} field.
 *
 * <p>
 * The database fixture is the shared multi-tenant embedded-postgres cluster
 * ({@link MultiTenantDatabaseContainerRule#getInstance()}); the concrete extension
 * ({@link MtiqServerExtension}) supplies the multi-tenant service factory, configurator and test
 * configurations.
 */
public abstract class AbstractMtiqServerExtension
    extends AbstractSpikeServerExtension
{
  private MtiqTestContext currentContext;

  /** The shared multi-tenant database container rule (a JVM-wide singleton for the MTIQ variant). */
  protected final MultiTenantDatabaseContainerRule mtiqDatabaseRule() {
    return MultiTenantDatabaseContainerRule.getInstance();
  }

  @Override
  protected void beforeEachTest(final ExtensionContext context, final ServerHandle handle) throws Exception {
    currentContext = new MtiqTestContext(handle.server(), mtiqDatabaseRule());
    currentContext.beforeTest(context.getRequiredTestMethod().getName());
    injectContext(context.getRequiredTestInstance(), currentContext);
  }

  @Override
  protected void afterEachTest(final ExtensionContext context, final ServerHandle handle) {
    if (currentContext != null) {
      try {
        currentContext.afterTest();
      }
      finally {
        currentContext = null;
      }
    }
  }

  private static void injectContext(final Object testInstance, final MtiqTestContext context) {
    for (Class<?> type = testInstance.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
      for (Field field : type.getDeclaredFields()) {
        if (MtiqTestContext.class.isAssignableFrom(field.getType())) {
          try {
            field.setAccessible(true);
            field.set(testInstance, context);
          }
          catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not inject MtiqTestContext into " + field, e);
          }
        }
      }
    }
  }
}
