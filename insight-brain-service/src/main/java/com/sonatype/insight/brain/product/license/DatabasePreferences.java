/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.prefs.AbstractPreferences;

import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.model.configuration.ProductLicense;

public class DatabasePreferences
    extends AbstractPreferences
{
  // cf. de.schlichtherle.license.LicenseManager.PREFERENCES_KEY
  public static final String LICENSE_KEY = "license";

  public static final String LICENSE_DETAILS_KEY = "licenseDetails";

  private final ProductLicenseDAO productLicenseDAO;

  public DatabasePreferences(final ProductLicenseDAO productLicenseDAO) {
    super(null, "");
    this.productLicenseDAO = productLicenseDAO;
  }

  @Override
  public boolean isUserNode() {
    return true;
  }

  private void validateKey(String key) {
    if (!LICENSE_KEY.equals(key)) {
      throw new IllegalArgumentException("Invalid key name: " + key);
    }
  }

  @Override
  protected void putSpi(String key, String value) {
    validateKey(key);
    ProductLicense productLicense = new ProductLicense();
    productLicense.setLicenseKey(value);
    productLicenseDAO.update(productLicense);
  }

  @Override
  protected String getSpi(String key) {
    validateKey(key);
    ProductLicense productLicense = productLicenseDAO.get();
    if (productLicense == null) {
      return null;
    }
    return productLicense.getLicenseKey();
  }

  @Override
  protected void removeSpi(String key) {
    validateKey(key);
    productLicenseDAO.delete();
  }

  @Override
  protected void removeNodeSpi() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected String[] keysSpi() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected String[] childrenNamesSpi() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected AbstractPreferences childSpi(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void syncSpi() {
    // no-op, backing store is access directly
  }

  @Override
  protected void flushSpi() {
    // no-op, backing store is access directly
  }
}
