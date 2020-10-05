/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.List;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.test.LogOutput;

import org.assertj.core.api.ThrowableAssertAlternative;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper class to run regular unit test cases for the {@link PolicyEvaluator} related test classes: {@link
 * DefaultPolicyEvaluatorTest}, {@link ExpandedCoveragePolicyEvaluatorTest}, {@link
 * DefaultPolicyEvaluatorReverseProxyAuthTest}.
 */
public class JUnitPolicyEvaluatorTestRunner
    extends AbstractPolicyEvaluatorTestRunner
{
  private final Parameters params;

  private final PolicyEvaluator evaluator;

  public JUnitPolicyEvaluatorTestRunner(
      final List<String> params,
      final PolicyEvaluator evaluator,
      final LogOutput logOutput)
  {
    super(logOutput);
    this.params = new Parameters(params.toArray(new String[0]));
    this.evaluator = evaluator;
  }

  @Override
  public void doPolicyEvaluationRun() throws Exception {
    executeTest(() -> {
      evaluator.run(params);
      // nothing to return for the `run` method
      return null;
    });
  }

  @Override
  public ClientScanResult doPolicyEvaluationScan(
      final ProprietaryConfig proprietaryConfig,
      final RestClient restClient) throws Exception
  {
    return executeTest(() -> evaluator.scan(params, proprietaryConfig, restClient));
  }

  @Override
  protected void assertException(final ThrowableAssertAlternative<ExitException> result) {
    result.withCauseInstanceOf(exception.getKey())
        .satisfies(e -> assertThat(e.getCause().getMessage()).isEqualTo(exception.getValue()));
  }
}
