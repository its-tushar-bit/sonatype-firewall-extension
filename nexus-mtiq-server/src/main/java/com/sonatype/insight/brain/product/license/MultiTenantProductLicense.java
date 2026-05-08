/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
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

  @Inject
  public MultiTenantProductLicense(
      final DeveloperEnablementService developerEnablementService)
  {
    super(developerEnablementService);
  }

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
      Set<ProductLicensingModel> licensingModels,
      Integer maxApplications,
      Integer maxUsers,
      Integer maxFirewallUsers,
      Integer maxSboms)
  {
    productLicenseData.set(new ProductLicenseData(fingerprint, productLicenseKey.getExpirationDate().getTime(),
        productLicenseKey.getContactName(), productLicenseKey.getContactCompany(),
        productLicenseKey.getContactEmailAddress(), Collections.unmodifiableSet(products),
        Collections.unmodifiableSet(features), Collections.unmodifiableSet(stageTypes), licensingModels,
        maxApplications, maxUsers, maxFirewallUsers, maxSboms, null));
  }

  @Override
  ProductLicenseData getProductLicenseData() {
    return productLicenseData.get();
  }

  // Credit is a self-hosted-only feature. MTIQ never uses credit-based licensing, so these methods are no-ops.
  @Override
  public void setCreditAmount(BigDecimal creditAmount) {
    // no-op: credit is not applicable to multi-tenant deployments
  }

  @Override
  public BigDecimal getCreditAmount() {
    return null; // credit is not applicable to multi-tenant deployments
  }
}
