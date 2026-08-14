/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import java.lang.reflect.Method;

import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.rules.TestName;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;

/**
 * Base class for multi-tenant tests in the {@code nexus-mtiq-server} module.
 *
 * <p>
 * This base is dual-engine: it carries both the JUnit 4 lifecycle (the {@link TestName} and {@link MultiTenantRule}
 * rules) and the JUnit 5 (Jupiter) lifecycle ({@code @BeforeEach}). A concrete subclass runs under exactly one engine,
 * so only that engine's annotations fire. This lets subclasses migrate to JUnit 5 class by class while un-migrated
 * subclasses keep running under the Vintage engine.
 * </p>
 */
public abstract class AbstractMultiTenantTest
{
  // JUnit 4 (Vintage) populates the running method name via this rule. Under JUnit 5 the rule does not fire and the
  // name is captured from TestInfo instead (see captureJupiterMethodName). Kept so un-migrated JUnit 4 subclasses
  // keep working.
  @Rule
  public TestName testName = new TestName();

  // JUnit 4 (Vintage) enters multi-tenant mode via this rule. Under JUnit 5 the rule does not fire and
  // jupiterInitMultiTenantMode does it instead.
  @Rule
  public MultiTenantRule multiTenantRule = new MultiTenantRule();

  private String jupiterMethodName;

  // JUnit 5 only: enter multi-tenant mode, since the JUnit 4 MultiTenantRule does not fire under Jupiter.
  @BeforeEach
  public void jupiterInitMultiTenantMode() {
    TenantTestHelper.initMultiTenantMode();
  }

  // JUnit 5 only: capture the running method name, since the JUnit 4 TestName rule does not fire under Jupiter.
  @BeforeEach
  public void captureJupiterMethodName(final TestInfo testInfo) {
    jupiterMethodName = testInfo.getTestMethod().map(Method::getName).orElseGet(testInfo::getDisplayName);
  }

  /**
   * The running test method name, resolved from the JUnit 4 {@link TestName} rule (Vintage) or, when that has not
   * fired, from the JUnit 5 {@link TestInfo}.
   *
   * <p>
   * Only safe to call from {@code @Test} or {@code @AfterEach} scope, where both of this base's {@code @BeforeEach}
   * methods are guaranteed to have run. Do not call it from another {@code @BeforeEach}: the order between this
   * base's two {@code @BeforeEach} methods is not defined, so the name may not be captured yet.
   * </p>
   */
  protected String currentMethodName() {
    String ruleMethodName = testName.getMethodName();
    return ruleMethodName != null ? ruleMethodName : jupiterMethodName;
  }

  protected Tenant testAsNewTenant(ConsumerWithException<Tenant> test) {
    return TenantTestHelper.testAsNewTenant(TenantTestHelper.createTenantNameFromTest(currentMethodName()), test);
  }

  protected void testAsGlobalTenant(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAsTenant(GLOBAL_TENANT, test);
  }
}
