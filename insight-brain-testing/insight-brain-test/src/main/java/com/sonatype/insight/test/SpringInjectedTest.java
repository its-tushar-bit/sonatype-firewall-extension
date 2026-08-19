/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import jakarta.inject.Inject;
import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.util.AopTestUtils;

/**
 * Spring-based test base class for dependency-injected tests.
 * Provides dependency injection for tests using the Spring test context.
 *
 * <p>
 * Tests extending this class will have dependencies injected via @Inject annotations.
 * The test context is configured by the @ContextConfiguration annotation.
 * </p>
 *
 * <p>
 * Subclasses can provide their own @TestConfiguration inner classes to register
 * test-specific beans.
 * </p>
 *
 * <p>
 * <b>Migration Note:</b> Tests that previously relied on the legacy injected test base should now extend this class.
 * </p>
 */
@ContextConfiguration(classes = SpringTestConfiguration.class)
public abstract class SpringInjectedTest
{
  private static final int MAX_MOCK_REPLACEMENT_DEPTH = 4;

  private static final Map<Class<?>, String> LAST_CONTEXT_SIGNATURES = new ConcurrentHashMap<>();

  /**
   * Tracks the fixture signature from the most recent test method execution across all test classes.
   * Used to detect when a previous test class left the Spring context configured for a different
   * fixture type (e.g., PostgreSQL beans leaking into an H2-based test due to Spring context caching).
   */
  private static final Object FIXTURE_LOCK = new Object();

  private static String lastActiveFixtureSignature;

  /**
   * Set when the shared test {@code DatabaseContainer} is re-provisioned by <em>another</em> test in the
   * same reused JVM (e.g. a full-server {@code AbstractBaseIntegrationTest} flips the
   * {@code DatabaseContainerRule} singleton onto a different base-class fixture, closing the previous
   * connection pool). The cached Spring context still holds beans bound to the now-closed datasource, yet
   * the fixture-type signature is unchanged, so the normal fixture check would not rebuild it. This flag
   * forces the next dependency-injected test to refresh its context before touching the DB. Written via
   * {@link #onSharedDatabaseContainerReprovisioned()} and consumed under {@link #FIXTURE_LOCK}.
   */
  private static volatile boolean sharedDatabaseContainerReprovisioned;

  private static final String DB_FIXTURE_ANNOTATION_PREFIX =
      "com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations$";

  private static final String SEARCH_FIXTURE_ANNOTATION_PREFIX =
      "com.sonatype.insight.brain.search.SearchIndexRuleAnnotations$";

  private static final Set<String> CONTEXT_FIXTURE_ANNOTATION_TYPES = Set.of(
      "com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations$H2InMemoryTest",
      "com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations$H2DiskTest",
      "com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations$PostgresTest",
      "com.sonatype.insight.brain.search.SearchIndexRuleAnnotations$LuceneTest",
      "com.sonatype.insight.brain.search.SearchIndexRuleAnnotations$OpenSearchHttpTest");

  private final Map<BeanFieldMutation, Object> originalBeanFieldValues = new LinkedHashMap<>();

  @ClassRule
  public static final TestRule SPRING_CLASS_RULE = new TestRule()
  {
    private final SpringClassRule delegate = new SpringClassRule();

    @Override
    public Statement apply(final Statement base, final Description description) {
      Statement delegateStatement = delegate.apply(base, description);
      return new Statement()
      {
        @Override
        public void evaluate() throws Throwable {
          clearTrackedContextSignature(description.getTestClass());
          seedTrackedContextSignature(description.getTestClass());
          SpringTestExecutionContext.setCurrentTestClass(description.getTestClass());
          try {
            delegateStatement.evaluate();
          }
          finally {
            clearTrackedContextSignature(description.getTestClass());
            SpringTestExecutionContext.clearCurrentTestMethod();
            SpringTestExecutionContext.clearCurrentTestClass();
          }
        }
      };
    }
  };

