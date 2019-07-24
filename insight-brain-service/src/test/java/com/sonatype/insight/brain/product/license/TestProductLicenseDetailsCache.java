/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.io.Resources;

@Named
@Singleton
public class TestProductLicenseDetailsCache
    extends ProductLicenseDetailsCache
{
  private String json;

  public TestProductLicenseDetailsCache() {
    try {
      json = Resources.toString(getClass().getResource("/TestProductLicenseDetailsCache/productLicenseDetails.json"),
          StandardCharsets.UTF_8);
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
}
