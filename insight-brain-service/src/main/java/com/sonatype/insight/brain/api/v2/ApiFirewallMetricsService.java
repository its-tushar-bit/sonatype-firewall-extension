/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
public class ApiFirewallMetricsService
{
  private  final FirewallMetricsDAO firewallMetricsDAO;

  private final ProductLicense productLicense;

  @Inject
  public ApiFirewallMetricsService(final FirewallMetricsDAO firewallMetricsDAO,
                                   final ProductLicense productLicense)
  {
    this.firewallMetricsDAO = firewallMetricsDAO;
    this.productLicense = productLicense;
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  private void checkProductLicense() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE) ||
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY)) {
      throw new InvalidLicenseException();
    }
  }

  Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> getFirewallMetrics() {
    checkProductLicense();
    checkReadPermission(RepositoryContainer.SINGLETON);
    Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> resultMap = firewallMetricsDAO.getMetricsValueByName();
    for (FirewallMetricsName firewallMetricsName: FirewallMetricsName.values()) {
      resultMap.putIfAbsent(firewallMetricsName, new ApiFirewallMetricsResultDTO(0, null));
    }
    return resultMap;
  }
}
