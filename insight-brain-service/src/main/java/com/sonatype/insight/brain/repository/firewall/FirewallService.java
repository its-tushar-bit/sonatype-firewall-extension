/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.firewall;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

/**
 * @since 1.105.0
 */
@Named
public class FirewallService
{
  private final InsightConfig insightConfig;

  private final ProductLicense productLicense;

  @Inject
  public FirewallService(
      final InsightConfig insightConfig,
      final ProductLicense productLicense)
  {
    this.insightConfig = insightConfig;
    this.productLicense = productLicense;
  }

  @Authorize(permission = Permission.READ)
  public FirewallStatusDTO getFirewallStatus() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL) ||
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY)) {
      throw new InvalidLicenseException();
    }

    FirewallStatusDTO firewallStatusDTO = new FirewallStatusDTO();
    firewallStatusDTO.experimentalFeatures
        .put(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(),
            insightConfig.isExperimentalFeatureEnabled(Feature.FIREWALL_AUTO_UNQUARANTINE));
    return firewallStatusDTO;
  }
}
