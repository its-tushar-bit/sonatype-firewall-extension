/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class TestProductLicenseDetailsCache
    extends ProductLicenseDetailsCache
{
  private String json;

  @Override
  String loadJson() {
    return json;
  }

  @Override
  void saveJson(String json) {
    this.json = json;
  }
}
