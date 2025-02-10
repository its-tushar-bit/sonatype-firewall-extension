/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.RoiConfigurationPage;

import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;

public class RoiConfigurationPageTest extends AbstractFunctionalTest
{
  @BeforeClass
  public static void before() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Test
  public void testRendersPageSuccessfully() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
        ProductLicenseDetails.PRODUCT_REPOSITORY_FIREWALL_SAAS
    );
    refreshOrOpen(RoiConfigurationPage.url());
    RoiConfigurationPage roiConfigurationPage = new RoiConfigurationPage();
    roiConfigurationPage.title().shouldHave(text("Return on Investment"));
  }

  @Test
  public void testRendersPermissionError() {
    User user = tempEntity.newUser("john.doe", "John", "Doe", "john@doe.com");
    refreshOrOpen(RoiConfigurationPage.url());
    logout();
    login(user.getUsername(), user.getPassword());
    refreshOrOpen(RoiConfigurationPage.url());
    RoiConfigurationPage roiConfigurationPage = new RoiConfigurationPage();
    roiConfigurationPage.loadError().shouldHave(
            text("An error occurred loading data. It appears you do not have permission to access this page. " +
                    "If you believe this to be incorrect please contact your administrator."));

    logout();
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Test
  public void testRendersLicenseError() {
    uninstallLicense();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    refresh();

    refreshOrOpen(RoiConfigurationPage.url());
    RoiConfigurationPage roiConfigurationPage = new RoiConfigurationPage();
    roiConfigurationPage.loadError().shouldHave(
            text("An error occurred loading data. Must have Lifecycle or " +
                    "Repository Firewall license to configure ROI metrics.")
    );
  }
}
