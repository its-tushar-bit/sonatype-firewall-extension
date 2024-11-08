/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.Set;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DeveloperEnablementService
{
  private static final Logger log = LoggerFactory.getLogger(DeveloperEnablementService.class);

  static final String MIN_DEVELOPER_COMPATIBLE_VERSION = "1.180.0-min";

  private static final Set<String> LIFECYCLE_PRODUCTS = Set.of(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
      ProductLicenseDetails.PRODUCT_TEAMS_EDITION);

  static final String HDS_DEVELOPER_VERSION_UPPER_BOUND_PATH = "rest/productLicense/developer-upper-bound";

  private final VersionService versionService;

  private final ProductLicense productLicense;

  private final Provider<HdsClient> hdsClientProvider;

  @Inject
  public DeveloperEnablementService(
      final VersionService versionService,
      final ProductLicense productLicense,
      final Provider<HdsClient> hdsClientProvider)
  {
    this.versionService = versionService;
    this.productLicense = productLicense;
    this.hdsClientProvider = hdsClientProvider;
  }

  public boolean shouldEnableDeveloperProduct() {
    final boolean hasLifecycleProduct = LIFECYCLE_PRODUCTS.stream().anyMatch(productLicense::hasProduct);
    final String version = versionService.getVersion();
    final boolean isEligibleVersion = isEligibleVersion();
    log.trace("Has Lifecycle product = {} ; Has eligible version = {}, version = {}", hasLifecycleProduct,
        isEligibleVersion, version);
    return hasLifecycleProduct && isEligibleVersion;
  }

  private boolean isEligibleVersion() {
    final String version = versionService.getVersion();
    if (version == null) {
      return false;
    }
    final String upperBoundVersion = getVersionUpperBound();
    if (StringUtils.isNotEmpty(upperBoundVersion)) {
      return versionService.compare(version, MIN_DEVELOPER_COMPATIBLE_VERSION) >= 0 &&
          versionService.compare(version, upperBoundVersion) <= 0;
    }
    return versionService.compare(version, MIN_DEVELOPER_COMPATIBLE_VERSION) >= 0;
  }

  private String getVersionUpperBound() {
    try {
      return hdsClientProvider.get().get(String.class, HDS_DEVELOPER_VERSION_UPPER_BOUND_PATH);
    }
    catch (final Exception e) {
      // Do not block ex. scenarios where they may not be a license installed
      log.error(e.getMessage(), e);
      return null;
    }
  }
}
