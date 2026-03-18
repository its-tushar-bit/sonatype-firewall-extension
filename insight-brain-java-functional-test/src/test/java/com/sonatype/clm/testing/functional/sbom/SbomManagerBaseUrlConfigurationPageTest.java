/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import com.sonatype.clm.testing.functional.brain.configuration.BaseUrlConfigurationPageTest;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerBaseUrlConfigurationPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.junit.Before;
import org.junit.Test;

public class SbomManagerBaseUrlConfigurationPageTest
    extends BaseUrlConfigurationPageTest
{
  @Before
  @Override
  public void before() {
    dao = lookup(SystemConfigurationPropertyDAO.class);
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);

    baseUrlConfigurationPage = new SbomManagerBaseUrlConfigurationPage();
    refreshOrOpen(baseUrlConfigurationPage.getUrl());
    loginAsAdmin();
    baseUrl = dao.get(SystemConfigurationProperty.BASE_URL);
  }

  @Test
  @Override
  public void testDefaultState() {
    eyesWatcher.eyesCheck("sbom manager base url configuration editor on top");
    assertDefaultState();
  }
}
