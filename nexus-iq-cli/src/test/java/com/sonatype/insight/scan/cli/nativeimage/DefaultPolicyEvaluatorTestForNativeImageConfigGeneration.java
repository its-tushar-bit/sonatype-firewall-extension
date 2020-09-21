/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli.nativeimage;

import com.sonatype.insight.scan.cli.DefaultPolicyEvaluatorTest;
import com.sonatype.insight.scan.cli.Parameters;
import com.sonatype.insight.scan.cli.PolicyEvaluatorTestRunner;

/**
 * Native image config generation for the {@link DefaultPolicyEvaluatorTest}. This extends that class and will execute
 * its tests except using the {@link NativeImageConfigGenerationTestRunner}.
 *
 * See readme.md in the nexus-iq-cli-native-image module for full details
 */
public class DefaultPolicyEvaluatorTestForNativeImageConfigGeneration
    extends DefaultPolicyEvaluatorTest
{
  @Override
  protected PolicyEvaluatorTestRunner withTestRunner(final Parameters params) {
    return new NativeImageConfigGenerationTestRunner(params, evaluator, logOutput);
  }
}
