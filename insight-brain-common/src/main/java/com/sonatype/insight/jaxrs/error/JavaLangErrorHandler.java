/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-utils
package com.sonatype.insight.jaxrs.error;

import java.sql.SQLException;
import java.util.function.Supplier;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a Throwable instance, it searches for an instance of java.lang.Error in the causality chain(s).
 * If a fatal java.lang.Error instance is found, it logs it and exits the JVM if the exitOnFatalError flag is true.
 *
 * @since 2.1.7
 */
@Named
@Singleton
public class JavaLangErrorHandler
{
  private static final Logger log = LoggerFactory.getLogger(JavaLangErrorHandler.class);

  private Supplier<Boolean> exitOnFatalErrorSupplier = () -> true;

  private Error lastFatalError;

  public void setExitOnFatalErrorSupplier(Supplier<Boolean> supplier) {
    this.exitOnFatalErrorSupplier = supplier;
  }

  public void handle(Throwable exception) {
    Error error = findFatalError(exception);
    if (error != null) {
      try {
        lastFatalError = error;
        boolean shouldExit = shouldExitOnFatalError();
        // Try to log to stderr before trying the standard logging because
        // the standard logging may not be operational at this point.
        error.printStackTrace();
        exception.printStackTrace();
        System.err.println("exitOnFatalError=" + shouldExit);
        log.error(error.getMessage(), error);
        log.error(exception.getMessage(), exception);
        log.info("exitOnFatalError={}", shouldExit);
      }
      finally {
        handleExit(Runtime.getRuntime());
      }
    }
  }

  public void handleExit(Runtime runtime) {
    boolean shouldExit = shouldExitOnFatalError();
    if (shouldExit) {
      try {
        log.error("Exiting on fatal error"
            + ", see https://links.sonatype.com/products/lifecycle/docs/automatic-shutdown-on-errors for details.");
      }
      finally {
        runtime.exit(1);
      }
    }
  }

  public Error getLastFatalError() {
    return lastFatalError;
  }

  private Error findFatalError(Throwable exception) {
    if (exception == null) {
      return null;
    }

    // Explicit per-request opt-out. A fault that implements NonFatalRequestFault has been
    // classified by the layer that raised it as confined to a single request -- the canonical case
    // (CLM-44515) is a memory-mapped Lucene search read that hit a mapped-page SIGBUS, which
    // HotSpot surfaces as an "unsafe memory access" InternalError after safely recovering. It
    // surfaces as an HTTP 5xx for the offending request only and must not be able to terminate the
    // JVM (DoS via a user-supplied advanced-search query). This check runs before the
    // VirtualMachineError branch and does not walk the cause, so the marker short-circuits even
    // though it wraps a VirtualMachineError (the InternalError) as its cause. The marker is only
    // ever worn by faults the raising layer has proven request-scoped (see
    // com.sonatype.insight.brain.search.lucene.SearchMmapFaultAspect, which matches the specific
    // mmap read fault at the Lucene search-read boundary), so nothing genuinely JVM-fatal hides
    // beneath it. Every other mmap/Unsafe consumer -- and the Lucene writer/merge path -- is
    // unaffected: such a fault stays fatal exactly as before.
    if (exception instanceof NonFatalRequestFault) {
      return null;
    }

    if (exception instanceof VirtualMachineError) {
      return (Error) exception;
    }

    Error error = findFatalError(exception.getCause());
    if (error != null) {
      return error;
    }

    if (exception instanceof SQLException) {
      SQLException sqlException = (SQLException) exception;
      return findFatalError(sqlException.getNextException());
    }

    return null;
  }

  private boolean shouldExitOnFatalError() {
    try {
      return exitOnFatalErrorSupplier.get();
    }
    catch (Exception e) {
      log.error("Error checking exit-on-fatal-error configuration, using default value of true", e);
      return true;
    }
  }
}
