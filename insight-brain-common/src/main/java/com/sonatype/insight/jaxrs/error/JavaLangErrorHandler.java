/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-utils
package com.sonatype.insight.jaxrs.error;

import java.sql.SQLException;
import java.util.function.Supplier;
import javax.inject.Named;
import javax.inject.Singleton;

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
