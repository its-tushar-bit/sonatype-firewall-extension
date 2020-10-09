/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

/**
 * Native image config generation for the {@link DefaultPolicyEvaluatorTest}. This extends that class and will execute
 * its tests except using the {@link NativeImageConfigGenerationTestRunner}.
 *
 * See readme.md in the nexus-iq-cli-native-image module for full details
 */
public class NativeImageConfigDefaultPolicyEvaluatorTest
    extends DefaultPolicyEvaluatorTest
{
  @Override
  protected AbstractPolicyEvaluatorTestRunner withTestRunner(final List<String> params) {
    return new NativeImageConfigGenerationTestRunner(params, environmentVariables.get(), logOutput);
  }

  /**
   * This test can only run on native image as the junit tests actually start at the {@link DefaultPolicyEvaluator} and
   * not the calling {@link PolicyEvaluatorCli} class
   */
  @Test
  public void testRun_NoParameters() throws Exception {
    List<String> params = ImmutableList.of();
    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog("Usage: java -jar nexus-iq-cli.jar [options] Archives or directories to scan")
        .expectErrorLog("The following options are required: [-s | --server-url], [-i | --application-id]")
        .doPolicyEvaluationRun();
  }

}
