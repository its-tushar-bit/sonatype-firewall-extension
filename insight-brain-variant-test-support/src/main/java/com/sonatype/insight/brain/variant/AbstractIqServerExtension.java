/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.test.SpringTestExecutionContext;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Single-tenant IQ specialisation of {@link AbstractSpikeServerExtension}. On top of the shared
 * boot-once/reuse machinery it adds the per-test setup/reset that a real resource test needs
 * (data setup, license state, HDS mock, DB reset) and hands the test an {@link IqTestContext} by
 * injection — so the test class needs <b>no base class</b>, only an {@code IqTestContext} field.
 *
 * <p>
 * Concrete variants ({@code IqH2ServerExtension}, {@code IqPostgresServerExtension}) differ only in
 * the database fixture, which they select via {@link #fixtureMarker()} (a class carrying the
 * {@code @H2DiskTest}/{@code @PostgresTest} annotation). The database itself is provisioned through a
 * per-variant {@link OwnedDatabaseContainerRule} — an owned, non-singleton instance cached by
 * {@link #variantKey()} — so the variant tests never read or mutate the JVM-wide
 * {@code DatabaseContainerRule} singleton.
 */
public abstract class AbstractIqServerExtension
    extends AbstractSpikeServerExtension
{
  /** Per-variant, test-owned DB rules keyed by {@link #variantKey()}. Never the JVM-wide singleton. */
  private static final Map<String, OwnedDatabaseContainerRule> RULES = new ConcurrentHashMap<>();

  private IqTestContext currentContext;

  /**
   * The fixture-annotated marker class selecting this variant's database (e.g. a class annotated
   * {@code @H2DiskTest} or {@code @PostgresTest}). Read once, at boot, to provision the fixture.
   */
  protected abstract Class<?> fixtureMarker();

  /** The per-variant owned rule (created on first use), decoupled from {@code DatabaseContainerRule.getInstance()}. */
  protected final OwnedDatabaseContainerRule ownedRule() {
    return RULES.computeIfAbsent(variantKey(), key -> new OwnedDatabaseContainerRule());
  }

  @Override
  protected DatabaseContainer provisionDatabase() {
    // Communicate the fixture annotation to the rule the same way the legacy Spring test bootstrap
    // does — via the (thread-local) execution context — then provision on this (boot) thread. Only
    // one server boots per variant per JVM, so this thread-local is set exactly once.
    SpringTestExecutionContext.setCurrentTestClass(fixtureMarker());
    OwnedDatabaseContainerRule rule = ownedRule();
    rule.ensureInitializedForSpringContext();
    return rule.getDatabaseContainer();
  }

  @Override
  protected void afterServerStarted(final ServerHandle handle) {
    // Seed a non-forced base URL so redirect-building endpoints work on the reused server.
    SpikeSupport.seedBaseUrl(handle.server());
  }

  @Override
  protected void beforeEachTest(final ExtensionContext context, final ServerHandle handle) throws Exception {
    currentContext = new IqTestContext(handle.server(), ownedRule());
    currentContext.beforeTest();
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

  private static void injectContext(final Object testInstance, final IqTestContext context) {
    for (Class<?> type = testInstance.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
      for (Field field : type.getDeclaredFields()) {
        if (IqTestContext.class.isAssignableFrom(field.getType())) {
          try {
            field.setAccessible(true);
            field.set(testInstance, context);
          }
          catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not inject IqTestContext into " + field, e);
          }
        }
      }
    }
  }
}
