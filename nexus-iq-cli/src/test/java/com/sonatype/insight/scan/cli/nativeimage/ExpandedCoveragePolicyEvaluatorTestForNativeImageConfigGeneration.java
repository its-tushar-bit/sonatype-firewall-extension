/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli.nativeimage;

import com.sonatype.insight.scan.cli.ExpandedCoveragePolicyEvaluatorTest;
import com.sonatype.insight.scan.cli.Parameters;
import com.sonatype.insight.scan.cli.PolicyEvaluatorTestRunner;

/**
 * Native image config generation for the {@link ExpandedCoveragePolicyEvaluatorTest}. This extends that class and will
 * execute its tests except using the {@link NativeImageConfigGenerationTestRunner}.
 *
 * See readme.md in the nexus-iq-cli-native-image module for full details
 */
public class ExpandedCoveragePolicyEvaluatorTestForNativeImageConfigGeneration
    extends ExpandedCoveragePolicyEvaluatorTest
{
  @Override
  protected PolicyEvaluatorTestRunner withTestRunner(final Parameters params) {
    return new NativeImageConfigGenerationTestRunner(params, evaluator, logOutput);
  }

  @Override
  protected String getTestClassName() {
    return getClass().getSuperclass().getSimpleName();
  }
}
