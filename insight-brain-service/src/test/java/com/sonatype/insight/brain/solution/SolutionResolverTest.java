/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.solution;

import java.util.Set;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SolutionResolverTest
{
  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testAllProductsMapped() {
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
}
