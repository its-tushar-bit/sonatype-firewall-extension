/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for test fixtures used by IQ as JUnit rules. Provides automatic support for re-provisioning fixtures when
 * needed based on conditions. Each fixture type will need annotations to control the type of that particular fixture.
 * <p>
 * See current implementations for examples of usage.
 *
 * @param <T> Enum containing the specific options for the fixture
 * @param <F> The fixture type itself
 */
public abstract class InsightFixtureRule<T, F extends InsightTestFixture>
    extends ExternalResource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected F fixture;

  protected T type;

  protected T previousType;

  // Track any fixture annotation applied to the current test
  protected Annotation annotation;

  protected volatile String testName;

  // Track the current test class for detecting test class changes across fork-reused JVMs
  protected Class<?> currentTestClass;

  // Track if the current fixture is brand new for the current test
  // - True if the fixture was just (re-)initialized during the current single test
  // - False if the fixture has remained the same compared to the previous test
  protected boolean isNewFixtureForCurrentTest;

  // A test can mark the current fixture as dirty, meaning it needs to be closed and a fresh one created
  protected boolean isCurrentFixtureDirty = true;

  private boolean forceClean;

  private boolean lastTestHadCustomSettings = false;

  @Override
  public final Statement apply(final Statement base, final Description description) {
    // grab the annotation for this test
    annotation = getAnnotation(description);
    testName = description.getMethodName();
    currentTestClass = description.getTestClass();

    return super.apply(base, description);
  }

  @Override
  protected void before() throws Throwable {
    // get the current fixture type, if defined, for the current method under test
    type = getType();

    forceClean = getForceClean(annotation);

    if (fixtureNeedsReinitialization()) {
      log.info("(Re)initializing test fixture");

      initializeFixture();

      afterInitializeFixture();
    }
  }

  @Override
  protected void after() {
    isNewFixtureForCurrentTest = false;

    // after each test method is complete, mark the fixture type that was used for it
    previousType = type;

    lastTestHadCustomSettings = getLastTestHadCustomSettings(annotation);

    if (fixture != null && !fixture.isFixtureReusable()) {
      closePreviousFixture();
      // The fixture was destroyed — mark dirty so the next test's before() re-initializes it.
      // Without this, fixtureNeedsReinitialization() may return false (e.g. when hasAnnotation()
      // returns false for default @H2DiskTest settings after PR #15866), leaving the next test
      // with a dead fixture whose pools and databases have already been closed.
      isCurrentFixtureDirty = true;
    }
  }

  /**
   * Special test method which forces a shutdown of the fixture. Only use if your test requires it. Will nuke the
   * fixture so no other calls to it can be made, and a new fixture will be provisioned for the next test.
   */
  public void shutdown() throws Exception {
    fixture.close();
    fixture = null;
  }

  public boolean isFixtureReusable() {
    // Not reusable if this is a new fixture for the current test
    return !isNewFixtureForCurrentTest;
  }

  /**
   * Any test that fudges the fixture should mark it as dirty so the next test will get a cleanly provisioned fixture.
   */
  public void markFixtureAsDirty() {
    isCurrentFixtureDirty = true;
  }

  /**
   * Has the fixture type changed between the previous test and current test
   */
  protected boolean hasFixtureTypeChanged() {
    return previousType != type;
  }

  /**
   * Provides an opportunity for the implementer to perform operations after the fixture is initialized
   */
  protected void afterInitializeFixture() {
    // default no-op
  }

  protected boolean getLastTestHadCustomSettings(final Annotation annotation) {
    return false;
  }

  protected abstract List<Class<? extends Annotation>> getAnnotationTypes();

  /**
   * If applicable, return the value of the `forceClean` attribute on the annotation
   */
  protected abstract boolean getForceClean(final Annotation annotation);

  /**
   * Return if the current test has any fixture/rule annotation
   */
  protected abstract boolean hasAnnotation();

  protected abstract F createNewFixture();

  protected abstract T getType();

  public Map<String, Object> getMetadata() {
    return fixture.getMetadata();
  }

  /**
   * Retrieves any defined rule/fixture annotation on the currently executing test:
   * <ul>
   * <li>annotations on the method have higher precedence than the class</li>
   * <li>annotations on the subclass have higher precedence than the super class</li>
   * </ul>
   */
  private Annotation getAnnotation(final Description description) {
    // method annotations have higher priority
    Annotation[] methodAnnotations =
        description.getAnnotations().toArray(new Annotation[description.getAnnotations().size()]);
    Annotation[] classAnnotations = description.getTestClass().getAnnotations();

    // reverse them as the subclass should have priority
    ArrayUtils.reverse(methodAnnotations);
    ArrayUtils.reverse(classAnnotations);

    Annotation[] annotations = ArrayUtils.addAll(methodAnnotations, classAnnotations);

    Optional<Annotation> annotation =
        Arrays.stream(annotations)
            .filter(a -> getAnnotationTypes().contains(a.annotationType()))
            .findFirst();
    return annotation.orElse(null);
  }

  /**
   * JUnit 5 (Jupiter) entry point. The JUnit 4 {@link #apply(Statement, Description)} does not run under the
   * Jupiter engine, so a migrated test provisions the fixture from a {@code @BeforeEach} that supplies the
   * running test class and method (from {@code TestInfo}). Mirrors {@code apply}: resolve the fixture annotation
   * (method has priority over class), publish the test identity, then run the same {@link #before()}.
   */
  public final void beforeFromJupiter(final Class<?> testClass, final Method testMethod) {
    annotation = getAnnotationFrom(testMethod, testClass);
    testName = testMethod != null ? testMethod.getName() : null;
    currentTestClass = testClass;
    try {
      before();
    }
    catch (Throwable t) {
      throw new IllegalStateException("Failed to initialize test fixture for the JUnit 5 test context", t);
    }
  }

  /**
   * JUnit 5 (Jupiter) teardown counterpart to {@link #beforeFromJupiter}. Runs the same per-test {@link #after()}
   * the JUnit 4 @Rule would run, so the fixture's dirty/reset bookkeeping stays correct across tests (otherwise the
   * fixture is reused without reset and per-test data leaks, causing PK violations / stale-data failures).
   */
  public final void afterFromJupiter() {
    after();
  }

  private Annotation getAnnotationFrom(final Method testMethod, final Class<?> testClass) {
    Annotation[] methodAnnotations = testMethod != null ? testMethod.getAnnotations() : new Annotation[0];
    Annotation[] classAnnotations = testClass != null ? testClass.getAnnotations() : new Annotation[0];

    // reverse so the subclass / method has priority (mirrors getAnnotation(Description))
    ArrayUtils.reverse(methodAnnotations);
    ArrayUtils.reverse(classAnnotations);

    Annotation[] annotations = ArrayUtils.addAll(methodAnnotations, classAnnotations);

    return Arrays.stream(annotations)
        .filter(a -> getAnnotationTypes().contains(a.annotationType()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Checks if the current test needs to re-initialize the fixture
   */
  private boolean fixtureNeedsReinitialization() {
    if (hasFixtureTypeChanged()) {
      log.info("Fixture type has changed from '{}' to '{}'. Need to re-initialize.",
          previousType, type);
      return true;
    }

    // If any annotation exists, it means that the fixture needs to be re-provisioned.
    if (hasAnnotation()) {
      log.info("Current test is using custom configuration. Need to re-initialize test fixture.");
      return true;
    }

    if (lastTestHadCustomSettings) {
      log.info("Last test had `customSettings`. Need to re-initialize fixture.");

      // reset it
      lastTestHadCustomSettings = false;
      return true;
    }

    if (forceClean) {
      log.info("Clean test fixture requested. Need to re-initialize.");
      return true;
    }

    if (isCurrentFixtureDirty) {
      log.info("Current fixture marked as dirty. Need to re-initialize.");
      return true;
    }

    return false;
  }

  private void initializeFixture() {
    // close the previous fixture if necessary
    if (previousType != null) {
      closePreviousFixture();
    }

    initializeNewFixture();
    isCurrentFixtureDirty = false;
  }

  private void closePreviousFixture() {
    if (fixture != null) {
      try {
        // Allow subclasses to perform cleanup before closing the fixture
        beforeCloseFixture();

        fixture.close();
      }
      catch (Exception e) {
        throw new RuntimeException("Unable to close previous fixture", e);
      }
    }
  }

  /**
   * Provides an opportunity for the implementer to perform operations before the fixture is closed.
   * This is called right before closing the fixture when switching database types or cleaning up resources.
   */
  protected void beforeCloseFixture() {
    // default no-op
  }

  private void initializeNewFixture() {
    log.info("Creating new fixture: {}", type);
    fixture = createNewFixture();
    isNewFixtureForCurrentTest = true;
  }
}
