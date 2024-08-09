/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Set;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DeveloperEnablementService
{
  private static final Logger log = LoggerFactory.getLogger(DeveloperEnablementService.class);

  private static final String MIN_DEVELOPER_COMPATIBLE_VERSION = "1.180.0-min";

  private static final Set<String> LIFECYCLE_PRODUCTS = Set.of(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
      ProductLicenseDetails.PRODUCT_TEAMS_EDITION);

  private final VersionService versionService;

  private final ProductLicense productLicense;

  @Inject
  public DeveloperEnablementService(
      final VersionService versionService,
      final ProductLicense productLicense)
  {
    this.versionService = versionService;
    this.productLicense = productLicense;
  }

  public boolean shouldEnableDeveloperProduct() {
    final boolean hasLifecycleProduct = LIFECYCLE_PRODUCTS.stream().anyMatch(productLicense::hasProduct);
    final String version = versionService.getVersion();
    final boolean isEligibleVersion =
        version != null && versionService.compare(version, MIN_DEVELOPER_COMPATIBLE_VERSION) >= 0;
    log.info("Has Lifecycle product = {} ; Has eligible version = {}, version = {}", hasLifecycleProduct,
        isEligibleVersion, version);
    return hasLifecycleProduct && isEligibleVersion;
  }
}
