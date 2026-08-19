/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license.entitlement;

/**
 * Upsell metadata for a tier-gated feature.
 */
public class UpsellInfo
{
  private final String message;

  private final String upgradeHint;

  private final String docsUrl;

  private final String ctaUrl;

  public UpsellInfo(String message, String upgradeHint, String docsUrl, String ctaUrl) {
    this.message = message;
    this.upgradeHint = upgradeHint;
    this.docsUrl = docsUrl;
    this.ctaUrl = ctaUrl;
  }

  public String getMessage() {
    return message;
  }

  public String getUpgradeHint() {
    return upgradeHint;
  }

  public String getDocsUrl() {
    return docsUrl;
  }

  public String getCtaUrl() {
    return ctaUrl;
  }
}
