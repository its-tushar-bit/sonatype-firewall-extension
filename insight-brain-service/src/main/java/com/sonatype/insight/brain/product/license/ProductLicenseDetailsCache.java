/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches the product license details queried from the HDS to decouple server restarts from HDS uptime.
 */
@Named
@Singleton
public class ProductLicenseDetailsCache
{
  private static final Logger log = LoggerFactory.getLogger(ProductLicenseDetailsCache.class);

  private final ProductLicenseDAO productLicenseDAO;

  @Inject
  public ProductLicenseDetailsCache(ProductLicenseDAO productLicenseDAO) {
    this.productLicenseDAO = productLicenseDAO;
  }

  String loadJson() {
    ProductLicense productLicense = productLicenseDAO.get();
    if (productLicense == null) {
      return null;
    }
    return productLicense.getLicenseDetails();
  }

  void saveJson(String json) {
    ProductLicense productLicense = productLicenseDAO.get();
    productLicense.setLicenseDetails(json);
    productLicenseDAO.update(productLicense);
  }

  public SignedProductLicenseDetailsDTO getProductLicenseDetails() {
    log.debug("Loading product license details");
    try {
      String json = loadJson();
      return json == null ? null : JsonUtils.parse(json, SignedProductLicenseDetailsDTO.class);
    }
    catch (IOException e) {
      log.warn("Could not read cached product license details, forcing reload", e);
      return null;
    }
  }

  public void setProductLicenseDetails(SignedProductLicenseDetailsDTO licenseDetails) {
    log.debug("Saving product license details");
    saveJson(JsonUtils.writeUnformatted(licenseDetails));
  }
}
