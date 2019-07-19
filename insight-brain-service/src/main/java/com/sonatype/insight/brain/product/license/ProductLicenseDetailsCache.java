/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.util.prefs.Preferences;

import javax.inject.Named;
import javax.inject.Singleton;

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

  private static final String KEY_LICENSE_DETAILS = "licenseDetails";

  private Preferences getPreferences() {
    return Preferences.userRoot().node("com/sonatype/clm");
  }

  String loadJson() {
    return getPreferences().get(KEY_LICENSE_DETAILS, null);
  }

  void saveJson(String json) {
    getPreferences().put(KEY_LICENSE_DETAILS, json);
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
