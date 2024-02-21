/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.license.model.LicensedFeature;
import org.sonatype.licensing.product.ProductLicenseKey;

import ru.vyarus.dropwizard.guice.module.installer.order.Order;

@Named
@Singleton
@Priority(MultiTenantProductLicense.PRIORITY)
@Order(Integer.MAX_VALUE - MultiTenantProductLicense.PRIORITY)
public class MultiTenantProductLicense
    extends DefaultProductLicense
{
  public static final int PRIORITY = 1;

  private final TenantReference<ProductLicenseData> productLicenseData =
      new TenantReference<>(DefaultProductLicense::initialProductLicenseData);

  @Override
  public void clear() {
    productLicenseData.remove();
  }

  @Override
  public void set(
      ProductLicenseKey productLicenseKey,
      String fingerprint,
      Set<String> products,
      Set<LicensedFeature> features,
      Set<StageType> stageTypes,
      ProductLicensingModel licensingModel,
      Integer maxApplications,
      Integer maxUsers,
      Integer maxFirewallUsers,
      Integer maxSboms)
  {
    productLicenseData.set(new ProductLicenseData(fingerprint, productLicenseKey.getExpirationDate().getTime(),
        productLicenseKey.getContactName(), productLicenseKey.getContactCompany(),
        productLicenseKey.getContactEmailAddress(), Collections.unmodifiableSet(products),
        Collections.unmodifiableSet(features), Collections.unmodifiableSet(stageTypes), licensingModel, maxApplications,
        maxUsers, maxFirewallUsers, maxSboms));
  }

  @Override
  ProductLicenseData getProductLicenseData() {
    return productLicenseData.get();
  }
}
