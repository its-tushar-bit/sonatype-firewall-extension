/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantExecutorThreadPoolsTest
    extends AbstractMultiTenantTest
{
  private static final long VERIFICATION_TIMEOUT = 500L;

  @Rule
  public TestName name = new TestName();

  @Mock
  Runnable mockRunnable;

  @Mock
  Callable<?> mockCallable;

  MultiTenantExecutorThreadPools underTest;

  ForkJoinPool pool;

  Tenant tenant;

  @Before
  public void setup() {
    underTest = new MultiTenantExecutorThreadPools();
    pool = underTest.namedForkJoinPool(1, name.getMethodName());
    tenant = new Tenant("tenant-name");
  }

  @Test
  public void submitCallableShouldRunWithinTenant() throws Exception {
    doAnswer(invocationOnMock -> assertCorrectTenantSet()).when(mockCallable).call();

    testAsTenant(tenant, t -> pool.submit(mockCallable));
    verify(mockCallable, timeout(VERIFICATION_TIMEOUT)).call();
    assertTenantSet(GLOBAL_TENANT);
  }

  @Test
  public void submitRunnableShouldRunWithinTenant() throws Exception {
    doAnswer(invocationOnMock -> assertCorrectTenantSet()).when(mockRunnable).run();

    testAsTenant(tenant, t -> pool.submit(mockRunnable));
    verify(mockRunnable, timeout(VERIFICATION_TIMEOUT)).run();
    assertTenantSet(GLOBAL_TENANT);
  }

  @Test
  public void submitRunnableWithResultShouldRunWithinTenant() throws Exception {
    doAnswer(invocationOnMock -> assertCorrectTenantSet()).when(mockRunnable).run();

    testAsTenant(tenant, t -> pool.submit(mockRunnable, null));
    verify(mockRunnable, timeout(VERIFICATION_TIMEOUT)).run();
    assertTenantSet(GLOBAL_TENANT);
  }

  @Test
  public void executeRunnableShouldRunWithinTenant() throws Exception {
    doAnswer(invocationOnMock -> assertCorrectTenantSet()).when(mockRunnable).run();

    testAsTenant(tenant, t -> pool.execute(mockRunnable));
    verify(mockRunnable, timeout(VERIFICATION_TIMEOUT)).run();
    assertTenantSet(GLOBAL_TENANT);
  }

  @Test
  public void submitForkJoinTaskShouldFail() {
    ForkJoinTask<?> mockTask = mock(ForkJoinTask.class);

    assertThatThrownBy(() -> pool.submit(mockTask)).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void executeForkJoinTaskShouldFail() {
    ForkJoinTask<?> mockTask = mock(ForkJoinTask.class);

    assertThatThrownBy(() -> pool.execute(mockTask)).isInstanceOf(RuntimeException.class);
  }

  private Void assertCorrectTenantSet() {
    assertTenantSet(tenant);
    return null;
  }
}
