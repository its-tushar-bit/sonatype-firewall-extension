/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

public class PolicyEvaluatorCli
    extends AbstractPolicyEvaluatorCli
{
  public static void main(String[] args) {
    Parameters params = new Parameters(args);

    Class<? extends PolicyEvaluator<Parameters>> policyEvaluatorClass = params
        .isExpandedCoverageMode() ? ExpandedCoveragePolicyEvaluator.class : DefaultPolicyEvaluator.class;

    new PolicyEvaluatorCli().run(policyEvaluatorClass, params);
  }
}
