/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TenantAwareRunnableTest
    extends MultiTenantTestSupport
{
  @Test
  public void shouldCallWrappedRunnable_usingTenantSetAtCreationTime() {
    Runnable mockRunnable = mock(Runnable.class);

    Tenant expectedTenant = new Tenant("correcttenant");
    TenantTestHelper.setTenant(expectedTenant);

    doAnswer(invocationOnMock -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(expectedTenant);
      return null;
    }).when(mockRunnable).run();

    TenantAwareRunnable runnable = new TenantAwareRunnable(mockRunnable);

    TenantTestHelper.setTenant(new Tenant("wrongtenant"));

    runnable.run();

    // Verify that the runnable did actually run and therefore the assertion also was called
    verify(mockRunnable).run();
  }

  @Test
  public void shouldNotThrowExceptions_whenReused() {
    Runnable mockRunnable = mock(Runnable.class);

    TenantAwareRunnable runnable = new TenantAwareRunnable(mockRunnable);

    for (int i = 0; i < 5; i++) {
      runnable.run();
    }
  }
}
