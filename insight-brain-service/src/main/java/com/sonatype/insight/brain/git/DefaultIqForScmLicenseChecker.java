/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;

/**
 * Used by the IQ for SCM feature to check if the required license feature is present for each IQ for SCM operation.
 */
@Named
@Singleton
public class DefaultIqForScmLicenseChecker implements IqForScmLicenseChecker
{
  private final ProductLicense productLicense;

  @Inject
  public DefaultIqForScmLicenseChecker(final ProductLicense productLicense) {
    this.productLicense = productLicense;
  }

  @Override
  public boolean isPullRequestCommentingSupported() {
    return hasAutomationFeature();
  }

  @Override
  public boolean isPullRequestRemediationSupported() {
    return hasAutomationFeature();
  }

  @Override
  public boolean isCommitStatusSupported() {
    return hasNotificationsFeature();
  }

  /**
   * Use this method when you want to check if at least one of the license features required by IQ for SCM is enabled
   */
  @Override
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
