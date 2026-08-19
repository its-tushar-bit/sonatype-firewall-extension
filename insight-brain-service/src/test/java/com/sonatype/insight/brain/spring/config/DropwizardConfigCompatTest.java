/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class DropwizardConfigCompatTest
{
  private ListAppender<ILoggingEvent> listAppender;

  private Logger compatLogger;

  @BeforeEach
  public void setUp() {
    compatLogger = (Logger) LoggerFactory.getLogger(DropwizardConfigCompat.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    compatLogger.addAppender(listAppender);
  }

  @AfterEach
  public void tearDown() {
    compatLogger.detachAppender(listAppender);
  }

  @Test
  public void warnOnDeprecatedFields_warnsOnInheritedDeprecatedFields() {
    Child child = new Child();
    child.parentDeprecatedField = "set";
    child.childDeprecatedField = "also set";

    DropwizardConfigCompat.warnOnDeprecatedFields(child, "test-section");

    assertThat(listAppender.list)
        .extracting(ILoggingEvent::getFormattedMessage)
        .anyMatch(msg -> msg.contains("parentDeprecatedField"))
        .anyMatch(msg -> msg.contains("childDeprecatedField"));
  }

  @Test
  public void warnOnDeprecatedFields_skipsNullFields() {
    Child child = new Child();

    DropwizardConfigCompat.warnOnDeprecatedFields(child, "test-section");

    assertThat(listAppender.list).isEmpty();
  }

  @Test
  public void warnOnDeprecatedFields_skipsNonDeprecatedFields() {
    Child child = new Child();
    child.activeField = "set";

    DropwizardConfigCompat.warnOnDeprecatedFields(child, "test-section");

    assertThat(listAppender.list).isEmpty();
  }

  static class Parent
  {
    @JsonProperty
    String activeField;

    @Deprecated
    @JsonProperty
    String parentDeprecatedField;
  }

  static class Child
      extends Parent
  {
    @Deprecated
    @JsonProperty
    String childDeprecatedField;
  }
}
