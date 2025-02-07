/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.ApiPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;

public class ApiPageTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() {
    hardreset();
  }

  @Before
  public void before() {
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(true);
    refreshOrOpen(ApiPage.url());
  }

  @Test
  public void testInitialTab() {
    ApiPage apiPage = new ApiPage();
    apiPage.publicTab().shouldHave(cssClass("active"));
    apiPage.swaggerUi().should(exist).shouldHave(text("/api/v2"));
  }

  @Test
  public void testToExperimentalTab() {
    ApiPage apiPage = new ApiPage();
    apiPage.publicTab().shouldHave(cssClass("active"));
    apiPage.experimentalTab().click();
    apiPage.experimentalTab().shouldHave(cssClass("active"));
    apiPage.swaggerUi().should(exist).shouldHave(text("/api/experimental"));
    eyesWatcher.eyesCheck("API page Experimental tab");
  }

  @Test
  public void testToPublicTab() {
    ApiPage apiPage = new ApiPage();
    apiPage.experimentalTab().click();
    apiPage.experimentalTab().shouldHave(cssClass("active"));
    apiPage.publicTab().click();
    apiPage.publicTab().shouldHave(cssClass("active"));
    apiPage.swaggerUi().should(exist).shouldHave(text("/api/v2"));
    eyesWatcher.eyesCheck("API page public tab");
  }

  @Test
  public void testSwaggerGetApplications() {
    Application application = tempEntity.newApplicationWithParent();
    ApiPage apiPage = new ApiPage();
    apiPage.should(exist).swaggerUi().should(exist).shouldHave(text("/api/v2"));

    MainHeader.loginButton().should(exist).click();
    loginAsAdmin();
    SelenideElement getApplicationsDiv = apiPage.swaggerUi().find("#operations-Applications-getApplications");
    getApplicationsDiv.should(exist).click();
    SelenideElement tryItOutButton = getApplicationsDiv.find(".try-out__btn");
    tryItOutButton.should(exist).click();
    SelenideElement executeButton = getApplicationsDiv.find(".execute");
    executeButton.should(exist).click();
    SelenideElement responseDiv = getApplicationsDiv.find(".response-col_description .microlight");
    responseDiv.should(exist).shouldHave(text(application.getId()));
  }
}
