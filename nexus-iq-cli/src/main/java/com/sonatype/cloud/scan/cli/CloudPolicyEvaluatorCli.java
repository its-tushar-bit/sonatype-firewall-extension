/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.cloud.scan.cli;

/**
 * CLI implementations of {@link com.sonatype.insight.scan.cli.PolicyEvaluatorCli} to allow evaluation for Cloud
 * Services.
 *
 * @since 1.101
 */
public class CloudPolicyEvaluatorCli
    extends com.sonatype.insight.scan.cli.PolicyEvaluatorCli
{
  public static void main(String[] args) {
    new CloudPolicyEvaluatorCli().run(CloudPolicyEvaluator.class, new CloudParameters(args));
  }
}
