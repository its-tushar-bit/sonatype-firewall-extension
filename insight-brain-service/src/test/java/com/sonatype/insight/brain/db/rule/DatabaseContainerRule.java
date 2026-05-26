/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.rule;

import com.sonatype.insight.brain.db.DatabaseConfigProvider;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.TestDatabaseContainer;
import com.sonatype.insight.brain.db.fixture.h2.H2DiskDatabaseFixture;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.test.SpringTestExecutionContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * <p>
 * Extends the {@link DatabaseRule} (see those javadocs) with the {@link DatabaseContainer} required for the main
 * application.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * @Rule(order = 1)
 * public DatabaseContainerRule databaseRule = DatabaseContainerRule.getInstance();
 * }
 * </pre>
 * </p>
 * <p>
 * See also the javadoc in {@link DatabaseRule}
 * </p>
 */
public class DatabaseContainerRule
    extends DatabaseRule
{
  private static final DatabaseContainerRule INSTANCE = new DatabaseContainerRule();

  private static Class<?> currentTestClassType;

  private TestDatabaseContainer databaseContainer;

  private boolean springContextFixturePrepared;

  private DatabaseType springContextFixtureType;

  private Class<?> springContextTestClass;

  private String springContextTestName;

  private boolean springContextAnnotationFromMethod;

  protected DatabaseContainerRule() {
    // private constructor for singleton enforcement
  }

  /**
   * Return the singleton {@link DatabaseContainer}
   *
   * @param baseTestClassType you should pass in the BASE test class type here. The value is tracked between subsequent
   *          tests and when the value changes it is considered as making the currently active database
   *          NOT reusable and therefore a fresh database will be automatically be re-provisioned
   */
  public static DatabaseContainerRule getInstance(Class<?> baseTestClassType) {
    if (currentTestClassType != baseTestClassType) {
      INSTANCE.markFixtureAsDirty();
      currentTestClassType = baseTestClassType;
    }

    return INSTANCE;
  }

  public synchronized void ensureInitializedForSpringContext() {
    applySpringExecutionContext();

    try {
      before();
      preserveSpringContextFixtureIfNeeded();
      previousType = type;
      isNewFixtureForCurrentTest = false;
      springContextFixturePrepared = true;
      springContextFixtureType = type;
      springContextTestClass = currentTestClass;
      springContextTestName = testName;
      springContextAnnotationFromMethod =
          hasMethodLevelFixtureAnnotation(SpringTestExecutionContext.getCurrentTestMethod());
    }
    catch (Throwable t) {
      throw new IllegalStateException("Failed to initialize DatabaseContainerRule for Spring test context", t);
    }
  }

  private void applySpringExecutionContext() {
    Class<?> springTestClass = SpringTestExecutionContext.getCurrentTestClass();
    Method springTestMethod = SpringTestExecutionContext.getCurrentTestMethod();

    if (springTestClass == null && springTestMethod == null) {
      return;
    }

    annotation = resolveFixtureAnnotation(springTestMethod, springTestClass);
    testName = springTestMethod != null ? springTestMethod.getName() : springTestClass.getSimpleName();
    currentTestClass = springTestClass;
  }

  private Annotation resolveFixtureAnnotation(final Method springTestMethod, final Class<?> springTestClass) {
    Annotation methodAnnotation = resolveMethodFixtureAnnotation(springTestMethod);
    if (methodAnnotation != null) {
      return methodAnnotation;
    }

    if (springTestClass != null) {
      for (Annotation candidate : springTestClass.getAnnotations()) {
        if (DatabaseRuleAnnotations.ANNOTATION_TYPES.contains(candidate.annotationType())) {
          return candidate;
        }
      }
    }

    return null;
  }

  private Annotation resolveMethodFixtureAnnotation(final Method springTestMethod) {
    if (springTestMethod == null) {
      return null;
    }
    for (Annotation candidate : springTestMethod.getAnnotations()) {
      if (DatabaseRuleAnnotations.ANNOTATION_TYPES.contains(candidate.annotationType())) {
        return candidate;
      }
    }
    return null;
  }

  private boolean hasMethodLevelFixtureAnnotation(final Method springTestMethod) {
    return resolveMethodFixtureAnnotation(springTestMethod) != null;
  }

  @Override
  protected void before() throws Throwable {
    if (shouldReuseSpringContextFixture()) {
      return;
    }

    springContextFixturePrepared = false;
    super.before();

    if (hasFixtureTypeChanged() || !isFixtureReusable()) {
      this.databaseContainer = createTestDatabaseContainer();
    }
  }

  private void preserveSpringContextFixtureIfNeeded() {
    if (fixture instanceof H2DiskDatabaseFixture h2DiskDatabaseFixture) {
      h2DiskDatabaseFixture.setReusableForSpringContext(true);
    }
  }

  private boolean shouldReuseSpringContextFixture() {
    if (!springContextFixturePrepared || isCurrentFixtureDirty) {
      return false;
    }

    DatabaseType currentFixtureType = getType();
    boolean sameClass = isSameTestClass(springContextTestClass, currentTestClass);
    boolean sameScope = springContextAnnotationFromMethod
        ? sameClass && Objects.equals(springContextTestName, testName)
        : sameClass;

    if (springContextFixtureType == currentFixtureType && sameScope) {
      type = currentFixtureType;
      return true;
    }

    springContextFixturePrepared = false;
    return false;
  }

  private boolean isSameTestClass(final Class<?> left, final Class<?> right) {
    if (left == right) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.getName().equals(right.getName());
  }

  private TestDatabaseContainer createTestDatabaseContainer() {
    log.info("Creating new test DatabaseContainer");
    return new TestDatabaseContainer(getDataSourceProvider(), this);
  }

  public DatabaseContainer getDatabaseContainer() {
    return databaseContainer;
  }

  public DatabaseConfigProvider getDatabaseConfigProvider() {
    return new DatabaseConfigProvider()
    {
      @Override
      public DatabaseConfig getDatabaseConfig(final DatabaseName databaseName) {
        return DatabaseContainerRule.this.getDatabaseConfig(databaseName.name());
      }

      @Override
      public DatabaseEngine getDatabaseEngine() {
        return getDatabaseEngine();
      }
    };
  }

  public void resetMocks() {
    databaseContainer.resetMocks();
  }
}
