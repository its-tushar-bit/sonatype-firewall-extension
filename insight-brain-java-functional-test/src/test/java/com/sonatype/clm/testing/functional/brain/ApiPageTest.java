/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.ApiPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

@RunWith(Parameterized.class)
public class ApiPageTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() {
    hardreset();
  }

  @Parameters
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {ProductLicenseDetails.PRODUCT_SONATYPE_DEVELOPMENT, ApiPage.developerUrl()},
        {ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, ApiPage.lifecycleUrl()},
        {ProductLicenseDetails.PRODUCT_FIREWALL_V2, ApiPage.firewallUrl()},
        {ProductLicenseDetails.PRODUCT_SBOM_MANAGER, ApiPage.sbomManagerUrl()}
    });
  }

  private final String licensedProduct;

  private final String url;

  public ApiPageTest(final String licensedProduct, final String url) {
    this.licensedProduct = licensedProduct;
    this.url = url;
  }

  @Before
  public void before() {
    setLicensedProducts(licensedProduct);
    hardreset();
    refreshOrOpen(url);
  }

  @Test
  public void testInitialTab() {
    ApiPage apiPage = new ApiPage();
    apiPage.publicTab().shouldHave(cssClass("active"));
    apiPage.swaggerUi().shouldBe(visible).shouldHave(text("/api/v2"));
  }

  @Test
  public void testToExperimentalTab() {
    ApiPage apiPage = new ApiPage();
    apiPage.publicTab().shouldHave(cssClass("active"));
    apiPage.experimentalTab().click();
    apiPage.experimentalTab().shouldHave(cssClass("active"));
    apiPage.swaggerUi().shouldBe(visible).shouldHave(text("/api/experimental"));
  }

  @Test
  public void testToPublicTab() {
    ApiPage apiPage = new ApiPage();
    apiPage.experimentalTab().click();
    apiPage.experimentalTab().shouldHave(cssClass("active"));
    apiPage.publicTab().click();
    apiPage.publicTab().shouldHave(cssClass("active"));
    apiPage.swaggerUi().shouldBe(visible).shouldHave(text("/api/v2"));
  }

  @Test
  public void testSwaggerGetApplications() {
    Application application = tempEntity.newApplicationWithParent();
    ApiPage apiPage = new ApiPage();
    apiPage.shouldBe(visible).swaggerUi().shouldBe(visible).shouldHave(text("/api/v2"));

    MainHeader.loginButton().shouldBe(visible).click();
    loginAsAdmin();
    SelenideElement getApplicationsDiv = apiPage.swaggerUi().find("#operations-Applications-getApplications");
    getApplicationsDiv.shouldBe(visible).click();
    SelenideElement tryItOutButton = getApplicationsDiv.find(".try-out__btn");
    tryItOutButton.shouldBe(visible).click();
    SelenideElement executeButton = getApplicationsDiv.find(".execute");
    executeButton.shouldBe(visible).click();
    SelenideElement responseDiv = getApplicationsDiv.find(".response-col_description .microlight");
    responseDiv.shouldBe(visible).shouldHave(text(application.getId()));
  }
}
