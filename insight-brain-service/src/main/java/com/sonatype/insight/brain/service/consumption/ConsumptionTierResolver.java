/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.EnumSet;
import java.util.Set;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicensingModel;

/**
 * Resolves the consumption tier string from a {@link ProductLicense}.
 * Single source of truth for tier-resolution across the consumption subsystem
 * (filter and REST resource).
 *
 * @since 1.204
 */
public final class ConsumptionTierResolver
{
  private ConsumptionTierResolver() {
    // utility
  }

  /**
   * Returns the name of the first configured licensing model, or
   * {@link ProductLicensingModel#APP_BASED} as a fallback when no licensing
   * models are configured.
   */
  public static String resolveTier(final ProductLicense productLicense) {
    Set<ProductLicensingModel> models = productLicense.getLicensingModels();
    if (models != null && !models.isEmpty()) {
      return EnumSet.copyOf(models).iterator().next().name();
    }
    return ProductLicensingModel.APP_BASED.name();
  }
}
