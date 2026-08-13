/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-test
package com.sonatype.insight.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.groups.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

public class LogOutputAssert
    extends AbstractAssert<LogOutputAssert, LogOutput>
{
  public static class LogEntryAssert
      extends AbstractCharSequenceAssert<LogEntryAssert, CharSequence>
  {
    private final ILoggingEvent event;

    LogEntryAssert(ILoggingEvent actual) {
      super(actual.getFormattedMessage(), LogEntryAssert.class);
      event = actual;
    }

    public LogEntryAssert hasNoThrowable() {
      IThrowableProxy proxy = event.getThrowableProxy();
      if (proxy != null) {
        failWithMessage("Expected no throwable in log entry but got %s: %s", proxy.getClassName(), proxy.getMessage());
      }
      return this;
    }

    public LogEntryAssert hasThrowable(Throwable expected) {
      if (expected == null) {
        return hasNoThrowable();
      }
      IThrowableProxy proxy = event.getThrowableProxy();
      if (proxy == null) {
        failWithMessage("Expected throwable for log entry <%s> but got none", actual);
      }
      Throwable actual = ((ThrowableProxy) proxy).getThrowable();
      assertThat(actual).isEqualTo(expected);
      return this;
    }

    public LogEntryAssert hasNestedThrowable(Throwable expected) {
      return hasNestedThrowable(Function.identity(), expected);
    }

    public LogEntryAssert hasNestedThrowable(Class<? extends Throwable> expected) {
      return hasNestedThrowable(Throwable::getClass, expected);
    }

    public LogEntryAssert hasNestedThrowable(CharSequence expected) {
      return hasNestedThrowable(Throwable::getMessage, Objects.toString(expected, null));
    }

    public LogEntryAssert hasNestedThrowable(Class<? extends Throwable> expectedClass, CharSequence expectedMessage) {
      return hasNestedThrowable((Throwable t) -> Tuple.tuple(t.getClass(), t.getMessage()),
          Tuple.tuple(expectedClass, expectedMessage));
    }

    private <T> LogEntryAssert hasNestedThrowable(Function<Throwable, T> converter, T expected) {
      if (expected == null) {
        return hasNoNestedThrowable();
      }
      assertThat(getNestedThrowables().stream().map(converter))
          .withFailMessage("Expected nested throwable for log entry <%s> but got none", actual)
          .contains(expected);
      return this;
    }

    public LogEntryAssert hasNoNestedThrowable() {
      List<Throwable> throwables = getNestedThrowables();
      if (!throwables.isEmpty()) {
        failWithMessage("Expected no nested throwable in log entry but got [%s]",
            throwables.stream().map(Throwable::toString).collect(Collectors.joining(",")));
      }
      return this;
    }

    private List<Throwable> getNestedThrowables() {
      List<Throwable> throwables = new ArrayList<>();
      if (event.getThrowableProxy() instanceof ThrowableProxy) {
        Throwable throwable = ((ThrowableProxy) event.getThrowableProxy()).getThrowable();
        while (throwable != null) {
          throwables.add(throwable);
          throwable = throwable.getCause();
        }
      }
      return throwables;
    }
  }

  private String logger;

  private Level level;

  LogOutputAssert(LogOutput actual) {
    super(actual, LogOutputAssert.class);
  }

  public LogOutputAssert fromAnyLogger() {
    logger = null;
    return this;
  }

  public LogOutputAssert fromLogger(String logger) {
    this.logger = logger;
    return this;
  }

  public LogOutputAssert fromLogger(Class<?> type) {
    return fromLogger(type.getName());
  }

  public LogOutputAssert atAnyLevel() {
    level = null;
    return this;
  }

  public LogOutputAssert atTraceLevel() {
    level = Level.TRACE;
    return this;
  }

  public LogOutputAssert atDebugLevel() {
    level = Level.DEBUG;
    return this;
  }

  public LogOutputAssert atInfoLevel() {
    level = Level.INFO;
    return this;
  }

  public LogOutputAssert atWarnLevel() {
    level = Level.WARN;
    return this;
  }

  public LogOutputAssert atErrorLevel() {
    level = Level.ERROR;
    return this;
  }

  private ListAssert<ILoggingEvent> assertEvents() {
    isNotNull();
    return assertThat(actual.getEvents()).as(description()).filteredOn(this::matchingEvent);
  }

  private String description() {
    String filterDescription = " at " + (level != null ? level.toString() : "any") + " level"
        + (logger != null ? " from " + logger : "");
    return getWritableAssertionInfo().descriptionText() + filterDescription;
  }

  private boolean matchingEvent(ILoggingEvent event) {
    if (logger != null && !logger.equals(event.getLoggerName())) {
      return false;
    }
    if (level != null && level.toInt() != event.getLevel().toInt()) {
      return false;
    }
    return true;
  }

  public LogOutputAssert isEmpty() {
    assertEvents().isEmpty();
    return this;
  }

  public LogOutputAssert isNotEmpty() {
    assertEvents().isNotEmpty();
    return this;
  }

  public LogOutputAssert contains(Consumer<LogEntryAssert> requirement) {
    assertEvents().anySatisfy(event -> requirement.accept(new LogEntryAssert(event)));
    return this;
  }

  public LogOutputAssert containsOnly(Consumer<LogEntryAssert> requirement) {
    assertEvents().allSatisfy(event -> requirement.accept(new LogEntryAssert(event)));
    return this;
  }

  public LogOutputAssert contains(CharSequence substring) {
    return as("Expecting <%s>", substring).contains(log -> log.contains(substring));
  }

  public LogOutputAssert contains(CharSequence substring, Throwable throwable) {
    return as("Expecting <%s> accompanied by <%s>", substring, throwable != null ? throwable : "no exception")
        .contains(log -> log.contains(substring).hasThrowable(throwable));
  }

  public LogOutputAssert containsNoNestedThrowable() {
    return as("Expecting no nested throwable").containsOnly(LogEntryAssert::hasNoNestedThrowable);
  }

  public LogOutputAssert containsNestedThrowable(Throwable throwable) {
    return nestedThrowableAs(throwable).contains(log -> log.hasNestedThrowable(throwable));
  }

  public LogOutputAssert containsNestedThrowable(Class<? extends Throwable> throwableClass) {
    return nestedThrowableAs(throwableClass).contains(log -> log.hasNestedThrowable(throwableClass));
  }

  public LogOutputAssert containsNestedThrowable(CharSequence throwableMessage) {
    return nestedThrowableAs(throwableMessage).contains(log -> log.hasNestedThrowable(throwableMessage));
  }

  public LogOutputAssert containsNestedThrowable(
      Class<? extends Throwable> expectedClass,
      CharSequence expectedMessage)
  {
    if (expectedClass == null && expectedMessage == null) {
      return containsNestedThrowable((Throwable) null);
    }
    if (expectedClass == null) {
      return containsNestedThrowable(expectedMessage);
    }
    if (expectedMessage == null) {
      return containsNestedThrowable(expectedClass);
    }
    return nestedThrowableAs(Tuple.tuple(expectedClass, expectedMessage)).contains(
        log -> log.hasNestedThrowable(expectedClass, expectedMessage));
  }

  private LogOutputAssert nestedThrowableAs(Object throwable) {
    return as("Expecting%s nested throwable%s", throwable != null ? "" : " no",
        throwable != null ? (" <" + throwable + ">") : "");
  }

  public LogOutputAssert containsPattern(CharSequence regex) {
    return as("Expecting pattern <%s>", regex).contains(log -> log.containsPattern(regex));
  }

  public LogOutputAssert doesNotContain(Consumer<LogEntryAssert> restriction) {
    assertEvents().noneSatisfy(event -> restriction.accept(new LogEntryAssert(event)));
    return this;
  }

  public LogOutputAssert doesNotContain(CharSequence substring) {
    return as("Not expecting <%s>", substring).doesNotContain(log -> log.contains(substring));
  }

  public LogOutputAssert doesNotContainPattern(CharSequence regex) {
    return as("Not expecting pattern <%s>", regex).doesNotContain(log -> log.containsPattern(regex));
  }
}
