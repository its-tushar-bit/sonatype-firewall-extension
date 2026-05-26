/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import com.sonatype.insight.brain.product.license.ProductLicense;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.boot.health.contributor.Health;

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
  public Health check() throws Exception {
    Health.Builder healthBuilder = Health.up();
    if (!productLicense.isValid()) {
      healthBuilder.down();
    }
    // calculated to match similar field displayed in UI
    int remainingDays = Math.max(0,
        (int) ((productLicense.getExpirationTimestamp() - System.currentTimeMillis()) / 1000 / 60 / 60 / 24));
    healthBuilder.withDetail("remainingDays", remainingDays);
    return healthBuilder.build();
  }
}
