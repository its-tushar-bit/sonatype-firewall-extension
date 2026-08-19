/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.Arrays;
import java.util.TreeSet;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ProductLicenseDetailsCacheTest
    extends AbstractComponentH2Test
{
  @Inject
  private ProductLicenseDetailsCache productLicenseDetailsCache;

  @Test
  public void testGetProductLicenseDetails_Null() {
    productLicenseDetailsCache.saveJson(null);
    assertThat(productLicenseDetailsCache.getProductLicenseDetails()).isNull();
  }

  @Test
  public void testGetProductLicenseDetails_BadJson() {
    productLicenseDetailsCache.saveJson("{");
    assertThat(productLicenseDetailsCache.getProductLicenseDetails()).isNull();
  }

  @Test
  public void testGetSetProductLicenseDetails() {
    SignedProductLicenseDetailsDTO dto = new SignedProductLicenseDetailsDTO();
    dto.version = 13;
    dto.features = new TreeSet<>(Arrays.asList("feature1", "feature2"));
    dto.stageIds = new TreeSet<>(Arrays.asList("stageId1", "stageId2"));
    dto.maxApplications = 1234;
    dto.signature = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};
    dto.signatureKeyAlias = "testKeyAlias";

    productLicenseDetailsCache.setProductLicenseDetails(dto);
    dto = productLicenseDetailsCache.getProductLicenseDetails();

    assertThat(dto.version).isEqualTo(13);
    assertThat(dto.features).containsExactly("feature1", "feature2");
    assertThat(dto.stageIds).containsExactly("stageId1", "stageId2");
    assertThat(dto.maxApplications).isEqualTo(1234);
    assertThat(dto.signature).containsExactly(0, 1, 2, 3, 4, 5, 6, 7);
    assertThat(dto.signatureKeyAlias).isEqualTo("testKeyAlias");
  }
}
