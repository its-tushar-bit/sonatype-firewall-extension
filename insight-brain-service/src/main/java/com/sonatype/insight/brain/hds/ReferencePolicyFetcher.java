/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.policy.PolicyExportResult;

@Named
public class ReferencePolicyFetcher
{
  private static final int REFERENCE_POLICY_VERSION = 2;

  private static final String REFERENCE_POLICY_PATH = "rest/referencePolicies/v" + REFERENCE_POLICY_VERSION;

  private final HdsClient hdsClient;

  @Inject
  public ReferencePolicyFetcher(final HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  public PolicyExportResult getReferencePolicies() throws IOException {
    return hdsClient.get(PolicyExportResult.class, REFERENCE_POLICY_PATH, null);
  }
}
