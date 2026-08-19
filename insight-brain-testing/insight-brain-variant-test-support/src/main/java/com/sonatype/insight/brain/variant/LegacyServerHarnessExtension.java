/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.search.SearchIndexRule;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 replacement for the ordered JUnit 4 {@code @Rule} stack declared by the own-server integration
 * chain ({@code AbstractBaseIntegrationTest -> AbstractBrainServiceIntegrationTest -> AbstractResourceTest /
 * AbstractAuditTest / ...}). Those rules are inert under the Jupiter engine, so this extension reproduces
 * their {@code before()}/{@code after()} lifecycle as Jupiter callbacks.
 *
 * <p>
 * The chain's own {@code @Before}/{@code @After} methods ({@code initTest} — which boots/reuses the
 * {@code TestCLMServer} — and {@code cleanupTest}) run via their dual {@code @BeforeEach}/{@code @AfterEach}
 * annotations; this extension only drives the rule fields and seeds the {@code @Rule TestName} (inert under
 * Jupiter). The Jupiter lifecycle is:
 * <ol>
 * <li><b>this extension's {@link #beforeEach}</b> — seed {@code TestName}, provision the (reused)
 * {@code DatabaseContainerRule} (order=1) and {@code SearchIndexRule} (order=2), run the remaining aux
 * {@link ExternalResource} rules ({@code QuartzJobSchedulingServiceRule}, {@code TemporaryFolder},
 * {@code MockCleaner}, {@code TestProductLicenseRule}) then {@code TemporaryEntity.before()} (the DB
 * snapshot).</li>
 * <li>the chain's {@code @BeforeEach} methods (super&rarr;sub): {@code initTest} (mocks + HDS + server boot),
 * then the intermediate/leaf setup.</li>
 * </ol>
 * Teardown runs in reverse: the chain {@code @AfterEach} methods (leaf&rarr;super, incl. {@code disableSso}
 * and {@code cleanupTest} which closes the Mockito session and resets the reused fixture), then this
 * extension's {@link #afterEach} runs {@code TemporaryEntity.after()} (data restore) + the aux rule
 * {@code after()} in reverse.
 *
 * <p>
 * Fields annotated {@link RegisterExtension} (e.g. a {@code LogOutput} a leaf registers itself) are skipped —
 * Jupiter already drives those.
 */
