/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.BooleanSupplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class ActiveRequestCounterFilterTest
    extends AbstractComponentH2Test
{
  private ActiveRequestCounterFilter activeRequestsFilter;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private LongAdder mockLongAdder;

  @Captor
  private ArgumentCaptor<BooleanSupplier> booleanSupplierArgumentCaptor;

  @Mock
  private HttpServletRequest mockHttpServletRequest;

  @Mock
  private HttpServletResponse mockHttpServletResponse;

  @Mock
  private FilterChain mockFilterChain;

  @BeforeEach
  public void before() {
    activeRequestsFilter = new ActiveRequestCounterFilter(mockShutdownHandler, mockLongAdder);
  }

  @Test
  public void testActiveRequestsFilter_AddsShutdownRequest() {
    verify(mockShutdownHandler).add(booleanSupplierArgumentCaptor.capture(), eq(ShutdownPriority.ACTIVE_REQUESTS));
    BooleanSupplier booleanSupplier = booleanSupplierArgumentCaptor.getValue();

    when(mockLongAdder.sum()).thenReturn(1L);
    assertThat(booleanSupplier.getAsBoolean()).isTrue();

    when(mockLongAdder.sum()).thenReturn(0L);
    assertThat(booleanSupplier.getAsBoolean()).isFalse();
  }

  @Test
  public void testIsShutdownPath_DoesNotMatch() {
    assertThat(activeRequestsFilter.isShutdownPath("/other")).isFalse();
  }

  @Test
  public void testIsShutdownPath_Matches() {
    assertThat(activeRequestsFilter.isShutdownPath("/tasks/shutdown")).isTrue();
  }

  @Test
  public void testDoFilter_AfterShutdownGracePeriod_ShutdownPath() throws Exception {
    when(mockShutdownHandler.isAfterGracePeriod()).thenReturn(true);
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/tasks/shutdown");

    activeRequestsFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockLongAdder, never()).increment();
    verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    verify(mockLongAdder, never()).decrement();
  }

  @Test
  public void testDoFilter_BeforeShutdownGracePeriod_ShutdownPath() throws Exception {
    when(mockShutdownHandler.isAfterGracePeriod()).thenReturn(false);
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/tasks/shutdown");

    activeRequestsFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockLongAdder, never()).increment();
    verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    verify(mockLongAdder, never()).decrement();
  }

  @Test
  public void testDoFilter_AfterShutdownGracePeriod_NotShutdownPath() throws Exception {
    when(mockShutdownHandler.isAfterGracePeriod()).thenReturn(true);
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/other");

    activeRequestsFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockLongAdder, never()).increment();
    verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    verify(mockLongAdder, never()).decrement();
  }

  @Test
  public void testDoFilter_BeforeShutdownGracePeriod_NotShutdownPath() throws Exception {
    when(mockShutdownHandler.isAfterGracePeriod()).thenReturn(false);
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/other");

    activeRequestsFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockLongAdder).increment();
    verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    verify(mockLongAdder).decrement();
  }

  @Test
  public void testDoFilter_AfterShutdownGracePeriod_NotShutdownPath_Exception() throws Exception {
    when(mockShutdownHandler.isAfterGracePeriod()).thenReturn(false);
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/other");
    doThrow(new RuntimeException()).when(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
        () -> activeRequestsFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain));

    verify(mockLongAdder).increment();
    verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    verify(mockLongAdder).decrement();
  }
}
