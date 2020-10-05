/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.List;

import javax.inject.Inject;

/**
 * Runs the default unit tests in {@link DefaultPolicyEvaluatorReverseProxyAuthTest}.
 */
public class JUnitDefaultPolicyEvaluatorReverseProxyAuthTest
    extends DefaultPolicyEvaluatorReverseProxyAuthTest
{
  protected DefaultPolicyEvaluator evaluator;

  @Inject
  public JUnitDefaultPolicyEvaluatorReverseProxyAuthTest(final boolean rutEnabled) {
    super(rutEnabled);
  }

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
