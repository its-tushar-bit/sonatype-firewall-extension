/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;

/**
 * @since 1.109
 */
@Named
@Singleton
public class ProductLicenseOperationalCheck
    extends AbstractOperationalCheck
{
  private final ProductLicense productLicense;

  @Inject
  public ProductLicenseOperationalCheck(ProductLicense productLicense) {
    super("product-license");
    this.productLicense = productLicense;
  }

  @Override
  protected Result check() throws Exception {
    ResultBuilder resultBuilder = Result.builder();
    if (!productLicense.isValid()) {
      resultBuilder.unhealthy();
    }
    // calculated to match similar field displayed in UI
    int remainingDays = Math.max(0,
        (int) ((productLicense.getExpirationTimestamp() - System.currentTimeMillis()) / 1000 / 60 / 60 / 24));
    resultBuilder.withDetail("remainingDays", remainingDays);
    return resultBuilder.build();
  }
}
