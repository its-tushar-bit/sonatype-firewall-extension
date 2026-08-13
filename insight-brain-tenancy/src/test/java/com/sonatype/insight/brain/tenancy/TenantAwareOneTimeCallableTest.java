/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TenantAwareOneTimeCallableTest
    extends MultiTenantTestSupport
{
  @Test
  public void shouldCallWrappedCallable_usingTenantSetAtCreationTime() throws Exception {
    Callable<?> mockCallable = mock(Callable.class);

    Tenant expectedTenant = new Tenant("correcttenant");
    TenantTestHelper.setTenantWithoutValidation(expectedTenant);

    when(mockCallable.call()).then(invocationOnMock -> {
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(expectedTenant);
      return null;
    });

    TenantAwareOneTimeCallable<?> callable = new TenantAwareOneTimeCallable<>(mockCallable);

    TenantTestHelper.setTenantWithoutValidation(new Tenant("wrongtenant"));

    callable.call();

    // Verify that the callable did actually run and therefore the assertion also was called
    verify(mockCallable).call();

    verify(subject).associateWith(any(Callable.class));
  }

  @Test
  public void shouldFail_whenReused() throws Exception {
    Callable<?> mockCallable = mock(Callable.class);

    TenantAwareOneTimeCallable<?> callable = new TenantAwareOneTimeCallable<>(mockCallable);
    callable.call();

    assertThatThrownBy(callable::call)
        .isInstanceOfAny(RuntimeException.class)
        .hasMessage("TenantAwareOneTimeCallable cannot be reused");
  }
}
