/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;

import com.google.common.io.Resources;

@Named
@Singleton
public class TestProductLicenseDetailsCache
    extends ProductLicenseDetailsCache
{
  private final String defaultJson;

  private String json;

  @Inject
  public TestProductLicenseDetailsCache(ProductLicenseDAO productLicenseDAO) {
    super(productLicenseDAO);
    try {
      defaultJson =
          Resources.toString(getClass().getResource("/TestProductLicenseDetailsCache/productLicenseDetails.json"),
              StandardCharsets.UTF_8);
      json = defaultJson;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  String loadJson() {
    return json;
  }

  @Override
  void saveJson(String json) {
    this.json = json;
  }

  public void resetToDefaults() {
    json = defaultJson;
  }
}
