/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.model.configuration.ProductLicense;

import org.junit.rules.ExternalResource;

/**
 * Insert a test license if needed.
 * <p>
 * Background: {@link TestProductLicenseManager} previously contained this logic to insert a product license into the
 * database. However, that is currently a static and needs a DAO yet the database is not yet provisioned that early. So
 * this logic is refactored out into this class as a rule to insert the default test license as a Junit rule.
 */
public class TestProductLicenseRule
    extends ExternalResource
{
  private final DataStoreProvider dataStoreProvider;

  public TestProductLicenseRule(final DataStoreProvider dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  protected void before() throws Throwable {
    insertLicenseIfNeeded();
  }

  public void insertLicenseIfNeeded() {
    ProductLicenseDAO productLicenseDAO = new ProductLicenseDAO(dataStoreProvider.getOperationalDataStore());
    if (productLicenseDAO.get() == null) {
      ProductLicense productLicense = new ProductLicense();
      productLicense.setLicenseKey(Base64.getEncoder().encodeToString("LICENSE_KEY".getBytes(StandardCharsets.UTF_8)));
      productLicenseDAO.insert(productLicense);
    }
  }
}