  @Rule
  public final MethodRule springMethodRule = new MethodRule()
  {
    private final SpringMethodRule delegate = new SpringMethodRule();

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
      Statement delegateStatement = delegate.apply(base, method, target);
      return new Statement()
      {
        @Override
        public void evaluate() throws Throwable {
          SpringTestExecutionContext.setCurrentTestClass(target.getClass());
          SpringTestExecutionContext.setCurrentTestMethod(method.getMethod());
          SpringTestExecutionContext.setCurrentTestInstance(target);
          refreshApplicationContextIfFixtureChanged(target, method.getMethod());
          try {
            delegateStatement.evaluate();
          }
          finally {
            SpringTestExecutionContext.clearCurrentTestInstance();
            SpringTestExecutionContext.clearCurrentTestMethod();
          }
        }
      };
    }
  };

  @Inject
  private ApplicationContext applicationContext;

  private static void refreshApplicationContextIfFixtureChanged(
      final Object testInstance,
      final Method testMethod)
  {
    Class<?> testClass = testInstance.getClass();
    String currentSignature = computeContextSignature(testClass, testMethod);

    // Synchronized to prevent TOCTOU races under parallel test execution - the read-compare-act
    // must be atomic so one thread's getAndSet cannot cause another thread to spuriously refresh.
    synchronized (FIXTURE_LOCK) {
      // A test elsewhere in this reused JVM re-provisioned the shared DatabaseContainer (closing the
      // previous connection pool). The fixture-type signature is unchanged, so the checks below would
      // not rebuild the cached context and its beans would stay bound to the now-closed datasource.
      // Force a rebuild so this test's DB access uses the current pool.
      if (sharedDatabaseContainerReprovisioned) {
        sharedDatabaseContainerReprovisioned = false;
        lastActiveFixtureSignature = currentSignature;
        LAST_CONTEXT_SIGNATURES.put(testClass, currentSignature);
        refreshApplicationContext(testClass, testInstance);
        return;
      }
      // Cross-class fixture check: detect when a previous test class left the Spring context
      // configured for a different fixture type (e.g., PostgreSQL beans leaking into an H2-based
      // test due to Spring context caching). The per-class check below only compares within the
      // same test class and cannot catch cross-class pollution.
      String previousActive = lastActiveFixtureSignature;
      lastActiveFixtureSignature = currentSignature;
      if (previousActive != null && !previousActive.equals(currentSignature)) {
        LAST_CONTEXT_SIGNATURES.put(testClass, currentSignature);
        refreshApplicationContext(testClass, testInstance);
        // Early return skips within-class check intentionally: the refresh already rebuilt the
        // context for the current fixture, and subsequent methods will compare correctly.
        return;
      }

      // Within-class fixture check (existing logic)
      String previousSignature = LAST_CONTEXT_SIGNATURES.put(testClass, currentSignature);
      if (previousSignature != null && !previousSignature.equals(currentSignature)) {
        refreshApplicationContext(testClass, testInstance);
      }
    }
  }

  /**
   * Signals that the shared test {@code DatabaseContainer} was re-provisioned (its connection pool
   * swapped/closed) by some other test in this reused JVM. The next dependency-injected test will
   * rebuild its Spring context before touching the DB, so its beans are re-bound to the live pool
   * instead of the closed one. Invoked by {@code DatabaseContainerRule} when it creates a new container.
   */
  public static void onSharedDatabaseContainerReprovisioned() {
    sharedDatabaseContainerReprovisioned = true;
  }

  private static void clearTrackedContextSignature(final Class<?> testClass) {
    if (testClass != null) {
      LAST_CONTEXT_SIGNATURES.remove(testClass);
    }
  }

  // Seeds with class-level annotations only (null method). If the first method has a method-level
  // fixture annotation, the within-class check will detect the mismatch and refresh - this is
  // intentional since the context needs the correct fixture for that method.
  private static void seedTrackedContextSignature(final Class<?> testClass) {
    if (testClass != null) {
      LAST_CONTEXT_SIGNATURES.put(testClass, computeContextSignature(testClass, null));
    }
  }

  // --- JUnit 5 (Jupiter) lifecycle bridge -------------------------------------------------------
  // These package-private hooks let SpringInjectedTestExtension reproduce, under the Jupiter engine,
  // the SpringTestExecutionContext bookkeeping + fixture-signature tracking that the JUnit 4
  // @ClassRule/@Rule stack drives under Vintage. The reflective context refresh only ever fires on a
  // cross-fixture change, which cannot happen in the single-fixture (module-segregated) Jupiter
  // modules these tests run under.
  static void jupiterBeforeAll(final Class<?> testClass) {
    clearTrackedContextSignature(testClass);
    seedTrackedContextSignature(testClass);
    SpringTestExecutionContext.setCurrentTestClass(testClass);
  }

  static void jupiterBeforeEach(final Object testInstance, final Method testMethod) {
    SpringTestExecutionContext.setCurrentTestClass(testInstance.getClass());
    SpringTestExecutionContext.setCurrentTestMethod(testMethod);
    SpringTestExecutionContext.setCurrentTestInstance(testInstance);
    refreshApplicationContextIfFixtureChanged(testInstance, testMethod);
  }

  static void jupiterAfterEach() {
    SpringTestExecutionContext.clearCurrentTestInstance();
    SpringTestExecutionContext.clearCurrentTestMethod();
  }

  static void jupiterAfterAll(final Class<?> testClass) {
    clearTrackedContextSignature(testClass);
    SpringTestExecutionContext.clearCurrentTestMethod();
    SpringTestExecutionContext.clearCurrentTestClass();
  }

  private static void refreshApplicationContext(final Class<?> testClass, final Object testInstance) {
    try {
      TestContextManager testContextManager = getTestContextManager(testClass);
      testContextManager.getTestContext().markApplicationContextDirty(DirtiesContext.HierarchyMode.CURRENT_LEVEL);
      testContextManager.prepareTestInstance(testInstance);
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed to refresh Spring test context for " + testClass.getName(), e);
    }
  }

  // Accesses SpringClassRule's private getTestContextManager(Class) via reflection because no
  // public API exists to obtain the shared TestContextManager without re-creating one.
  // Validated against Spring Boot 4.0.6 (Spring Framework 7.0.x) - re-verify on Spring upgrades.
  // See SpringClassRuleReflectionTest for build-time verification of this contract.
  private static TestContextManager getTestContextManager(
      final Class<?> testClass) throws ReflectiveOperationException
  {
    Method accessor = SpringClassRule.class.getDeclaredMethod("getTestContextManager", Class.class);
    accessor.setAccessible(true);
    return (TestContextManager) accessor.invoke(null, testClass);
  }

  private static String computeContextSignature(final Class<?> testClass, final Method testMethod) {
    return "db=" + resolveFixtureScopeSignature(testMethod, testClass, DB_FIXTURE_ANNOTATION_PREFIX)
        + ";search=" + resolveFixtureScopeSignature(testMethod, testClass, SEARCH_FIXTURE_ANNOTATION_PREFIX);
  }

  private static String resolveFixtureScopeSignature(
      final Method testMethod,
      final Class<?> testClass,
      final String annotationClassPrefix)
  {
    Annotation methodAnnotation = findRelevantAnnotation(testMethod != null ? testMethod.getAnnotations() : null,
        annotationClassPrefix);
    if (methodAnnotation != null && testMethod != null) {
      return describeFixtureAnnotation(methodAnnotation) + "#" + testMethod.getName();
    }

    Annotation classAnnotation = findRelevantAnnotation(testClass != null ? testClass.getAnnotations() : null,
        annotationClassPrefix);
    return classAnnotation != null ? describeFixtureAnnotation(classAnnotation) : "default";
  }

  private static Annotation findRelevantAnnotation(
      final Annotation[] annotations,
      final String annotationClassPrefix)
  {
    if (annotations == null) {
      return null;
    }
    for (Annotation annotation : annotations) {
      String annotationName = annotation.annotationType().getName();
      if (annotationName.startsWith(annotationClassPrefix)
          && CONTEXT_FIXTURE_ANNOTATION_TYPES.contains(annotationName))
      {
        return annotation;
      }
    }
    return null;
  }

  private static String describeFixtureAnnotation(final Annotation annotation) {
    return annotation.annotationType().getName() + ":" + annotation;
  }

  @Before
  @BeforeEach
  public void prepareInjectedTestInstance() {
    if (!enforceSecurityAspects()) {
      SecurityAspectControl.disableEnforcement();
    }
    originalBeanFieldValues.clear();
    snapshotInjectedBeanState();
    populateLegacyTestFields();
    unwrapInjectedTestFields();
    wireMocksIntoInjectedFields();
    applyBeanFieldOverrides();
  }

  @After
  @AfterEach
  public void restoreInjectedTestInstance() {
    SecurityAspectControl.enableEnforcement();
    List<Map.Entry<BeanFieldMutation, Object>> mutations = new ArrayList<>(originalBeanFieldValues.entrySet());
    Collections.reverse(mutations);
    for (Map.Entry<BeanFieldMutation, Object> entry : mutations) {
      try {
        entry.getKey().field.setAccessible(true);
        entry.getKey().field.set(entry.getKey().target, entry.getValue());
      }
      catch (IllegalAccessException ignored) {
        // best-effort compatibility shim for legacy injected tests
      }
    }
    originalBeanFieldValues.clear();
  }

  /**
   * Tests that verify AOP behavior can override this to keep the Spring proxies.
   */
  protected boolean preserveAopProxies() {
    return false;
  }

  /**
   * Tests that verify security authorization can override this to keep CTW security aspects active.
   * By default, CTW security aspects ({@code @Authorize}, {@code @HasFeature}, etc.) are disabled
   * during tests because they are woven into the bytecode and cannot be bypassed by unwrapping proxies.
   */
  protected boolean enforceSecurityAspects() {
    return false;
  }

  /**
   * Deterministic bean-field overrides for tests that need to replace collaborators that are not declared as
   * {@code @Mock} fields on the test class or cannot be reliably swapped through best-effort graph traversal.
   */
  protected List<BeanFieldOverride> getBeanFieldOverrides() {
    return Collections.emptyList();
  }

  protected static BeanFieldOverride beanFieldOverride(
      final Class<?> beanType,
      final String fieldName,
      final Object value)
  {
    return new BeanFieldOverride(beanType, fieldName, value);
  }

  /**
   * Look up a component from the Spring context by class.
   */
  protected final <T> T lookup(Class<T> type) {
    String conventionalBeanName = Introspector.decapitalize(type.getSimpleName());
    T bean;
    if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())
        && applicationContext.containsBean(conventionalBeanName))
    {
      bean = adaptBeanForTest(applicationContext.getBean(conventionalBeanName, type));
    }
    else {
      String[] beanNames = applicationContext.getBeanNamesForType(type, false, false);
      bean = beanNames.length == 1
          ? adaptBeanForTest(applicationContext.getBean(beanNames[0], type))
          : adaptBeanForTest(applicationContext.getBean(type));
    }
    wireMocksIntoBean(bean);
    return bean;
  }

  /**
   * Look up a component from the Spring context by name and class.
   */
  protected final <T> T lookup(String name, Class<T> type) {
    T bean = adaptBeanForTest(applicationContext.getBean(name, type));
    wireMocksIntoBean(bean);
    return bean;
  }

  /**
   * Get the ApplicationContext for advanced lookups.
   */
  protected final ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  private void populateLegacyTestFields() {
    for (Field field : getAllFields(getClass())) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      try {
        field.setAccessible(true);
        if (field.get(this) != null) {
          continue;
        }
        if (isSpyField(field)) {
          Object bean = resolveUniqueBean(field.getType());
          if (bean != null) {
            field.set(this, createMockitoSpy(adaptBeanForTest(bean)));
          }
          continue;
        }
        if (isMockOnlyField(field)) {
          field.set(this, createMockitoMock(field.getType()));
          continue;
        }
        if (isActiveLegacyContextLookupField(field)) {
          Object bean = resolveUniqueBean(field.getType());
          if (bean != null) {
            field.set(this, adaptBeanForTest(bean));
          }
        }
      }
      catch (IllegalAccessException ignored) {
        // best-effort compatibility shim for legacy injected tests
      }
    }

    assignLegacyMockFieldsToMatchingInjectedFields();
  }

  private void assignLegacyMockFieldsToMatchingInjectedFields() {
    for (Field mockField : getAllFields(getClass())) {
      if (Modifier.isStatic(mockField.getModifiers()) || !isActiveMockField(mockField)) {
        continue;
      }

      try {
        mockField.setAccessible(true);
        Object mockValue = mockField.get(this);
        if (mockValue == null) {
          continue;
        }

        for (Field injectedField : getAllFields(getClass())) {
          if (Modifier.isStatic(injectedField.getModifiers()) || !isInjectedField(injectedField)) {
            continue;
          }
          if (!injectedField.getType().isInstance(mockValue)) {
            continue;
          }
          if (!getLegacyMockFieldNames(mockField).contains(injectedField.getName())) {
            continue;
          }

          injectedField.setAccessible(true);
          if (injectedField.get(this) != mockValue) {
            injectedField.set(this, mockValue);
          }
        }
      }
      catch (IllegalAccessException ignored) {
        // best-effort compatibility shim for legacy injected tests
      }
    }
  }

  private void unwrapInjectedTestFields() {
    if (preserveAopProxies()) {
      return;
    }

    for (Field field : getAllFields(getClass())) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      if (!isInjectedField(field) && !isActiveLegacyContextLookupField(field)) {
        continue;
      }

      try {
        field.setAccessible(true);
        Object currentValue = field.get(this);
        Object adaptedValue = adaptBeanForTest(currentValue);
        if (adaptedValue != currentValue) {
          field.set(this, adaptedValue);
        }
      }
      catch (IllegalAccessException ignored) {
        // best-effort compatibility shim for legacy injected tests
      }
    }
  }

  private void wireMocksIntoInjectedFields() {
    for (Field field : getAllFields(getClass())) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      if (!isInjectedField(field) && !isActiveLegacyContextLookupField(field)) {
        continue;
      }

      try {
        field.setAccessible(true);
        wireMocksIntoBean(field.get(this));
      }
      catch (IllegalAccessException ignored) {
        // best-effort compatibility shim for legacy injected tests
      }
    }
  }

  private void applyBeanFieldOverrides() {
    for (BeanFieldOverride override : getBeanFieldOverrides()) {
      applyBeanFieldOverride(override.beanType(), override.fieldName(), override.value());
    }
  }

  private void snapshotInjectedBeanState() {
    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Field field : getAllFields(getClass())) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      if (!isInjectedField(field) && !isActiveLegacyContextLookupField(field)) {
        continue;
      }

      try {
        field.setAccessible(true);
        snapshotBeanFields(field.get(this), visited);
      }
      catch (IllegalAccessException ignored) {
        // best-effort compatibility shim for legacy injected tests
      }
    }
  }

  private void snapshotBeanFields(final Object bean, final Set<Object> visited) {
    Object target = unwrapProxyTarget(bean);
    if (target == null || visited.contains(target) || !shouldSnapshotBeanState(target.getClass())) {
      return;
    }

    visited.add(target);
    for (Field beanField : getAllFields(target.getClass())) {
      if (Modifier.isStatic(beanField.getModifiers()) || Modifier.isFinal(beanField.getModifiers())) {
        continue;
      }
      try {
        beanField.setAccessible(true);
        recordOriginalFieldValue(target, beanField, snapshotFieldValue(beanField.get(target)));
      }
      catch (IllegalAccessException ignored) {
        // best-effort compatibility shim for legacy injected tests
      }
    }
  }

  private static boolean shouldSnapshotBeanState(final Class<?> type) {
    Package pkg = type.getPackage();
    return pkg != null && pkg.getName().startsWith("com.sonatype.");
  }

  private static Object snapshotFieldValue(final Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof List<?> list) {
      return new ArrayList<>(list);
    }
    if (value instanceof Set<?> set) {
      return new LinkedHashSet<>(set);
    }
    if (value instanceof java.util.SortedMap<?, ?> sortedMap) {
      return new java.util.TreeMap<>(sortedMap);
    }
    if (value instanceof Map<?, ?> map) {
      return new LinkedHashMap<>(map);
    }
    if (value.getClass().isArray()) {
      int length = Array.getLength(value);
      Object copy = Array.newInstance(value.getClass().getComponentType(), length);
      System.arraycopy(value, 0, copy, 0, length);
      return copy;
    }
    return value;
  }

  protected final void applyBeanFieldOverride(
      final Class<?> beanType,
      final String fieldName,
      final Object value)
  {
    Object bean = resolveBeanForOverride(beanType);
    Object target = unwrapProxyTarget(bean);
    setFieldValue(target, fieldName, value);
  }

  private Object resolveBeanForOverride(final Class<?> beanType) {
    for (Field field : getAllFields(getClass())) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      if (!field.isAnnotationPresent(Inject.class) && !field.isAnnotationPresent(Autowired.class)) {
        continue;
      }
      if (!beanType.isAssignableFrom(field.getType())) {
        continue;
      }
      try {
        field.setAccessible(true);
        Object bean = field.get(this);
        if (bean != null) {
          return bean;
        }
      }
      catch (IllegalAccessException ignored) {
        // fall back to application context lookup below
      }
    }

    return applicationContext.getBean(beanType);
  }

  private static Object unwrapProxyTarget(final Object bean) {
    if (bean == null) {
      return null;
    }
    try {
      if (bean instanceof Advised advised) {
        Object target = advised.getTargetSource().getTarget();
        if (target != null) {
          return target;
        }
      }
      return AopTestUtils.getUltimateTargetObject(bean);
    }
    catch (Exception ignored) {
      return bean;
    }
  }

  private void setFieldValue(final Object target, final String fieldName, final Object value) {
    if (target == null) {
      throw new IllegalArgumentException("Cannot override field '" + fieldName + "' on a null bean target");
    }

    for (Field field : getAllFields(target.getClass())) {
      if (!field.getName().equals(fieldName)) {
        continue;
      }
      try {
        field.setAccessible(true);
        Object currentValue = field.get(target);
        if (currentValue != value) {
          recordOriginalFieldValue(target, field, currentValue);
          field.set(target, value);
        }
        return;
      }
      catch (IllegalAccessException e) {
        throw new IllegalStateException(
            "Failed to override field '" + fieldName + "' on bean type " + target.getClass().getName(), e);
      }
    }

    throw new IllegalArgumentException(
        "Could not find field '" + fieldName + "' on bean type " + target.getClass().getName());
  }

  private void wireMocksIntoBean(final Object bean) {
    Map<String, Object> mocksByName = new LinkedHashMap<>();
    List<Object> mocksByType = new ArrayList<>();
    List<Class<?>> injectedFieldTypes = new ArrayList<>();
    for (Field field : getAllFields(getClass())) {
      if (isInjectedField(field)) {
        injectedFieldTypes.add(field.getType());
      }
      if (!isActiveMockField(field)) {
        continue;
      }
      try {
        field.setAccessible(true);
        Object mock = field.get(this);
        if (mock != null) {
          for (String name : getLegacyMockFieldNames(field)) {
            mocksByName.putIfAbsent(name, mock);
          }
          mocksByType.add(mock);
        }
      }
      catch (IllegalAccessException ignored) {
        // best-effort compatibility shim for legacy injected tests
      }
    }

    if (mocksByType.isEmpty()) {
      return;
    }

    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    replaceMockDependencies(bean, mocksByName, mocksByType, injectedFieldTypes, visited, 0);
  }

  @SuppressWarnings("unchecked")
  private <T> T adaptBeanForTest(final T bean) {
    if (bean == null || preserveAopProxies()) {
      return bean;
    }
    try {
      if (bean instanceof Advised advised) {
        Object target = advised.getTargetSource().getTarget();
        if (target != null) {
          return (T) target;
        }
      }
      return (T) AopTestUtils.getUltimateTargetObject(bean);
    }
    catch (Exception ignored) {
      return bean;
    }
  }

  private void replaceMockDependencies(
      final Object candidate,
      final Map<String, Object> mocksByName,
      final List<Object> mocksByType,
      final List<Class<?>> injectedFieldTypes,
      final Set<Object> visited,
      final int depth)
  {
    Object target = unwrapProxyTarget(candidate);
    if (target == null || depth > MAX_MOCK_REPLACEMENT_DEPTH || visited.contains(target)) {
      return;
    }
    if (isMockitoDouble(target)) {
      return;
    }

    Class<?> type = target.getClass();
    if (!shouldTraverse(type)) {
      return;
    }

    visited.add(target);
    for (Field field : getAllFields(type)) {
      if (Modifier.isStatic(field.getModifiers()) || !shouldTraverse(field.getDeclaringClass())) {
        continue;
      }

      try {
        field.setAccessible(true);
        Object currentValue = field.get(target);
        Object replacement = findMockReplacement(field, mocksByName, mocksByType, injectedFieldTypes);
        if (replacement == null) {
          replacement = findCollectionMockReplacement(field, mocksByType);
        }
        if (replacement != null && currentValue != replacement) {
          recordOriginalFieldValue(target, field, currentValue);
          field.set(target, replacement);
          continue;
        }
        replaceMockDependencies(currentValue, mocksByName, mocksByType, injectedFieldTypes, visited, depth + 1);
      }
      catch (IllegalAccessException ignored) {
        // best-effort compatibility shim for legacy injected tests
      }
    }
  }

  private Object findMockReplacement(
      final Field field,
      final Map<String, Object> mocksByName,
      final List<Object> mocksByType,
      final List<Class<?>> injectedFieldTypes)
  {
    Object namedMock = mocksByName.get(field.getName());
    if (namedMock != null && field.getType().isInstance(namedMock)) {
      return namedMock;
    }

    if (hasInjectedFieldOfType(field.getType(), injectedFieldTypes)) {
      return null;
    }

    Object resolvedMock = null;
    for (Object mock : mocksByType) {
      if (!field.getType().isInstance(mock)) {
        continue;
      }
      if (resolvedMock != null) {
        return null;
      }
      resolvedMock = mock;
    }
    return resolvedMock;
  }

  private Object findCollectionMockReplacement(final Field field, final List<Object> mocksByType) {
    Class<?> fieldType = field.getType();
    if (!Collection.class.isAssignableFrom(fieldType)) {
      return null;
    }

    Class<?> elementType = resolveCollectionElementType(field);
    if (elementType == null) {
      return null;
    }

    List<Object> matchingMocks = new ArrayList<>();
    for (Object mock : mocksByType) {
      if (elementType.isInstance(mock)) {
        matchingMocks.add(mock);
      }
    }
    if (matchingMocks.isEmpty()) {
      return null;
    }

    Collection<Object> replacement = instantiateCollection(fieldType);
    if (replacement == null) {
      return null;
    }
    replacement.addAll(matchingMocks);
    return replacement;
  }

  private static Collection<Object> instantiateCollection(final Class<?> fieldType) {
    if (!fieldType.isInterface()) {
      try {
        Object value = fieldType.getDeclaredConstructor().newInstance();
        if (value instanceof Collection<?> collection) {
          @SuppressWarnings("unchecked")
          Collection<Object> typedCollection = (Collection<Object>) collection;
          return typedCollection;
        }
      }
      catch (ReflectiveOperationException ignored) {
        // fall back to common collection implementations below
      }
    }

    if (Set.class.isAssignableFrom(fieldType)) {
      return new LinkedHashSet<>();
    }
    if (List.class.isAssignableFrom(fieldType) || Collection.class.isAssignableFrom(fieldType)) {
      return new ArrayList<>();
    }
    return null;
  }

  private static Class<?> resolveCollectionElementType(final Field field) {
    Type genericType = field.getGenericType();
    if (!(genericType instanceof ParameterizedType parameterizedType)) {
      return null;
    }

    Type[] typeArguments = parameterizedType.getActualTypeArguments();
    if (typeArguments.length != 1) {
      return null;
    }

    Type elementType = typeArguments[0];
    if (elementType instanceof Class<?> elementClass) {
      return elementClass;
    }
    if (elementType instanceof ParameterizedType nestedType && nestedType.getRawType() instanceof Class<?> rawType) {
      return rawType;
    }
    return null;
  }

  private static List<String> getLegacyMockFieldNames(final Field field) {
    List<String> names = new ArrayList<>();
    names.add(field.getName());

    String alias = getLegacyMockFieldAlias(field.getName());
    if (alias != null && !alias.equals(field.getName())) {
      names.add(alias);
    }
    return names;
  }

  private static String getLegacyMockFieldAlias(final String fieldName) {
    if (fieldName.startsWith("mock") && fieldName.length() > 4) {
      return Introspector.decapitalize(fieldName.substring(4));
    }
    if (fieldName.startsWith("spy") && fieldName.length() > 3) {
      return Introspector.decapitalize(fieldName.substring(3));
    }
    if (fieldName.endsWith("Mock") && fieldName.length() > 4) {
      return fieldName.substring(0, fieldName.length() - 4);
    }
    if (fieldName.endsWith("Spy") && fieldName.length() > 3) {
      return fieldName.substring(0, fieldName.length() - 3);
    }
    return null;
  }

  private Object resolveUniqueBean(final Class<?> type) {
    if (applicationContext == null || type == null) {
      return null;
    }
    String conventionalBeanName = Introspector.decapitalize(type.getSimpleName());
    if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())
        && applicationContext.containsBean(conventionalBeanName)
        && applicationContext.isTypeMatch(conventionalBeanName, type))
    {
      return applicationContext.getBean(conventionalBeanName, type);
    }
    String[] beanNames = applicationContext.getBeanNamesForType(type, false, false);
    if (beanNames.length != 1) {
      return null;
    }
    return applicationContext.getBean(beanNames[0], type);
  }

  private static Object createMockitoMock(final Class<?> type) {
    try {
      Class<?> mockitoClass = Class.forName("org.mockito.Mockito");
      return mockitoClass.getMethod("mock", Class.class).invoke(null, type);
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to create Mockito mock for " + type.getName(), e);
    }
  }

  private static Object createMockitoSpy(final Object bean) {
    try {
      Class<?> mockitoClass = Class.forName("org.mockito.Mockito");
      return mockitoClass.getMethod("spy", Object.class).invoke(null, bean);
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to create Mockito spy for " + bean.getClass().getName(), e);
    }
  }

  private static boolean isMockField(final Field field) {
    return isMockOnlyField(field) || isSpyField(field);
  }

  private static boolean isMockOnlyField(final Field field) {
    String name = field.getName();
    return name.startsWith("mock") || name.endsWith("Mock")
        || hasAnnotation(field, "org.mockito.Mock");
  }

  private static boolean isSpyField(final Field field) {
    String name = field.getName();
    return name.startsWith("spy") || name.endsWith("Spy")
        || hasAnnotation(field, "org.mockito.Spy");
  }

  private static boolean hasAnnotation(final Field field, final String annotationName) {
    return java.util.Arrays.stream(field.getAnnotations())
        .map(annotation -> annotation.annotationType().getName())
        .anyMatch(annotationName::equals);
  }

  private static boolean isInjectedField(final Field field) {
    return field.isAnnotationPresent(Inject.class) || field.isAnnotationPresent(Autowired.class);
  }

  private boolean isActiveMockField(final Field field) {
    if (isMockField(field)) {
      return true;
    }

    try {
      field.setAccessible(true);
      return isMockitoDouble(field.get(this));
    }
    catch (IllegalAccessException ignored) {
      return false;
    }
  }

  private boolean isActiveLegacyContextLookupField(final Field field) {
    if (Modifier.isStatic(field.getModifiers()) || isInjectedField(field) || isActiveMockField(field)) {
      return false;
    }
    return isLegacyContextLookupField(field);
  }

  private static boolean isLegacyContextLookupField(final Field field) {
    if (isInjectedField(field) || isMockField(field) || Modifier.isStatic(field.getModifiers())) {
      return false;
    }
    String simpleName = field.getType().getSimpleName();
    if (jakarta.inject.Provider.class.isAssignableFrom(field.getType())
        || field.getType().isInterface()
        || Modifier.isAbstract(field.getType().getModifiers())
        || simpleName.endsWith("PersistenceService")
        || simpleName.endsWith("PersistenceServiceProvider"))
    {
      return false;
    }
    Package pkg = field.getType().getPackage();
    return pkg != null && pkg.getName().startsWith("com.sonatype.");
  }

  private static boolean hasInjectedFieldOfType(final Class<?> type, final List<Class<?>> injectedFieldTypes) {
    return injectedFieldTypes.stream().anyMatch(type::isAssignableFrom);
  }

  private static boolean isMockitoDouble(final Object candidate) {
    if (candidate == null) {
      return false;
    }
    try {
      Class<?> mockitoClass = Class.forName("org.mockito.Mockito");
      Object details = mockitoClass.getMethod("mockingDetails", Object.class).invoke(null, candidate);
      Boolean isMock = (Boolean) details.getClass().getMethod("isMock").invoke(details);
      if (Boolean.TRUE.equals(isMock)) {
        return true;
      }
      Boolean isSpy = (Boolean) details.getClass().getMethod("isSpy").invoke(details);
      return Boolean.TRUE.equals(isSpy);
    }
    catch (ReflectiveOperationException ignored) {
      Class<?> type = candidate.getClass();
      return java.lang.reflect.Proxy.isProxyClass(type) || type.getName().contains("MockitoMock");
    }
  }

  private static boolean shouldTraverse(final Class<?> type) {
    Package pkg = type.getPackage();
    if (pkg == null) {
      return false;
    }
    String packageName = pkg.getName();
    return packageName.startsWith("com.sonatype.");
  }

  private static List<Field> getAllFields(final Class<?> type) {
    List<Field> fields = new ArrayList<>();
    for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
      Collections.addAll(fields, current.getDeclaredFields());
    }
    return fields;
  }

  private void recordOriginalFieldValue(final Object target, final Field field, final Object currentValue) {
    originalBeanFieldValues.putIfAbsent(new BeanFieldMutation(target, field), currentValue);
  }

  protected record BeanFieldOverride(Class<?> beanType, String fieldName, Object value)
  {
  }

  private static final class BeanFieldMutation
  {
    private final Object target;

    private final Field field;

    private BeanFieldMutation(final Object target, final Field field) {
      this.target = target;
      this.field = field;
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof BeanFieldMutation mutation)) {
        return false;
      }
      return target == mutation.target && field.equals(mutation.field);
    }

    @Override
    public int hashCode() {
      return 31 * System.identityHashCode(target) + field.hashCode();
    }
  }
}
