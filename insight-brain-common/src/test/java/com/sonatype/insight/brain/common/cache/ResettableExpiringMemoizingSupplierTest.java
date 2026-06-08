/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.cache;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResettableExpiringMemoizingSupplierTest
{
  @Test
  public void testGet() throws Exception {
    Supplier<Integer> mockSupplier = mock(Supplier.class);
    when(mockSupplier.get()).thenReturn(1).thenReturn(2);

    ResettableExpiringMemoizingSupplier<Integer> underTest =
        spy(new ResettableExpiringMemoizingSupplier<>(mockSupplier, Duration.ofHours(1)));

    // 1st call, calls the delegate
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockSupplier).get();

    Mockito.clearInvocations(mockSupplier);

    // 2nd call, uses the memoized value
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockSupplier, never()).get();

    // 3rd call, after the expiration, calls the delegate
    when(underTest.nanoTime()).thenReturn(System.nanoTime() + Duration.ofHours(1).toNanos());
    assertThat(underTest.get()).isEqualTo(2);
    verify(mockSupplier).get();
  }

  @Test
  public void testReset() {
    Supplier<Integer> mockSupplier = mock(Supplier.class);
    when(mockSupplier.get()).thenReturn(1).thenReturn(2);

    ResettableExpiringMemoizingSupplier<Integer> underTest =
        new ResettableExpiringMemoizingSupplier<>(mockSupplier, Duration.ofHours(1));

    // 1st call, calls the delegate
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockSupplier).get();

    Mockito.clearInvocations(mockSupplier);

    // 2nd call, uses the memoized value
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockSupplier, never()).get();

    // 3rd call, after a reset, calls the delegate
    underTest.reset();
    assertThat(underTest.get()).isEqualTo(2);
    verify(mockSupplier).get();
  }

  @Test
  public void testOnChange_Expire() {
    Supplier<Integer> mockSupplier = mock(Supplier.class);
    when(mockSupplier.get()).thenReturn(1).thenReturn(2);
    Consumer<Integer> mockConsumer = mock(Consumer.class);
    ResettableExpiringMemoizingSupplier<Integer> underTest =
        spy(new ResettableExpiringMemoizingSupplier<>(mockSupplier, Duration.ofHours(1), mockConsumer));

    // 1st call, calls the delegate, and calls the onChange consumer
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockConsumer).accept(1);

    Mockito.clearInvocations(mockConsumer);

    // 2nd call, uses the memoized value, and doesn't call the onChange consumer
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockConsumer, never()).accept(1);

    when(underTest.nanoTime()).thenReturn(System.nanoTime() + Duration.ofHours(1).toNanos());

    // 3rd call, after the expiration, calls the delegate, and calls the onChange consumer
    assertThat(underTest.get()).isEqualTo(2);
    verify(mockConsumer).accept(2);
  }

  @Test
  public void testOnChange_Reset() {
    Supplier<Integer> mockSupplier = mock(Supplier.class);
    when(mockSupplier.get()).thenReturn(1).thenReturn(2);
    Consumer<Integer> mockConsumer = mock(Consumer.class);
    ResettableExpiringMemoizingSupplier<Integer> underTest =
        new ResettableExpiringMemoizingSupplier<>(mockSupplier, Duration.ofHours(1), mockConsumer);

    // 1st call, calls the delegate, and calls the onChange consumer
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockConsumer).accept(1);

    Mockito.clearInvocations(mockConsumer);

    // 2nd call, uses the memoized value, and doesn't call the onChange consumer
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockConsumer, never()).accept(1);

    underTest.reset();

    // 3rd call, after a reset, calls the delegate, and calls the onChange consumer
    assertThat(underTest.get()).isEqualTo(2);
    verify(mockConsumer).accept(2);
  }

  @Test
  public void testSetMemoizedValue() {
    Supplier<Integer> mockSupplier = mock(Supplier.class);
    when(mockSupplier.get()).thenReturn(1).thenReturn(2);

    ResettableExpiringMemoizingSupplier<Integer> underTest =
        spy(new ResettableExpiringMemoizingSupplier<>(mockSupplier, Duration.ofHours(1)));

    // 1st call, calls the delegate
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockSupplier).get();

    Mockito.clearInvocations(mockSupplier);

    // 2nd call, uses the memoized value
    assertThat(underTest.get()).isEqualTo(1);
    verify(mockSupplier, never()).get();

    // Set the value manually
    underTest.setMemoizedValue(3);

    // 3rd call, before expiration, returns the manually set value, the supplier is not called in this case
    assertThat(underTest.get()).isEqualTo(3);
    verify(mockSupplier, never()).get();

    // Double check it still returns the manually set value and that it wasn't a one-off
    assertThat(underTest.get()).isEqualTo(3);
    verify(mockSupplier, never()).get();

    when(underTest.nanoTime()).thenReturn(System.nanoTime() + Duration.ofHours(1).toNanos());

    // 4th call, after the expiration, calls the delegate, and calls the supplier
    assertThat(underTest.get()).isEqualTo(2);
    verify(mockSupplier).get();
  }
}
