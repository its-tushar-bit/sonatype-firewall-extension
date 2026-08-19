/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductModeTest
{
  @Test
  public void testToString() {
    assertThat(ProductMode.SBOM_MANAGER).hasToString("sbomManager");
  }

  @Test
  public void testFromString() {
    assertThat(ProductMode.fromString("sbomManager")).isEqualTo(ProductMode.SBOM_MANAGER);
  }
}
