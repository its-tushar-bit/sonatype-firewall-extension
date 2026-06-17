/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultRetryPolicy} (CLM-40039 Section 7.1).
 */
public class DefaultRetryPolicyTest
{
  private final DefaultRetryPolicy policy = new DefaultRetryPolicy(3);

  @Test
  public void testIsRetryable_socketTimeoutIsRetryable() {
    assertThat(policy.isRetryable(new SocketTimeoutException("read timed out"))).isTrue();
  }

  @Test
  public void testIsRetryable_connectExceptionIsRetryable() {
    assertThat(policy.isRetryable(new ConnectException("connection refused"))).isTrue();
  }

  @Test
  public void testIsRetryable_sqlTransientExceptionIsRetryable() {
    assertThat(policy.isRetryable(new SQLTransientException("transient"))).isTrue();
  }

  @Test
  public void testIsRetryable_wrappedTransientCauseIsRetryableAtDepthOne() {
    RuntimeException wrapper = new RuntimeException("jOOQ DataAccessException",
        new SocketTimeoutException("read timed out"));
    assertThat(policy.isRetryable(wrapper)).isTrue();
  }

  @Test
  public void testIsRetryable_doublyWrappedTransientCauseIsNotRetryablePastDepthOne() {
    RuntimeException inner = new RuntimeException("inner",
        new SocketTimeoutException("read timed out"));
    RuntimeException outer = new RuntimeException("outer", inner);
    assertThat(policy.isRetryable(outer)).isFalse();
  }

  @Test
  public void testIsRetryable_permanentExceptionsAreNotRetryable() {
    assertThat(policy.isRetryable(new IllegalStateException("bug"))).isFalse();
    assertThat(policy.isRetryable(new SQLException("bad sql"))).isFalse();
    assertThat(policy.isRetryable(new IOException("disk error"))).isFalse();
  }

  @Test
  public void testIsRetryable_dbcpPoolTimeoutSqlExceptionIsRetryable() {
    // DBCP 2.x throws plain SQLException with this message prefix on pool exhaustion / timeout.
    // Match by message rather than FQCN since the DBCP class isn't on the service module's
    // compile classpath.
    SQLException dbcpTimeout = new SQLException(
        "Cannot get a connection, pool error Timeout waiting for idle object");
    assertThat(policy.isRetryable(dbcpTimeout)).isTrue();
  }

  @Test
  public void testIsRetryable_dbcpPoolTimeoutWrappedAtDepthOneIsRetryable() {
    SQLException dbcpTimeout = new SQLException(
        "Cannot get a connection, pool error Timeout waiting for idle object");
    RuntimeException wrapper = new RuntimeException("jOOQ DataAccessException", dbcpTimeout);
    assertThat(policy.isRetryable(wrapper)).isTrue();
  }

  @Test
  public void testIsRetryable_unrelatedSqlExceptionMessageIsNotRetryable() {
    assertThat(policy.isRetryable(new SQLException("syntax error near 'foo'"))).isFalse();
    assertThat(policy.isRetryable(new SQLException((String) null))).isFalse();
  }

  @Test
  public void testIsRetryable_nullThrowableIsNotRetryable() {
    assertThat(policy.isRetryable(null)).isFalse();
  }

  @Test
  public void testIsRetryable_selfReferentialCauseDoesNotInfiniteLoop() {
    // Genuinely self-referential cause: getCause() returns the throwable itself. Without the
    // `cause != throwable` guard in DefaultRetryPolicy.isRetryable, the cause-walk would loop
    // forever. Subclassing Throwable to override getCause() is the only natural way to construct
    // this state — reflection-free, no JVM flags needed.
    @SuppressWarnings("serial")
    class SelfReferential
        extends RuntimeException
    {
      SelfReferential() {
        super("self-cause");
      }

      @Override
      public synchronized Throwable getCause() {
        return this;
      }
    }
    RuntimeException selfRef = new SelfReferential();

    // Asserts that isRetryable terminates AND returns false (the guard is hit, not the
    // sibling's transient-class branches). Real-state assertion: a finite return value, not
    // a verify(mock).
    assertThat(policy.isRetryable(selfRef)).isFalse();
  }

  @Test
  public void testMaxRetries_returnsConfiguredValue() {
    assertThat(new DefaultRetryPolicy(0).maxRetries()).isEqualTo(0);
    assertThat(new DefaultRetryPolicy(5).maxRetries()).isEqualTo(5);
  }

  @Test
  public void testMaxRetries_negativeValueIsRejected() {
    assertThatThrownBy(() -> new DefaultRetryPolicy(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxRetries");
  }

  @Test
  public void testMaxRetries_supplierReadsCurrentValueOnEachCall() {
    // maxRetries must be runtime-mutable so operators can re-tune the retry budget without
    // restart. The supplier is consulted on every maxRetries() call.
    AtomicInteger live = new AtomicInteger(2);
    DefaultRetryPolicy supplierPolicy = new DefaultRetryPolicy(live::get);
    assertThat(supplierPolicy.maxRetries()).isEqualTo(2);

    live.set(7);
    assertThat(supplierPolicy.maxRetries()).isEqualTo(7);
  }

  @Test
  public void testMaxRetries_supplierCtorRejectsNullSupplier() {
    assertThatThrownBy(() -> new DefaultRetryPolicy((java.util.function.IntSupplier) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("maxRetriesSupplier");
  }

  @Test
  public void testMaxRetries_supplierReturningNegativeValueIsRejectedAtCallTime() {
    DefaultRetryPolicy supplierPolicy = new DefaultRetryPolicy(() -> -3);
    assertThatThrownBy(supplierPolicy::maxRetries)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("negative");
  }
}
