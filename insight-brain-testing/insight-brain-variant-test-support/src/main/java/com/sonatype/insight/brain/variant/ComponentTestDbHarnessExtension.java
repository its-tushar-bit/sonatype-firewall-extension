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
import com.sonatype.insight.test.SpringTestExecutionContext;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 replacement for the ordered JUnit 4 {@code @Rule} DB/search/entity/Mockito harness that
 * the {@code SpringInjectedTest -> SpringBrainInjectedTest -> AbstractComponentTest} chain declares.
 *
 * <p>
 * Under JUnit 4 that chain relies on a strictly ordered rule stack —
 * {@code DatabaseContainerRule(order=1)} → {@code SearchIndexRule(order=2)} →
 * {@code TemporaryEntity(order=3)} plus the (unordered, outer) {@code MockitoRule}, {@code MockCleaner},
 * {@code DatamartUpdaterState}, {@code QuartzJobSchedulingServiceRule} and {@code TemporaryFolder}
 * rules — none of which fire under the Jupiter engine ({@code @Rule} is inert there). This one
 * composite extension reproduces that lifecycle in the SAME order, as Jupiter callbacks, so a
 * component test can run under Jupiter with identical per-test setup/reset semantics.
 *
 * <p>
 * <b>Ordering is guaranteed by doing everything in ONE extension</b> rather than several
 * {@code @Order}ed extensions: coordinating multiple extensions against {@link org.springframework
 * .test.context.junit.jupiter.SpringExtension} (which owns the cached {@code ApplicationContext} and
 * runs its {@code TestInstancePostProcessor}/{@code BeforeEachCallback} at fixed phases) is fragile.
 * The Jupiter lifecycle around this extension is:
 * <ol>
 * <li>{@code SpringExtension.postProcessTestInstance} — populates {@code @Inject}/{@code @Autowired}
 * fields from the (cached) context.</li>
 * <li><b>this extension's {@link #beforeEach}</b> — reproduces the rule {@code before()} stack:
 * outer aux {@code ExternalResource}s + Mockito {@code @Mock} init, then DB, then search, then
 * {@code TemporaryEntity.before()} (the data snapshot).</li>
 * <li>the base classes' {@code @BeforeEach} methods (super→sub): {@code prepareInjectedTestInstance}
 * (bean-state snapshot + mock wiring), {@code initializeSpringBrainInjectedTestHarness}
 * ({@code daoFactory}/static injection), {@code beforeTest} (component setup), then the leaf's own
 * {@code @BeforeEach}.</li>
 * </ol>
 * Teardown runs in reverse: the base {@code @AfterEach} methods (leaf→super, incl.
 * {@code afterTest} and {@code restoreInjectedTestInstance}) fire first, then this extension's
 * {@link #afterEach} runs {@code TemporaryEntity.after()} (data restore) + mock/aux resets.
 *
 * <p>
 * <b>Context reuse:</b> this extension never builds or dirties the Spring context — {@code
 * SpringExtension} caches ONE {@code ApplicationContext} per merged {@code @ContextConfiguration}
 * key across every class in the (single-fixture) module, exactly as it does for any Jupiter Spring
 * test. Because the module is single-database (all H2 in-memory by default), the JVM-wide {@code
 * DatabaseContainerRule} singleton — which {@code SpringTestConfiguration} wires its datastore beans
 * from — is provisioned once and reused; there is no fixture-signature change to refresh on. That is
 * why this harness intentionally uses the same singleton the context beans use rather than an
 * {@code OwnedDatabaseContainerRule}: the {@code TemporaryEntity}/data reset must operate on the very
 * same {@code DatabaseContainer} the injected beans read from.
 */
public class ComponentTestDbHarnessExtension
    implements BeforeEachCallback, AfterEachCallback
{
  private static final Logger log = LoggerFactory.getLogger(ComponentTestDbHarnessExtension.class);

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(ComponentTestDbHarnessExtension.class);

  private static final String MOCKS_KEY = "mockitoCloseable";

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();
    Class<?> testClass = testInstance.getClass();

    // (0) Publish the current test class/instance so the DB/search rules resolve the fixture the
    // same way the legacy Spring bootstrap does, and so any customizeConfig probe can see it. The
    // module is single-fixture, so this never triggers a context refresh. Subclasses may override
    // #fixtureClass to publish a fixture-marker class (e.g. one annotated @PostgresTest) so the
    // JVM-wide DatabaseContainerRule resolves a non-default fixture type for the whole module.
    SpringTestExecutionContext.setCurrentTestClass(fixtureClass(testClass));
    SpringTestExecutionContext.setCurrentTestInstance(testInstance);
    SpringTestExecutionContext.clearCurrentTestMethod();

    // (1) Outer, unordered rules first (MockitoRule/MockCleaner/DatamartUpdaterState/Quartz/
    // TemporaryFolder are all unordered => outermost in the JUnit 4 stack), then @Mock init.
    for (ExternalResource aux : auxExternalResources(testInstance)) {
      invokeLifecycle(aux, "before");
    }
    context.getStore(NAMESPACE).put(MOCKS_KEY, MockitoAnnotations.openMocks(testInstance));

    // (2) order=1 DatabaseContainerRule: ensure the (reused) fixture is provisioned. Idempotent —
    // the cached Spring context already provisioned it; this takes the reuse path.
    DatabaseContainerRule databaseContainerRule = requireFieldValue(testInstance, DatabaseContainerRule.class,
        "databaseContainerRule");
    databaseContainerRule.ensureInitializedForSpringContext();

    // (3) order=2 SearchIndexRule.
    SearchIndexRule searchIndexRule = firstFieldValue(testInstance, SearchIndexRule.class);
    if (searchIndexRule != null) {
      searchIndexRule.ensureInitializedForSpringContext();
    }

    // (4) order=3 TemporaryEntity.before() — snapshots the pristine DB state and initializes the
    // entity builders. Runs after DB/search are ready and before the base @BeforeEach chain.
    TemporaryEntity tempEntity = firstFieldValue(testInstance, TemporaryEntity.class);
    if (tempEntity != null) {
      tempEntity.before();
    }
  }

  /**
   * The class whose fixture annotation ({@code @H2InMemoryTest}/{@code @H2DiskTest}/{@code @PostgresTest})
   * selects this module's database fixture type. The default returns the running test class itself, so
   * an un-annotated component test resolves to the default in-memory H2 fixture. Subclasses (e.g. the
   * Postgres harness) return a stable marker class carrying the desired fixture annotation.
   */
  protected Class<?> fixtureClass(final Class<?> testClass) {
    return testClass;
  }

  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();
    try {
      // Reverse order: TemporaryEntity data restore first, then reset mocks, then aux rules
      // (reverse), then close the Mockito session.
      TemporaryEntity tempEntity = firstFieldValue(testInstance, TemporaryEntity.class);
      if (tempEntity != null) {
        runQuietly("TemporaryEntity.after", tempEntity::after);
      }

      DatabaseContainerRule databaseContainerRule = firstFieldValue(testInstance, DatabaseContainerRule.class);
      if (databaseContainerRule != null) {
        runQuietly("DatabaseContainerRule.resetMocks", databaseContainerRule::resetMocks);
      }

      List<ExternalResource> aux = auxExternalResources(testInstance);
      Collections.reverse(aux);
      for (ExternalResource resource : aux) {
        runQuietly(resource.getClass().getSimpleName() + ".after", () -> invokeLifecycle(resource, "after"));
      }

      Object mocks = context.getStore(NAMESPACE).remove(MOCKS_KEY);
      if (mocks instanceof AutoCloseable closeable) {
        runQuietly("Mockito.close", closeable::close);
      }
    }
    finally {
      SpringTestExecutionContext.clearCurrentTestInstance();
      SpringTestExecutionContext.clearCurrentTestMethod();
      SpringTestExecutionContext.clearCurrentTestClass();
    }
  }

  /**
   * All {@link ExternalResource} rule fields on the instance EXCEPT the DB/search/entity rules,
   * which this extension drives explicitly. Covers {@code MockCleaner}, {@code DatamartUpdaterState},
   * {@code QuartzJobSchedulingServiceRule}, {@code TemporaryFolder} and any leaf-declared
   * {@code ExternalResource} rule.
   */
  private static List<ExternalResource> auxExternalResources(final Object testInstance) {
    List<ExternalResource> resources = new ArrayList<>();
    for (Field field : allFields(testInstance.getClass())) {
      if (!ExternalResource.class.isAssignableFrom(field.getType())) {
        continue;
      }
      if (DatabaseContainerRule.class.isAssignableFrom(field.getType())
          || SearchIndexRule.class.isAssignableFrom(field.getType())
          || TemporaryEntity.class.isAssignableFrom(field.getType()))
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

  private static <T> T requireFieldValue(final Object instance, final Class<T> type, final String description) {
    T value = firstFieldValue(instance, type);
    if (value == null) {
      throw new IllegalStateException("Expected a non-null " + description + " (" + type.getName()
          + ") field on " + instance.getClass().getName()
          + "; @ComponentH2Test / @ComponentPgTest tests must extend AbstractComponentTest / AbstractServiceAuthzTest.");
    }
    return value;
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
      log.warn("Component test harness cleanup step '{}' failed: {}", description, e.getMessage());
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable
  {
    void run() throws Exception;
  }
}
