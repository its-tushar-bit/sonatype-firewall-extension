/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.List;

/**
 * Runs the default unit tests in {@link DefaultPolicyEvaluatorTest}.
 */
public class JUnitDefaultPolicyEvaluatorTest
    extends DefaultPolicyEvaluatorTest
{
  protected DefaultPolicyEvaluator evaluator;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    evaluator = getCLMServer().getInstance(DefaultPolicyEvaluator.class);
  }

  @Override
  protected AbstractPolicyEvaluatorTestRunner withTestRunner(final List<String> params) {
    return new JUnitPolicyEvaluatorTestRunner(params, evaluator, logOutput);
  }
}
