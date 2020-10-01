/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli.nativeimage;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.cli.Parameters;
import com.sonatype.insight.scan.cli.PolicyEvaluator;
import com.sonatype.insight.scan.cli.PolicyEvaluatorTestRunner;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.test.LogOutput;

/**
 * Custom test runner to generate the configuration files for the `native-image` tooling. Rather than a traditional
 * test execution and result assertion process (see {@link PolicyEvaluatorTestRunner} for that), this class will
 * execute the actual CLI JAR file with the given parameters, along with the native-image tracing agent, in order to
 * generate the config files. The results of the scan will still be asserted.
 */
public class NativeImageConfigGenerationTestRunner
    extends PolicyEvaluatorTestRunner
{
  public NativeImageConfigGenerationTestRunner(
      final Parameters params,
      final PolicyEvaluator evaluator,
      final LogOutput logOutput)
  {
    super(params, evaluator, logOutput);
  }

  @Override
  public void doPolicyEvaluationRun() throws Exception {
    /*
    TODO INT-3333
    This implementation will invoke the 'java' command on the obfuscated JAR along with the `native-image` tracing agent
    to build up the configuration files
    */
    // For now, just run the super impl so the tests don't fail
    super.doPolicyEvaluationRun();
  }

  @Override
  public ClientScanResult doPolicyEvaluationScan(
      final ProprietaryConfig proprietaryConfig, final RestClient restClient) throws Exception
  {
    /*
    TODO INT-3333
    This implementation will invoke the 'java' command on the obfuscated JAR along with the `native-image` tracing agent
    to build up the configuration files
     */
    // For now, just run the super impl so the tests don't fail
    return super.doPolicyEvaluationScan(proprietaryConfig, restClient);
  }
}
