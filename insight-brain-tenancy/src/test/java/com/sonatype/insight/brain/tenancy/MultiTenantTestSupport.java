/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.Callable;

import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
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
 */
public abstract class MultiTenantTestSupport
{
  @Rule
  public TestName testName = new TestName();

  protected Subject subject;

  protected SecurityManager securityManager;

  @Before
  public void setup() {
    TenantTestHelper.initMultiTenantMode();
    mockSecurityContext();
  }

  @After
  public void resetAfterTest() {
    TenantTestHelper.resetAfterTest();
  }

  protected Tenant testAsNewTenant(ConsumerWithException<Tenant> test) {
    return TenantTestHelper.testAsNewTenant(testName, test);
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
