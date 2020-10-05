/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import java.util.List;

import com.sonatype.insight.scan.cli.AbstractPolicyEvaluatorTest;
import com.sonatype.insight.scan.cli.NativeImageConfigGenerationTestRunner;

import org.assertj.core.api.AssertProvider;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.owasp.dependencycheck.Engine;

/**
 * Encapsulate the nuances of logging the external process for native image config generation. This allows the {@link
 * LogOutput} instance to remain in place in base class {@link AbstractPolicyEvaluatorTest} and still continue to
 * work for the native image test cases.
 *
 * Specifically processes have two output streams: stdout and stderr. The zt-exec library allows you to send these each
 * to a logger so {@link NativeImageConfigGenerationTestRunner} sends them to INFO and ERROR respectively
 */
public class NativeImageLogOutputDecorator
    extends LogOutput
    implements AssertProvider<LogOutputAssert>
{
  private final LogOutput logOutput;

  public NativeImageLogOutputDecorator(final LogOutput logOutput) {
    super(1, AbstractPolicyEvaluatorTest.class, Engine.class);
    this.logOutput = logOutput;
  }

  @Override
  public void before() {
    logOutput.before();
  }

  @Override
  public void clear() {
    logOutput.clear();
  }

  @Override
  public LogOutputAssert assertThat() {
    return new LogOutputAssert(logOutput)
    {
      /**
       * Override {@link #atDebugLevel()} to return the INFO level messages as zt-exec has pumped all stdout to INFO
       */
      @Override
      public LogOutputAssert atDebugLevel() {
        return super.atInfoLevel();
      }

      /**
       * Override {@link #atWarnLevel()} to return the ERROR level messages as zt-exec has pumped all stderr to ERROR
       */
      @Override
      public LogOutputAssert atWarnLevel() {
        return super.atErrorLevel();
      }
    };
  }

  @Override
  public List<String> getMessages(final String logger) {
    return logOutput.getMessages(logger);
  }

  /**
   * Return INFO messages when asked for DEBUG messages External execution of IQ CLI we only get stdout and stderr.
   * DEBUG will go to stdout. See logback-graal.xml.
   */
  @Override
  public List<String> getDebugMessages(final String logger) {
    return logOutput.getInfoMessages(logger);
  }

  @Override
  public List<String> getInfoMessages(final String logger) {
    return logOutput.getInfoMessages(logger);
  }

  /**
   * Return ERROR messages when asked for WARN messages External execution of IQ CLI we only get stdout and stderr. WARN
   * will go to stderr. See logback-graal.xml.
   */
  @Override
  public List<String> getWarnMessages(final String logger) {
    return logOutput.getErrorMessages(logger);
  }

  @Override
  public List<String> getErrorMessages(final String logger) {
    return logOutput.getErrorMessages(logger);
  }

  @Override
  public Statement apply(
      final Statement base,
      final Description description)
  {
    return logOutput.apply(base, description);
  }
}
