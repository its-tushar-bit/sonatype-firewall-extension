/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

public class ZScalerQuota
{
  private int uniqueUrlsProvisioned;

  private int remainingUrlsQuota;

  public ZScalerQuota() {
    // empty
  }

  public ZScalerQuota(final int uniqueUrlsProvisioned, final int remainingUrlsQuota) {
    this.uniqueUrlsProvisioned = uniqueUrlsProvisioned;
    this.remainingUrlsQuota = remainingUrlsQuota;
  }

  public void setRemainingUrlsQuota(final int remainingUrlsQuota) {
    this.remainingUrlsQuota = remainingUrlsQuota;
  }

  public int getRemainingUrlsQuota() {
    return remainingUrlsQuota;
  }

  public void setUniqueUrlsProvisioned(final int uniqueUrlsProvisioned) {
    this.uniqueUrlsProvisioned = uniqueUrlsProvisioned;
  }

  public int getUniqueUrlsProvisioned() {
    return uniqueUrlsProvisioned;
  }
}
