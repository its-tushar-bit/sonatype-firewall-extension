/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Default {@link RetryPolicy}: retries {@link SocketTimeoutException}, {@link ConnectException},
 * {@link SQLTransientException}, and DBCP pool-timeout {@link SQLException}s identified by
 * message prefix {@code "Cannot get a connection"} (CLM-40039 Section 7.1). All other throwables
 * are treated as permanent.
 * <p>
 * DBCP 2.x does not ship a dedicated transient-connection exception class on the service module's
 * compile classpath; pool-timeout failures surface as plain {@link SQLException} with the message
 * {@code "Cannot get a connection, pool error ..."}. We match by message prefix rather than FQCN.
 * <p>
 * Cause unwrapping walks at most one level deep. jOOQ wraps SQL exceptions in
 * {@code DataAccessException} with a direct cause; callers in this codebase rarely re-wrap
 * beyond that. Walking arbitrarily deep risks hiding bugs in suppressed-cause chains.
 * <p>
 * The retry limit is supplied as an {@link IntSupplier} so callers can wire it to a runtime-mutable
 * system-configuration property. A static-int convenience constructor is retained for tests and
 * call sites that want a fixed limit.
 */
public class DefaultRetryPolicy
    implements RetryPolicy
{
  private static final String DBCP_POOL_TIMEOUT_PREFIX = "Cannot get a connection";

  private final IntSupplier maxRetriesSupplier;

  public DefaultRetryPolicy(final int maxRetries) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries must be non-negative, was " + maxRetries);
    }
    this.maxRetriesSupplier = () -> maxRetries;
  }

  public DefaultRetryPolicy(final IntSupplier maxRetriesSupplier) {
    this.maxRetriesSupplier = Objects.requireNonNull(maxRetriesSupplier, "maxRetriesSupplier must not be null");
  }

  @Override
  public boolean isRetryable(final Throwable throwable) {
    if (throwable == null) {
      return false;
    }
    if (isTransientType(throwable)) {
      return true;
    }
    Throwable cause = throwable.getCause();
    return cause != null && cause != throwable && isTransientType(cause);
  }

  @Override
  public int maxRetries() {
    int value = maxRetriesSupplier.getAsInt();
    if (value < 0) {
      throw new IllegalStateException("maxRetries supplier returned negative value: " + value);
    }
    return value;
  }

  private static boolean isTransientType(final Throwable t) {
    if (t instanceof SocketTimeoutException
        || t instanceof ConnectException
        || t instanceof SQLTransientException)
    {
      return true;
    }
    if (t instanceof SQLException) {
      String message = t.getMessage();
      return message != null && message.startsWith(DBCP_POOL_TIMEOUT_PREFIX);
    }
    return false;
  }
}
