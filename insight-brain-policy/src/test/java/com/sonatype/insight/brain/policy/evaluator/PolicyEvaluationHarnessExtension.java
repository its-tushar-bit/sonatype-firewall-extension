/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.lang.reflect.Field;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.db.rule.DatabaseRule;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 (Jupiter) replacement for the ordered JUnit 4 harness on {@link AbstractPolicyEvaluationTest}:
 * {@code @Rule(order = 1) DatabaseRule} then {@code @Rule(order = 2) TemporaryEntity}. Under Jupiter {@code @Rule}
 * is inert, so this reproduces that lifecycle in the same order as Jupiter callbacks.
 *
 * <p>
 * It runs alongside {@code SpringExtension} (which owns the cached {@code ApplicationContext} and populates
 * {@code @Inject} fields) and {@link com.sonatype.insight.test.SpringInjectedTestExtension} (which publishes the
 * {@code SpringTestExecutionContext} bookkeeping). Its {@link #beforeEach} provisions the (reused) database fixture
 * and snapshots the pristine data via {@code TemporaryEntity.before()} BEFORE the base classes' {@code @BeforeEach}
 * ({@code prepareInjectedTestInstance}, then {@code setUp}) run; {@link #afterEach} restores the data via
 * {@code TemporaryEntity.after()}. The database module is single-fixture (in-memory H2), so the reused fixture is
 * provisioned once and never refreshed.
 */
public class PolicyEvaluationHarnessExtension
    implements BeforeEachCallback, AfterEachCallback
{
  @Override
  public void beforeEach(final ExtensionContext context) {
    Object testInstance = context.getRequiredTestInstance();

    // order=1: ensure the (reused) database fixture is provisioned. Idempotent for the shared in-memory fixture.
    DatabaseRule databaseRule = requireField(testInstance, DatabaseRule.class);
    databaseRule.ensureInitializedForSpringContext();

    // order=2: TemporaryEntity.before() snapshots the pristine DB state and initializes the entity builders.
    TemporaryEntity tempEntity = firstField(testInstance, TemporaryEntity.class);
    if (tempEntity != null) {
      tempEntity.before();
    }
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    // Reverse order: TemporaryEntity data restore. The reused DatabaseRule fixture is not torn down per test.
    TemporaryEntity tempEntity = firstField(context.getRequiredTestInstance(), TemporaryEntity.class);
    if (tempEntity != null) {
      tempEntity.after();
    }
  }

  private static <T> T requireField(final Object instance, final Class<T> type) {
    T value = firstField(instance, type);
    if (value == null) {
      throw new IllegalStateException(
          "Expected a non-null " + type.getSimpleName() + " field on " + instance.getClass().getName());
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  private static <T> T firstField(final Object instance, final Class<T> type) {
    for (Class<?> current = instance.getClass(); current != null && current != Object.class; current =
        current.getSuperclass())
    {
      for (Field field : current.getDeclaredFields()) {
        if (type.isAssignableFrom(field.getType())) {
          try {
            field.setAccessible(true);
            Object value = field.get(instance);
            if (value != null) {
              return (T) value;
            }
          }
          catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to read " + type.getSimpleName() + " field for the test harness",
                e);
          }
        }
      }
    }
    return null;
  }
}
