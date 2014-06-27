/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.junit.rules.ExternalResource;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;

/**
 * Helps to capture and verify log output.
 */
public class LogOutput
    extends ExternalResource
{
  private final String loggerName;

  private final LoggerContext loggerContext;

  private final ListAppender<ILoggingEvent> appender;

  public LogOutput() {
    this((String) null);
  }

  public LogOutput(Class<?> type) {
    this(type.getName());
  }

  public LogOutput(String loggerName) {
    this.loggerName = (loggerName != null) ? loggerName : org.slf4j.Logger.ROOT_LOGGER_NAME;
    loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    appender = new ListAppender<>();
    appender.setContext(loggerContext);
  }

  @Override
  protected void before() {
    appender.start();
    loggerContext.getLogger(loggerName).addAppender(appender);
  }

  @Override
  protected void after() {
    loggerContext.getLogger(loggerName).detachAppender(appender);
    appender.stop();
  }

  public void clear() {
    appender.list.clear();
  }

  public void assertDebug(String message) {
    assertLog(Level.DEBUG, message, null);
  }

  public void assertInfo(String message) {
    assertLog(Level.INFO, message, null);
  }

  public void assertWarn(String message) {
    assertWarn(message, null);
  }

  public void assertWarn(String message, Throwable error) {
    assertLog(Level.WARN, message, error);
  }

  public void assertError(String message) {
    assertError(message, null);
  }

  public void assertError(String message, Throwable error) {
    assertLog(Level.ERROR, message, error);
  }

  private void assertLog(Level level, String message, Throwable error) {
    for (ILoggingEvent event : appender.list) {
      if (level.equals(event.getLevel()) && message.equals(event.getFormattedMessage())
          && matches(error, event.getThrowableProxy())) {
        return;
      }
    }
    StringBuilder buffer = new StringBuilder(1024);
    buffer.append("Expected\n[").append(level).append("] ").append(message);
    if (error != null) {
      buffer.append("\n\t").append(error.getClass().getName()).append(": ").append(error.getMessage());
    }
    buffer.append("\nbut got");
    for (ILoggingEvent event : appender.list) {
      buffer.append("\n").append(event);
      IThrowableProxy tp = event.getThrowableProxy();
      if (tp != null) {
        buffer.append("\n\t").append(tp.getClassName()).append(": ").append(tp.getMessage());
      }
    }
    if (appender.list.isEmpty()) {
      buffer.append("\n(no log output)");
    }
    fail(buffer.toString());
  }

  private boolean matches(Throwable error, IThrowableProxy tp) {
    if (tp == null) {
      return error == null;
    }
    return error == ((ThrowableProxy) tp).getThrowable();
  }
}