public class LegacyServerHarnessExtension
    implements BeforeEachCallback, AfterEachCallback
{
  private static final Logger log = LoggerFactory.getLogger(LegacyServerHarnessExtension.class);

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();

    // (0) Seed the @Rule TestName (inert under Jupiter, so getMethodName() would be null) so
    // isTestUsingManualServerInit() and the chain's log lines resolve the running method.
    seedTestName(testInstance, context.getRequiredTestMethod().getName());

    // (1) order=1 DatabaseContainerRule then order=2 SearchIndexRule: ensure the reused fixtures exist.
    DatabaseContainerRule databaseContainerRule = firstFieldValue(testInstance, DatabaseContainerRule.class);
    if (databaseContainerRule != null) {
      databaseContainerRule.ensureInitializedForSpringContext();
    }
    SearchIndexRule searchIndexRule = firstFieldValue(testInstance, SearchIndexRule.class);
    if (searchIndexRule != null) {
      searchIndexRule.ensureInitializedForSpringContext();
    }

    // (2) The remaining aux ExternalResource rules, in field-declaration order so the DB-dependent
    // TestProductLicenseRule (order=2) runs after the database fixture above.
    for (ExternalResource aux : auxExternalResources(testInstance)) {
      invokeLifecycle(aux, "before");
    }

    // (3) order=2 TemporaryEntity.before(): snapshot the pristine DB state before the @Before chain.
    TemporaryEntity tempEntity = firstFieldValue(testInstance, TemporaryEntity.class);
    if (tempEntity != null) {
      tempEntity.before();
    }
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    Object testInstance = context.getRequiredTestInstance();

    TemporaryEntity tempEntity = firstFieldValue(testInstance, TemporaryEntity.class);
    try {
      // TemporaryEntity.after() restores the pristine DB snapshot; a failure here leaks state into later
      // tests, so let it surface (do NOT swallow) — mirroring the JUnit 4 rule machinery, where a failing
      // TemporaryEntity.after() is reported as a test error.
      if (tempEntity != null) {
        tempEntity.after();
      }
    }
    finally {
      // Aux-rule after() failures stay quiet (best-effort teardown, mirroring JUnit 4 "run all, report first").
      List<ExternalResource> aux = auxExternalResources(testInstance);
      Collections.reverse(aux);
      for (ExternalResource resource : aux) {
        runQuietly(resource.getClass().getSimpleName() + ".after", () -> invokeLifecycle(resource, "after"));
      }
    }
  }

  private static void seedTestName(final Object testInstance, final String methodName) {
    for (Field field : allFields(testInstance.getClass())) {
      if (TestName.class.isAssignableFrom(field.getType())) {
        setField(testInstance, field, new TestName()
        {
          @Override
          public String getMethodName() {
            return methodName;
          }
        });
        return;
      }
    }
  }

  /**
   * All {@link ExternalResource} rule fields on the instance EXCEPT the ones driven explicitly in
   * {@link #beforeEach} — {@link DatabaseContainerRule} (order=1), {@link SearchIndexRule} (order=2) and
   * {@link TemporaryEntity} — and any field a leaf registers itself via {@link RegisterExtension} (Jupiter
   * drives those). Covers {@code QuartzJobSchedulingServiceRule}, {@code TemporaryFolder}, {@code MockCleaner}
   * and {@code TestProductLicenseRule}.
   */
  private static List<ExternalResource> auxExternalResources(final Object testInstance) {
    List<ExternalResource> resources = new ArrayList<>();
    for (Field field : allFields(testInstance.getClass())) {
      if (!ExternalResource.class.isAssignableFrom(field.getType())) {
        continue;
      }
      if (TemporaryEntity.class.isAssignableFrom(field.getType())
          || DatabaseContainerRule.class.isAssignableFrom(field.getType())
          || SearchIndexRule.class.isAssignableFrom(field.getType())
          || field.isAnnotationPresent(RegisterExtension.class))
      {
        continue;
      }
      Object value = fieldValue(testInstance, field);
      if (value instanceof ExternalResource resource) {
        resources.add(resource);
      }
    }
    return resources;
  }

  @SuppressWarnings("unchecked")
  private static <T> T firstFieldValue(final Object instance, final Class<T> type) {
    for (Field field : allFields(instance.getClass())) {
      if (!type.isAssignableFrom(field.getType())) {
        continue;
      }
      Object value = fieldValue(instance, field);
      if (value != null) {
        return (T) value;
      }
    }
    return null;
  }

  private static Object fieldValue(final Object instance, final Field field) {
    try {
      field.setAccessible(true);
      return field.get(instance);
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException("Could not read field " + field, e);
    }
  }

  private static void setField(final Object instance, final Field field, final Object value) {
    try {
      field.setAccessible(true);
      field.set(instance, value);
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException("Could not set field " + field, e);
    }
  }

  private static List<Field> allFields(final Class<?> type) {
    List<Field> fields = new ArrayList<>();
    for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
      Collections.addAll(fields, current.getDeclaredFields());
    }
    return fields;
  }

  /**
   * Reflectively invoke the {@code protected} {@code before()}/{@code after()} lifecycle of a JUnit 4
   * {@link ExternalResource} (they have no public entry point outside the rule machinery).
   */
  private static void invokeLifecycle(final ExternalResource resource, final String methodName) {
    Method method = findNoArgMethod(resource.getClass(), methodName);
    if (method == null) {
      return;
    }
    try {
      method.setAccessible(true);
      method.invoke(resource);
    }
    catch (ReflectiveOperationException e) {
      Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getTargetException() : e;
      throw new IllegalStateException(
          "Failed to invoke " + methodName + "() on rule " + resource.getClass().getName(), cause);
    }
  }

  private static Method findNoArgMethod(final Class<?> type, final String name) {
    for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
      try {
        return current.getDeclaredMethod(name);
      }
      catch (NoSuchMethodException ignored) {
        // walk up
      }
    }
    return null;
  }

  private static void runQuietly(final String description, final ThrowingRunnable action) {
    try {
      action.run();
    }
    catch (Exception e) {
      log.warn("Legacy server test harness cleanup step '{}' failed: {}", description, e.getMessage());
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable
  {
    void run() throws Exception;
  }
}
