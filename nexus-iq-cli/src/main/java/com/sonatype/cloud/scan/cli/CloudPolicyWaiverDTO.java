/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.cloud.scan.cli;

/**
 * Simple DTO for Policy Waivers
 *
 * @since 1.101
 */
public class CloudPolicyWaiverDTO
{
  private String hash;

  private String policyId;

  public String getHash() {
    return hash;
  }

  public void setHash(final String hash) {
    this.hash = hash;
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(final String policyId) {
    this.policyId = policyId;
  }
}
