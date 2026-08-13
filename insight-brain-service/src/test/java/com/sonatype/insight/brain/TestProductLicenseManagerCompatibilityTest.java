/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonatype.licensing.product.ProductLicenseManager;

public class TestProductLicenseManagerCompatibilityTest
{
  @Test
  public void shouldKeepLegacyMockProductLicenseManagerClassAvailable() throws Exception {
    Class<?> managerClass = Class.forName("com.sonatype.insight.brain.MockProductLicenseManager");

    assertThat(ProductLicenseManager.class).isAssignableFrom(managerClass);
    assertThat(managerClass.getDeclaredConstructor().newInstance()).isInstanceOf(ProductLicenseManager.class);
  }
}
