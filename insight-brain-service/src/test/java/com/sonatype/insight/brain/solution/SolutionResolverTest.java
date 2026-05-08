/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.solution;

import java.util.Set;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SolutionResolverTest
{
  @After
  public void cleanUpStaticState() {
    SystemConfigurationPropertyFeature.injectDependencies(null);
  }

  private void injectEnabledGuideUiDao() {
    SystemConfigurationPropertyDAO mockDao = Mockito.mock(SystemConfigurationPropertyDAO.class);
    TransactionContext mockTx = Mockito.mock(TransactionContext.class);
    when(mockDao.createTransactionContext()).thenReturn(mockTx);
    when(mockDao.getByName(mockTx, SystemConfigurationProperty.GUIDE_UI_ENABLED))
        .thenReturn(new SystemConfigurationProperty(SystemConfigurationProperty.GUIDE_UI_ENABLED, "true"));
    SystemConfigurationPropertyFeature.injectDependencies(mockDao);
  }

  @Test
  public void testAllProductsMapped() {
    // Inject mock DAO so GUIDE_UI.isEnabled() works for Guide products
    injectEnabledGuideUiDao();

    for (String product : ProductLicenseDetails.PRODUCTS) {
      // given: a license mapped to a specific product and a solution resolver that uses that license
      ProductLicense productLicense = Mockito.mock(ProductLicense.class);
      when(productLicense.hasProduct(product)).thenReturn(true);
      SolutionResolver solutionResolver = new SolutionResolver(productLicense);

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
    SolutionResolver solutionResolver = new SolutionResolver(productLicense);

    // when: try to resolve the associated solution
    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();

    // then:
    assertThat(licensedSolutions).isEmpty();
  }

  // CLM-36382: Special case fix to validate the case when only SBOM Manager and ALP are present in the license. This
  // is to prevent to display in the solution switcher the Lifecycle solution when it shouldn't be shown.
  // No DAO injection needed: Guide product returns false via the else branch, so GUIDE_UI.isEnabled() is never called.
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

    SolutionResolver solutionResolver = new SolutionResolver(productLicense);
    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).containsExactly(Solution.SBOM_MANAGER);
  }

  @Test
  public void testGuideIncluded_WhenGuideUiEnabledAndProductLicensed() {
    injectEnabledGuideUiDao();

    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)).thenReturn(true);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).contains(Solution.GUIDE);
  }

  @Test
  public void testGuideNotIncluded_WhenGuideUiDisabled() {
    SystemConfigurationPropertyDAO mockDao = Mockito.mock(SystemConfigurationPropertyDAO.class);
    TransactionContext mockTx = Mockito.mock(TransactionContext.class);
    when(mockDao.createTransactionContext()).thenReturn(mockTx);
    // GUIDE_UI has enabledWhenAbsent=false — returning null means disabled
    when(mockDao.getByName(mockTx, SystemConfigurationProperty.GUIDE_UI_ENABLED)).thenReturn(null);
    SystemConfigurationPropertyFeature.injectDependencies(mockDao);

    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)).thenReturn(true);
    SolutionResolver solutionResolver = new SolutionResolver(productLicense);

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    assertThat(licensedSolutions).doesNotContain(Solution.GUIDE);
  }
}
