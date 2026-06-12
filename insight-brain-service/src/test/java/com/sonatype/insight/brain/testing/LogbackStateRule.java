/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.rules.ExternalResource;
import org.slf4j.LoggerFactory;

/**
 * Restores the JVM-wide logback {@link LoggerContext} to its autoconfigured baseline (logback-test.xml) before and
 * after every test. Code that configures logging has to mutate the global context returned by
 * {@link LoggerFactory#getILoggerFactory()} - the production initializers hardcode it - so a test exercising that code
 * would otherwise leak appenders/levels into the shared surefire fork (reuseForks=true) and could in turn inherit
 * state left behind by an earlier test. Resetting on both sides guarantees the global logging state is identical
 * before and after each test, so no test pollutes another and none is a victim of incoming state.
 */
public class LogbackStateRule
    extends ExternalResource
{
  @Override
  protected void before() {
    resetToBaseline();
  }

  @Override
  protected void after() {
    resetToBaseline();
  }

  private static void resetToBaseline() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.reset();
    try {
      new ContextInitializer(context).autoConfig();
    }
    catch (JoranException e) {
      throw new IllegalStateException("Failed to restore logback baseline configuration", e);
    }
  }
}
