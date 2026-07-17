/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.solution;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SolutionResolver
{
  private static final Logger log = LoggerFactory.getLogger(SolutionResolver.class);

  private static final List<String> DEVELOPER_PRODUCTS = ImmutableList.of(
      ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT,
      ProductLicenseDetails.PRODUCT_TEAMS_EDITION);

  private static final List<String> FIREWALL_PRODUCTS = ImmutableList.of(
      ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS,
      ProductLicenseDetails.PRODUCT_FIREWALL,
      ProductLicenseDetails.PRODUCT_FIREWALL_V2,
      ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY,
      ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD,
      ProductLicenseDetails.PRODUCT_REPOSITORY_FIREWALL_SAAS);

  @VisibleForTesting
  static final List<String> IGNORED_PRODUCTS = ImmutableList.of(
      ProductLicenseDetails.PRODUCT_SONATYPE_LIFT_PREMIUM,
      ProductLicenseDetails.PRODUCT_MALWARE_DEFENSE,
      ProductLicenseDetails.PRODUCT_MALWARE_DEFENSE_CLOUD,
      ProductLicenseDetails.PRODUCT_MALWARE_DEFENSE_SAAS);

  private static final List<String> LIFECYCLE_PRODUCTS = ImmutableList.of(
      ProductLicenseDetails.PRODUCT_ADVANCED_DEVELOPMENT_PACK,
      ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK,
      ProductLicenseDetails.PRODUCT_AUDITOR_SAAS,
      ProductLicenseDetails.PRODUCT_COMPONENT_ANALYSIS_SERVICE,
      ProductLicenseDetails.PRODUCT_FOUNDATION,
      ProductLicenseDetails.PRODUCT_INFRASTRUCTURE_AS_CODE_PACK,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_FOUNDATION_SAAS,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
      ProductLicenseDetails.PRODUCT_RISK,
      ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
      ProductLicenseDetails.PRODUCT_TEAMS_EDITION);

  private static final List<String> REPO_MANAGER_PRODUCTS = ImmutableList.of(
      ProductLicenseDetails.PRODUCT_NEXUS);

  private static final List<String> GUIDE_PRODUCTS = ImmutableList.of(
      ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED);

  // AI Developer ships as two mutually-exclusive SKUs: the self-hosted AiDeveloper (single-tenant)
  // and AiDeveloperSaas (multi-tenant). Each is only valid in its own deployment model.
  private static final List<String> AI_DEVELOPER_SINGLE_TENANT_PRODUCTS = ImmutableList.of(
      ProductLicenseDetails.PRODUCT_AI_DEVELOPER);

  private static final List<String> AI_DEVELOPER_MULTI_TENANT_PRODUCTS = ImmutableList.of(
      ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS);

  private static final List<String> SBOM_MANAGER_PRODUCTS = ImmutableList.of(
      ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
      ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);

  private final ProductLicense productLicense;

  private final TenantUtil tenantUtil;

  @Inject
  public SolutionResolver(ProductLicense productLicense, TenantUtil tenantUtil) {
    this.productLicense = productLicense;
    this.tenantUtil = tenantUtil;
  }

  /**
   * Get the set of solutions this instance of iq server is licensed for.
   *
   * @return Set containing Solution enum values that have been mapped over to product features or an empty set in
   *         case none of the licensed products map over to solutions
   */
  public Set<Solution> getLicensedSolutions() {
    log.trace("Setting licensed solutions for the Solution Switcher");
    Set<Solution> licensedSolutions = new HashSet<>();

    // Special case: If only SBOM Manager + ALP, don't include Lifecycle
    boolean hasSbomManager = hasAnyProduct(SBOM_MANAGER_PRODUCTS);
    boolean hasOnlyALP = productLicense.hasProduct(ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK)
        && !hasAnyOtherLifecycleProduct();

    if (hasAnyProduct(DEVELOPER_PRODUCTS)) {
      log.trace("Adding Developer product to licenced solutions");
      licensedSolutions.add(Solution.DEVELOPER);
    }

    if (hasAnyProduct(FIREWALL_PRODUCTS)) {
      licensedSolutions.add(Solution.FIREWALL);
    }

    if (hasAnyProduct(LIFECYCLE_PRODUCTS) && !(hasSbomManager && hasOnlyALP)) {
      licensedSolutions.add(Solution.LIFECYCLE);
    }

    if (hasAnyProduct(REPO_MANAGER_PRODUCTS)) {
      licensedSolutions.add(Solution.REPO_MANAGER);
    }

    if (hasAnyProduct(SBOM_MANAGER_PRODUCTS)) {
      licensedSolutions.add(Solution.SBOM_MANAGER);
    }

    if (hasAnyProduct(GUIDE_PRODUCTS) && productLicense.hasFeature(LicensedFeature.GUIDE)
        && !tenantUtil.isMultiTenant())
    {
      licensedSolutions.add(Solution.GUIDE);
    }

    if (productLicense.hasFeature(LicensedFeature.AI_DEVELOPER) && hasAiDeveloperProductForTenancy()) {
      licensedSolutions.add(Solution.AI_DEVELOPER);
    }

    return licensedSolutions;
  }

  /**
   * AI Developer is single-tenant for the self-hosted AiDeveloper SKU and multi-tenant for the
   * AiDeveloperSaas SKU. Each SKU is only honored in its matching deployment model, so a license
   * issued for the wrong tenancy never surfaces the solution.
   */
  private boolean hasAiDeveloperProductForTenancy() {
    return tenantUtil.isMultiTenant()
        ? hasAnyProduct(AI_DEVELOPER_MULTI_TENANT_PRODUCTS)
        : hasAnyProduct(AI_DEVELOPER_SINGLE_TENANT_PRODUCTS);
  }

  private boolean hasAnyProduct(List<String> products) {
    final boolean hasAnyProduct = products.stream().anyMatch(productLicense::hasProduct);
    log.trace("Products [{}] are included in the license products set [{}]? = {}", products,
        productLicense.getProducts(), hasAnyProduct);
    return hasAnyProduct;
  }

  private boolean hasAnyOtherLifecycleProduct() {
    return LIFECYCLE_PRODUCTS.stream()
        .filter(p -> !p.equals(ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK))
        .anyMatch(productLicense::hasProduct);
  }
}
