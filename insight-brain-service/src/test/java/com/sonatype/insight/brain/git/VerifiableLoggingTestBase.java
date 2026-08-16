/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.AbstractDataTest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.groups.Tuple;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class VerifiableLoggingTestBase
    extends AbstractDataTest
{
  private ListAppender<ILoggingEvent> listAppender;

  private final Class<?> classUnderTest;

  public VerifiableLoggingTestBase(Class<?> classUnderTest) {
    this.classUnderTest = classUnderTest;
  }

  @Before
  @BeforeEach
  public void setup() {
    Logger log = (Logger) LoggerFactory.getLogger(classUnderTest);
    listAppender = new ListAppender<>();
    listAppender.start();
    log.addAppender(listAppender);
  }

  @After
  @AfterEach
  public void tearDown() {
    Logger log = (Logger) LoggerFactory.getLogger(classUnderTest);
    log.detachAppender(listAppender);
    listAppender.stop();
  }

  protected Tuple debug(String message) {
    return tuple(message, Level.DEBUG);
  }

  protected Tuple info(String message) {
    return tuple(message, Level.INFO);
  }

  protected Tuple warn(String message) {
    return tuple(message, Level.WARN);
  }

  protected Tuple error(String message) {
    return tuple(message, Level.ERROR);
  }

  protected Tuple trace(String message) {
    return tuple(message, Level.TRACE);
  }

  protected void assertThatLogMessagesEqual(Tuple... logMessageTuples) {
    assertThat(listAppender.list)
        .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
        .containsExactly(logMessageTuples);
  }

  protected void assertThatLogMessagesContain(Tuple... logMessageTuples) {
    assertThat(listAppender.list)
        .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
        .contains(logMessageTuples);
  }

  protected void assertThatLogMessagesContainSubsequence(Tuple... logMessageTuples) {
    assertThat(listAppender.list)
        .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
        .containsSubsequence(logMessageTuples);
  }

  protected int logMessageCount() {
    return listAppender.list.size();
  }

  protected boolean logMessagesContainTuple(Tuple logMessageTuple) {
    return listAppender.list.stream()
        .anyMatch(e -> logMessageTuple.equals(tuple(e.getFormattedMessage(), e.getLevel())));
  }

  protected void assertNoErrorsInLogs() {
    assertNoLogEntriesAtLevel(Level.ERROR);
  }

  protected void assertNoWarningsInLogs() {
    assertNoLogEntriesAtLevel(Level.WARN);
  }

  private void assertNoLogEntriesAtLevel(Level level) {
    for (ILoggingEvent log : listAppender.list) {
      assertThat(log.getLevel()).isNotEqualTo(level);
    }
  }
}
