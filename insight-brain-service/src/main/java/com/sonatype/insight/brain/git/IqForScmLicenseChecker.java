/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;

/**
 * Used by the IQ for SCM feature to check if the required license feature is present for each IQ for SCM operation.
 */
@Named
@Singleton
public class IqForScmLicenseChecker
{
  private final ProductLicense productLicense;

  @Inject
  public IqForScmLicenseChecker(final ProductLicense productLicense) {
    this.productLicense = productLicense;
  }

  public boolean isPullRequestCommentingSupported() {
    return hasAutomationFeature();
  }

  public boolean isPullRequestRemediationSupported() {
    return hasAutomationFeature();
  }

  public boolean isCommitStatusSupported() {
    return hasNotificationsFeature();
  }

  /**
   * Use this method when you want to check if at least one of the license features required by IQ for SCM is enabled
   */
  public boolean isIqForScmSupported() {
    return hasAutomationFeature() || hasNotificationsFeature();
  }

  private boolean hasAutomationFeature() {
    return productLicense.hasFeature(LicensedFeature.AUTOMATION);
  }

  private boolean hasNotificationsFeature() {
    return productLicense.hasFeature(LicensedFeature.NOTIFICATIONS);
  }
}
