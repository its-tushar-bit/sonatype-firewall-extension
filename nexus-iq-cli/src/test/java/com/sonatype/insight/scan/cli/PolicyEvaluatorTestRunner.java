/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.test.LogOutput;

import org.assertj.core.api.ThrowableAssertAlternative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Helper class to run regular unit test cases for the {@link PolicyEvaluator} related test classes: {@link
 * DefaultPolicyEvaluatorTest}, {@link ExpandedCoveragePolicyEvaluatorTest}, {@link
 * DefaultPolicyEvaluatorReverseProxyAuthTest}.
 */
public class PolicyEvaluatorTestRunner
{
  private final Parameters params;

  private final PolicyEvaluator<Parameters> evaluator;

  private final LogOutput logOutput;

  private final List<String> debugLogs = new ArrayList<>();

  private final List<String> infoLogs = new ArrayList<>();

  private final List<String> warnLogs = new ArrayList<>();

  private final List<String> errorLogs = new ArrayList<>();

  private final List<Consumer<ThrowableAssertAlternative<ExitException>>> exceptions = new ArrayList<>();

  private Integer expectedExitCode = 0;

  private Boolean expectedExitException = false;

  private PolicyEvaluationResult policyEvaluationResult;

  public PolicyEvaluatorTestRunner(
      final Parameters params,
      final PolicyEvaluator<Parameters> evaluator,
      final LogOutput logOutput)
  {
    this.params = params;
    this.evaluator = evaluator;
    this.logOutput = logOutput;
  }

  public void doPolicyEvaluationRun() throws Exception {
    executeTest(() -> {
      evaluator.run(params);
      // nothing to return for the `run` method
      return null;
    });
  }

  public ClientScanResult doPolicyEvaluationScan(
      final ProprietaryConfig proprietaryConfig,
      final RestClient restClient) throws Exception
  {
    return executeTest(() -> evaluator.scan(params, new ProprietaryConfig(), restClient));
  }

  public PolicyEvaluatorTestRunner expectSuccessExit() {
    this.expectedExitCode = 0;
    this.expectedExitException = false;
    return this;
  }

  public PolicyEvaluatorTestRunner expectFailExit() {
    this.expectedExitCode = 1;
    this.expectedExitException = true;
    return this;
  }

  /**
   * The CLI has the '-e' (--ignore-system-errors) option which will throw an {@link ExitException} but still return an
   * exit code of 0 (success)
   */
  public PolicyEvaluatorTestRunner expectExitExceptionButSuccessExit() {
    this.expectedExitCode = 0;
    this.expectedExitException = true;
    return this;
  }

  public PolicyEvaluatorTestRunner expectDebugLog(final String log) {
    debugLogs.add(log);
    return this;
  }

  public PolicyEvaluatorTestRunner expectInfoLog(final String log) {
    infoLogs.add(log);
    return this;
  }

  public PolicyEvaluatorTestRunner expectWarnLog(final String log) {
    warnLogs.add(log);
    return this;
  }

  public PolicyEvaluatorTestRunner expectErrorLog(final String log) {
    errorLogs.add(log);
    return this;
  }

  public PolicyEvaluatorTestRunner expectPolicyEvaluationResult(final PolicyEvaluationResult policyEvaluationResult) {
    this.policyEvaluationResult = policyEvaluationResult;
    return this;
  }

  /**
   * The given consumer will be called after the test throws an Exception to allow for custom exception checking
   *
   * @param consumer the {@link Consumer} to invoke
   */
  public PolicyEvaluatorTestRunner expectException(Consumer<ThrowableAssertAlternative<ExitException>> consumer) {
    exceptions.add(consumer);
    return this;
  }

  private <T> T executeTest(Callable<T> func) throws Exception {
    T returnValue;

    if (expectedExitException) {
      // ensure failures throw ExitException with correct exit code
      ThrowableAssertAlternative<ExitException> result =
          assertThatExceptionOfType(ExitException.class).isThrownBy(func::call)
              .satisfies(e -> assertThat(e.getExitCode()).isEqualTo(expectedExitCode));

      // invoke any custom exception handling checks
      for (Consumer<ThrowableAssertAlternative<ExitException>> exception : exceptions) {
        exception.accept(result);
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

  private void assertLogSummary(PolicyEvaluationResult expectedPolicyEvalutionResult) {
    assertThat(logOutput).atInfoLevel()
        .contains(String.format("Number of components affected: %s critical, %s severe, %s moderate",
            expectedPolicyEvalutionResult.getCriticalComponentCount(),
            expectedPolicyEvalutionResult.getSevereComponentCount(),
            expectedPolicyEvalutionResult.getModerateComponentCount()))
        .contains(String.format("Number of open policy violations: %s critical, %s severe, %s moderate",
            expectedPolicyEvalutionResult.getCriticalPolicyViolationCount(),
            expectedPolicyEvalutionResult.getSeverePolicyViolationCount(),
            expectedPolicyEvalutionResult.getModeratePolicyViolationCount()))
        .contains(String.format("Number of grandfathered policy violations: %s",
            expectedPolicyEvalutionResult.getGrandfatheredPolicyViolationCount()))
        .contains(String.format("Number of components: %s",
            expectedPolicyEvalutionResult.getTotalComponentCount()));
  }
}
