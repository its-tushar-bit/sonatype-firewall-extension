/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

/**
 * @since 1.24
 */
public class TwistlockPolicyEvaluatorCli
    extends AbstractPolicyEvaluatorCli
{
  public static void main(String[] args) {
    TwistlockParameters params = new TwistlockParameters(args);

    TwistlockPolicyEvaluatorCli policyEvaluatorCli = new TwistlockPolicyEvaluatorCli();
    policyEvaluatorCli.run(TwistlockPolicyEvaluator.class, params);
  }
}
