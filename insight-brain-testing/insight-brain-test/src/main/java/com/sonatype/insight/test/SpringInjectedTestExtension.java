/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 (Jupiter) replacement for the JUnit 4 {@code @ClassRule SpringClassRule} /
 * {@code @Rule SpringMethodRule} stack that {@link SpringInjectedTest} declares.
 *
 * <p>
 * Those rules do two things beyond Spring's own wiring: they publish the running test class / method /
 * instance into {@link SpringTestExecutionContext} (which the DB/search fixture rules read to resolve the
 * active fixture) and they track the per-class fixture signature so a cross-fixture change can refresh the
 * cached context. Under the Jupiter engine {@code @Rule} is inert, so this extension reproduces exactly that
 * bookkeeping as Jupiter callbacks, delegating to the package-private hooks on {@link SpringInjectedTest} so
 * the logic stays single-sourced with the Vintage path.
 *
 * <p>
 * Spring's own {@code SpringExtension} (registered alongside this one on {@link SpringInjectedTest}) owns the
 * cached {@code ApplicationContext} and populates {@code @Inject}/{@code @Autowired} fields; this extension
 * never builds or dirties the context. The reflective context refresh only fires on a cross-fixture change,
 * which cannot occur in the single-fixture (module-segregated) modules that run these tests under Jupiter.
 */
public class SpringInjectedTestExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback
{
  @Override
  public void beforeAll(final ExtensionContext context) {
    SpringInjectedTest.jupiterBeforeAll(context.getRequiredTestClass());
  }

  @Override
  public void beforeEach(final ExtensionContext context) {
    SpringInjectedTest.jupiterBeforeEach(context.getRequiredTestInstance(), context.getRequiredTestMethod());
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    SpringInjectedTest.jupiterAfterEach();
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    SpringInjectedTest.jupiterAfterAll(context.getRequiredTestClass());
  }
}
