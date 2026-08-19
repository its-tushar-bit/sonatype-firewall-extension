/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-test
package com.sonatype.insight.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.slf4j.LoggerFactory;

/**
 * Helps to capture and verify log output.
 */
public class LogOutput
    extends ExternalResource
    implements AssertProvider<LogOutputAssert>, BeforeEachCallback, AfterEachCallback
{
  private final String[] loggerNames;

  private final LoggerContext loggerContext;

  private final ListAppender<ILoggingEvent> appender;

  private final Map<String, Level> originalLevels;

  private final LoggerContextListener loggerContextListener = new LoggerContextListener()
  {
    @Override
    public boolean isResetResistant() {
      return true;
    }

    @Override
    public void onReset(LoggerContext context) {
      // restore log capture after e.g. Dropwizard reset logging
      configureLoggers();
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
    }

    @Override
    public void onStart(LoggerContext context) {
    }

    @Override
    public void onStop(LoggerContext context) {
    }
  };

  public LogOutput(Class<?>... types) {
    this(0, types);
  }

  public LogOutput(int ancestors, Class<?>... types) {
    this(getLoggerNames(ancestors, types));
  }

  private static String[] getLoggerNames(int ancestors, Class<?>... types) {
    String[] loggerNames = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      loggerNames[i] = getLoggerName(types[i].getName(), ancestors);
    }
    return loggerNames;
  }

  private static String getLoggerName(String typeName, int ancestors) {
    String loggerName = typeName;
    for (int i = 0; i < ancestors; i++) {
      loggerName = loggerName.substring(0, loggerName.lastIndexOf('.'));
    }
    return loggerName;
  }

  public LogOutput(String... loggerNames) {
    if (loggerNames == null || loggerNames.length <= 0) {
      throw new IllegalArgumentException();
    }
    this.loggerNames = loggerNames;
    originalLevels = new HashMap<>();
    loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    appender = new ListAppender<>();
    appender.list = Collections.synchronizedList(appender.list);
    appender.setContext(loggerContext);
  }

  @Override
  public void before() {
    configureLoggers();
    // NOTE: try to remove listener first to ensure we do not end up with duplicate registrations
    loggerContext.removeListener(loggerContextListener);
    loggerContext.addListener(loggerContextListener);
  }

  private void configureLoggers() {
    appender.start();
    for (String loggerName : loggerNames) {
      Logger logger = loggerContext.getLogger(loggerName);
      originalLevels.putIfAbsent(loggerName, logger.getLevel());
      // The logging level must be at least the lowest level this helper class is able to assert,
      // which currently is DEBUG. If we add support for TRACE logging, we must change the level here.
      // Although the default level is DEBUG, we must set it here explicitly because it might have been changed
      // elsewhere. For ex, DropWizard sets the log level to INFO for everything when it starts.
      logger.setLevel(Level.DEBUG);
      logger.detachAppender(appender);
      logger.addAppender(appender);
    }
  }

  @Override
  protected void after() {
    loggerContext.removeListener(loggerContextListener);
    for (String loggerName : loggerNames) {
      Logger logger = loggerContext.getLogger(loggerName);
      logger.detachAppender(appender);
      logger.setLevel(originalLevels.remove(loggerName));
    }
    appender.stop();
  }

  // JUnit 5 registration (via @RegisterExtension) — mirrors the JUnit 4 @Rule lifecycle so a single LogOutput
  // instance works under both engines during the JUnit 4 -> 5 migration.
  @Override
  public void beforeEach(final ExtensionContext context) {
    before();
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    after();
  }

  public void setLogLevel(Level level) {
    for (String loggerName : loggerNames) {
      Logger logger = loggerContext.getLogger(loggerName);
      logger.setLevel(level);
    }
  }

  public void clear() {
    appender.list.clear();
  }

  @Override
  public LogOutputAssert assertThat() {
    return new LogOutputAssert(this);
  }

  List<ILoggingEvent> getEvents() {
    synchronized (appender.list) {
      return new ArrayList<>(appender.list);
    }
  }

  public List<String> getMessages(String logger) {
    return getMessages(logger, null);
  }

  public List<String> getDebugMessages(String logger) {
    return getMessages(logger, Level.DEBUG);
  }

  public List<String> getInfoMessages(String logger) {
    return getMessages(logger, Level.INFO);
  }

  public List<String> getWarnMessages(String logger) {
    return getMessages(logger, Level.WARN);
  }

  public List<String> getErrorMessages(String logger) {
    return getMessages(logger, Level.ERROR);
  }

  public List<Throwable> getThrowables(String logger) {
    return getThrowables(logger, null);
  }

  public List<Throwable> getDebugThrowables(String logger) {
    return getThrowables(logger, Level.DEBUG);
  }

  public List<Throwable> getInfoThrowables(String logger) {
    return getThrowables(logger, Level.INFO);
  }

  public List<Throwable> getWarnThrowables(String logger) {
    return getThrowables(logger, Level.WARN);
  }

  public List<Throwable> getErrorThrowables(String logger) {
    return getThrowables(logger, Level.ERROR);
  }

  private List<String> getMessages(String logger, Level level) {
    return getEventData(logger, level, (event, collection) -> collection.add(event.getFormattedMessage()));
  }

  private List<Throwable> getThrowables(String logger, Level level) {
    return getEventData(logger, level, (event, collection) -> {
      if (event.getThrowableProxy() instanceof ThrowableProxy) {
        collection.add(((ThrowableProxy) event.getThrowableProxy()).getThrowable());
      }
    });
  }

  private <T> List<T> getEventData(String logger, Level level, BiConsumer<ILoggingEvent, Collection<T>> handler) {
    List<T> messages = new ArrayList<>();
    synchronized (appender.list) {
      for (ILoggingEvent event : appender.list) {
        if ((level == null || level.equals(event.getLevel()))
            && (logger == null || logger.equals(event.getLoggerName())))
        {
          handler.accept(event, messages);
        }
      }
    }
    return messages;
  }
}
