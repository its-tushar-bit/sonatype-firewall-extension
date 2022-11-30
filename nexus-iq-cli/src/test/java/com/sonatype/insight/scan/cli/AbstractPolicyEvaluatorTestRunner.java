/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.test.LogOutput;

import org.assertj.core.api.ThrowableAssertAlternative;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public abstract class AbstractPolicyEvaluatorTestRunner
{
  protected final LogOutput logOutput;

  protected final List<String> debugLogs = new ArrayList<>();

  protected final List<String> infoLogs = new ArrayList<>();

  protected final List<String> warnLogs = new ArrayList<>();

  protected final List<String> errorLogs = new ArrayList<>();

  protected Integer expectedExitCode = 0;

  protected Boolean expectedExitException = false;

  protected AbstractMap.SimpleImmutableEntry<Class<? extends Throwable>, String> exception;

  private PolicyEvaluationResult policyEvaluationResult;

  public AbstractPolicyEvaluatorTestRunner(final LogOutput logOutput) {
    this.logOutput = logOutput;
  }

  public abstract void doPolicyEvaluationRun() throws Exception;

  public abstract ClientScanResult doPolicyEvaluationScan(
      final ProprietaryConfig proprietaryConfig,
      final RestClient restClient) throws Exception;

  public AbstractPolicyEvaluatorTestRunner expectSuccessExit() {
    this.expectedExitCode = 0;
    this.expectedExitException = false;
    return this;
  }

  public AbstractPolicyEvaluatorTestRunner expectFailExit() {
    return expectFailExit(1);
  }

  public AbstractPolicyEvaluatorTestRunner expectFailExit(int exitCode) {
    this.expectedExitCode = exitCode;
    this.expectedExitException = true;
    return this;
  }

  /**
   * The CLI has the '-e' (--ignore-system-errors) option which will throw an {@link ExitException} but still return an
   * exit code of 0 (success)
   */
  public AbstractPolicyEvaluatorTestRunner expectExitExceptionButSuccessExit() {
    this.expectedExitCode = 0;
    this.expectedExitException = true;
    return this;
  }

  public AbstractPolicyEvaluatorTestRunner expectDebugLog(final String log) {
    debugLogs.add(log);
    return this;
  }

  public AbstractPolicyEvaluatorTestRunner expectInfoLog(final String log) {
    infoLogs.add(log);
    return this;
  }

  public AbstractPolicyEvaluatorTestRunner expectWarnLog(final String log) {
    warnLogs.add(log);
    return this;
  }

  public AbstractPolicyEvaluatorTestRunner expectErrorLog(final String log) {
    errorLogs.add(log);
    return this;
  }

  public AbstractPolicyEvaluatorTestRunner expectPolicyEvaluationResult(
      final PolicyEvaluationResult policyEvaluationResult)
  {
    this.policyEvaluationResult = policyEvaluationResult;
    return this;
  }

  public AbstractPolicyEvaluatorTestRunner expectException(
      final Class<? extends Throwable> exceptionClass,
      final String message)
  {
    exception = new SimpleImmutableEntry<>(exceptionClass, message);
    return this;
  }

  protected <T> T executeTest(Callable<T> func) throws Exception {
    T returnValue;

    if (expectedExitException) {
      // ensure failures throw ExitException with correct exit code
      ThrowableAssertAlternative<ExitException> result =
          assertThatExceptionOfType(ExitException.class).isThrownBy(func::call)
              .satisfies(e -> assertThat(e.getExitCode()).isEqualTo(expectedExitCode));

      // invoke any custom exception handling checks
      if (exception != null) {
        assertException(result);
      }

      // expected failure means returns nothing
      returnValue = null;
    }
    else {
      returnValue = func.call();
    }

    assertLogs();

    assertPolicyEvaluationResult();
    return returnValue;
  }

  protected abstract void assertException(final ThrowableAssertAlternative<ExitException> result);

  private void assertLogs() {
    for (String debugLog : debugLogs) {
      assertThat(logOutput).atDebugLevel().contains(debugLog);
    }
    for (String infoLog : infoLogs) {
      assertThat(logOutput).atInfoLevel().contains(infoLog);
    }
    for (String warnLog : warnLogs) {
      assertThat(logOutput).atWarnLevel().contains(warnLog);
    }
    for (String errorLog : errorLogs) {
      assertThat(logOutput).atErrorLevel().contains(errorLog);
    }
  }

  private void assertPolicyEvaluationResult() {
    if (policyEvaluationResult != null) {
      assertLogSummary(policyEvaluationResult);
    }
  }

  private void assertLogSummary(PolicyEvaluationResult expectedPolicyEvaluationResult) {
    assertThat(logOutput).atInfoLevel()
        .contains(String.format("Number of components affected: %s critical, %s severe, %s moderate",
            expectedPolicyEvaluationResult.getCriticalComponentCount(),
            expectedPolicyEvaluationResult.getSevereComponentCount(),
            expectedPolicyEvaluationResult.getModerateComponentCount()))
        .contains(String.format("Number of open policy violations: %s critical, %s severe, %s moderate",
            expectedPolicyEvaluationResult.getCriticalPolicyViolationCount(),
            expectedPolicyEvaluationResult.getSeverePolicyViolationCount(),
            expectedPolicyEvaluationResult.getModeratePolicyViolationCount()))
        .contains(String.format("Number of grandfathered policy violations: %s",
            expectedPolicyEvaluationResult.getGrandfatheredPolicyViolationCount()))
        .contains(String.format("Number of components: %s",
            expectedPolicyEvaluationResult.getTotalComponentCount()));
  }
}
