/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.solution;

import java.util.Set;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SolutionResolverTest
{
  private final TenantUtil singleTenantUtil = createTenantUtil(false);

  private final TenantUtil multiTenantUtil = createTenantUtil(true);

  private static TenantUtil createTenantUtil(boolean multiTenant) {
    TenantUtil tenantUtil = Mockito.mock(TenantUtil.class);
    when(tenantUtil.isMultiTenant()).thenReturn(multiTenant);
    return tenantUtil;
  }

  @Test
  public void testAllProductsMapped() {
    for (String product : ProductLicenseDetails.PRODUCTS) {
      // given: a license mapped to a specific product and a solution resolver that uses that license
      ProductLicense productLicense = Mockito.mock(ProductLicense.class);
      when(productLicense.hasProduct(product)).thenReturn(true);
      // Guide requires both product AND feature
      if (product.equals(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)) {
        when(productLicense.hasFeature(LicensedFeature.GUIDE)).thenReturn(true);
      }
      // AI Developer requires both product AND feature, and only resolves in its SKU's tenancy:
      // AiDeveloper is single-tenant, AiDeveloperSaas is multi-tenant.
      TenantUtil tenantUtil = singleTenantUtil;
      if (product.equals(ProductLicenseDetails.PRODUCT_AI_DEVELOPER)) {
        when(productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)).thenReturn(true);
      }
      if (product.equals(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)) {
        when(productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)).thenReturn(true);
        tenantUtil = multiTenantUtil;
      }
      SolutionResolver solutionResolver = new SolutionResolver(productLicense, tenantUtil);

      // when: try to resolve the associated solution
      Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();

      // then:
      if (SolutionResolver.IGNORED_PRODUCTS.contains(product)) {
        assertThat(licensedSolutions).isEmpty();
      }
      else {
        assertThat(licensedSolutions)
            .withFailMessage("Product '%s' is not accounted for", product)
            .isNotEmpty();
      }
    }
  }

  @Test
  public void testNoLicensedProducts() {
    // given: a license mapped to no products and a solution resolver that uses that license
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(any())).thenReturn(false);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, singleTenantUtil);

    // when: try to resolve the associated solution
    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();

    // then:
    assertThat(licensedSolutions).isEmpty();
  }

  // CLM-36382: Special case fix to validate the case when only SBOM Manager and ALP are present in the license. This
  // is to prevent to display in the solution switcher the Lifecycle solution when it shouldn't be shown.
  @Test
  public void testOnlySbomLicensed_WhenOnlySbomProductAndALP_arePresentInLicense() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    for (String product : ProductLicenseDetails.PRODUCTS) {
      if (product.equals(ProductLicenseDetails.PRODUCT_SBOM_MANAGER) ||
          product.equals(ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK) ||
          product.equals(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS))
      {
        when(productLicense.hasProduct(product)).thenReturn(true);
      }
      else {
        when(productLicense.hasProduct(product)).thenReturn(false);
      }
    }

    SolutionResolver solutionResolver = new SolutionResolver(productLicense, singleTenantUtil);
    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).containsExactly(Solution.SBOM_MANAGER);
  }

  @Test
  public void testGuideIncluded_WhenGuideProductLicensed() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.GUIDE)).thenReturn(true);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, singleTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).contains(Solution.GUIDE);
  }

  @Test
  public void testGuideNotIncluded_WhenGuideProductNotLicensed() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)).thenReturn(false);
    when(productLicense.hasFeature(LicensedFeature.GUIDE)).thenReturn(true);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, singleTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).doesNotContain(Solution.GUIDE);
  }

  @Test
  public void testGuideNotIncluded_WhenGuideFeatureNotLicensed() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.GUIDE)).thenReturn(false);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, singleTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).doesNotContain(Solution.GUIDE);
  }

  @Test
  public void testGuideNotIncluded_WhenMultiTenant() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.GUIDE)).thenReturn(true);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, multiTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).doesNotContain(Solution.GUIDE);
  }

  @Test
  public void testAiDeveloperIncluded_WhenSelfHostedProductAndFeatureLicensed_SingleTenant() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER)).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)).thenReturn(true);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, singleTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).contains(Solution.AI_DEVELOPER);
  }

  @Test
  public void testAiDeveloperNotIncluded_WhenFeatureNotLicensed() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER)).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)).thenReturn(false);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, singleTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).doesNotContain(Solution.AI_DEVELOPER);
  }

  @Test
  public void testAiDeveloperNotIncluded_WhenSelfHostedProductButMultiTenant() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER)).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)).thenReturn(true);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, multiTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).doesNotContain(Solution.AI_DEVELOPER);
  }

  @Test
  public void testAiDeveloperSaasIncluded_WhenProductAndFeatureLicensed_MultiTenant() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)).thenReturn(true);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, multiTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).contains(Solution.AI_DEVELOPER);
  }

  @Test
  public void testAiDeveloperSaasNotIncluded_WhenSingleTenant() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)).thenReturn(true);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, singleTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).doesNotContain(Solution.AI_DEVELOPER);
  }

  @Test
  public void testAiDeveloperSaasNotIncluded_WhenFeatureNotLicensed() {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)).thenReturn(true);
    when(productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)).thenReturn(false);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense, multiTenantUtil);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).doesNotContain(Solution.AI_DEVELOPER);
  }
}
