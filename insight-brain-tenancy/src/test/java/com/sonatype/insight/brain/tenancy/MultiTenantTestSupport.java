/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.rules.TestName;
import org.mockito.quality.Strictness;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * This class is to support multi-tenant tests OUTSIDE of the nexus-mtiq-server module. Specifically this calls
 * {@link TenantTestHelper#resetAfterTest()} which puts it back into single-tenant mode which is not wanted in the full
 * multi-tenant tests. Tests in the `nexus-mtiq-server` module should use `AbstractMultiTenantTest`
 *
 * <p>
 * This base is dual-engine: it carries both the JUnit 4 lifecycle ({@code @Before}/{@code @After} plus the
 * {@link TestName} rule) and the JUnit 5 (Jupiter) lifecycle ({@code @BeforeEach}/{@code @AfterEach}). A concrete
 * subclass runs under exactly one engine, so only that engine's annotations fire. This lets subclasses migrate to
 * JUnit 5 class by class while un-migrated subclasses keep running under the Vintage engine.
 * </p>
 */
public abstract class MultiTenantTestSupport
{
  // JUnit 4 (Vintage) populates the running method name via this rule. Under JUnit 5 the rule does not fire and the
  // name is captured from TestInfo instead (see captureJupiterMethodName). Kept so un-migrated JUnit 4 subclasses
  // keep working.
  @Rule
  public TestName testName = new TestName();

  private String jupiterMethodName;

  protected Subject subject;

  protected SecurityManager securityManager;

  @Before
  @BeforeEach
  public void setup() {
    TenantTestHelper.initMultiTenantMode();
    mockSecurityContext();
  }

  // JUnit 5 only: capture the running method name, since the JUnit 4 TestName rule does not fire under Jupiter.
  @BeforeEach
  public void captureJupiterMethodName(final TestInfo testInfo) {
    jupiterMethodName = testInfo.getTestMethod().map(Method::getName).orElseGet(testInfo::getDisplayName);
  }

  @After
  @AfterEach
  public void resetAfterTest() {
    TenantTestHelper.resetAfterTest();
  }

  /**
   * The running test method name, resolved from the JUnit 4 {@link TestName} rule (Vintage) or, when that has not
   * fired, from the JUnit 5 {@link TestInfo}.
   *
   * <p>
   * Only safe to call from {@code @Test} or {@code @AfterEach} scope, where both of this base's
   * {@code @BeforeEach} methods are guaranteed to have run. Do not call it from another {@code @BeforeEach}:
   * the order between this base's two {@code @BeforeEach} methods is not defined, so the name may not be captured
   * yet.
   * </p>
   */
  protected String currentMethodName() {
    String ruleMethodName = testName.getMethodName();
    return ruleMethodName != null ? ruleMethodName : jupiterMethodName;
  }

  protected Tenant testAsNewTenant(ConsumerWithException<Tenant> test) {
    return TenantTestHelper.testAsNewTenant(TenantTestHelper.createTenantNameFromTest(currentMethodName()), test);
  }

  protected void testAsSingleTenant(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAsTenant(SINGLE_TENANT, test);
  }

  protected void testAsGlobalTenant(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAsTenant(GLOBAL_TENANT, test);
  }

  private void mockSecurityContext() {
    this.securityManager = mock(SecurityManager.class, withSettings().strictness(Strictness.LENIENT));
    this.subject = mock(Subject.class, withSettings().strictness(Strictness.LENIENT));

    when(subject.associateWith(any(Runnable.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    when(subject.associateWith(any(Callable.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);
  }
}
