/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingLandingPage;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingLandingPage.ContactCard;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingLandingPage.DashboardCard;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.enterprise.reporting.DashboardMetadataDTO;
import com.sonatype.insight.brain.enterprise.reporting.DashboardMetadataListDTO;
import com.sonatype.insight.brain.enterprise.reporting.DashboardsVersionDTO;
import com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingConfigDTO;
import com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class EnterpriseReportingLandingPageTest
    extends AbstractFunctionalTest
{
  private final EnterpriseReportingLandingPage page = new EnterpriseReportingLandingPage();

  @BeforeClass
  public static void before() {
    refreshOrOpen(EnterpriseReportingLandingPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    EnterpriseReportingService enterpriseReportingService =
        testCLMServer.getCLMServer().getInstance(EnterpriseReportingService.class);
    enterpriseReportingService.currentDashboardsVersionSupplier.reset();
  }

  @Test
  public void testFeatureDisabled_ShowsError() {
    pageHeaderShouldBeVisible();
    page.enterpriseReportingNotEnabledError().shouldBe(visible);
    page.enterpriseReports().shouldBe(hidden);
    page.insightsReports().shouldBe(hidden);
  }

  @Test
  public void testFeatureEnabled_Success() {
    DashboardMetadataDTO spotlightDefautColorDashboardMetadataDTO = mockDashboardMetadataDTOSpotlightDefaultColor();
    DashboardMetadataDTO spotlightProvidedColorDashboardMetadataDTO = mockDashboardMetadataDTOSpotlightProvidedColor();
    DashboardMetadataDTO nonSpotlightDashboardMetadataDTO = mockDashboardMetadataDTO();
    DashboardMetadataDTO spotlightTextProvidedDashboardMetadataDTO = mockDashboardMetadataDTOSpotlightTextProvided();
    DashboardMetadataDTO spotlightTextProvidedDisabledSpotlightDashboardMetadataDTO =
        mockDashboardMetadataDTOSpotlightTextProvidedDisabledSpotlight();
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(List.of(
        spotlightDefautColorDashboardMetadataDTO,
        spotlightProvidedColorDashboardMetadataDTO,
        nonSpotlightDashboardMetadataDTO,
        spotlightTextProvidedDashboardMetadataDTO,
        spotlightTextProvidedDisabledSpotlightDashboardMetadataDTO
    ));
    int version = 2;
    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingLandingPage.url());

    page.enterpriseReportingNotEnabledError().shouldBe(hidden);
    pageHeaderShouldBeVisible();
    page.infoAlert().shouldBe(visible);
    page.enterpriseReports().shouldBe(visible);
    page.insightsReports().shouldBe(visible);
    assertReportContent(spotlightDefautColorDashboardMetadataDTO);
    assertReportContent(spotlightProvidedColorDashboardMetadataDTO);
    assertReportContent(nonSpotlightDashboardMetadataDTO);
    assertReportContent(spotlightTextProvidedDashboardMetadataDTO);
    assertReportContent(spotlightTextProvidedDisabledSpotlightDashboardMetadataDTO);
    contactUsShouldBeVisible();
    page.helpLink().shouldBe(visible);
    page.helpLink().shouldHave(attribute("href", "https://links.sonatype.com/products/nxiq/doc/enterprise-reporting"));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testAdvancedReportingIndicator() {
    setAdvancedReportingEnabled(false);
    refreshOrOpen(EnterpriseReportingLandingPage.url());
    page.statusIndicator().shouldNotHave(cssClass("nx-status-indicator--positive"));

    setAdvancedReportingEnabled(true);
    refreshOrOpen(EnterpriseReportingLandingPage.url());
    page.statusIndicator().shouldHave(cssClass("nx-status-indicator--positive"));
  }

  @Test
  public void testCopyToClipboard__Success() {
    refreshOrOpen(EnterpriseReportingLandingPage.url());
    //need to mock the ClipboardApi writeText() function which needs to resolve before success message renders
    executeJavaScript( "navigator.clipboard = { writeText: function() { return Promise.resolve(); } }" );
    page.copyToClipboard().shouldBe(visible);
    page.copySuccessMessage().shouldBe(hidden);

    page.copyToClipboard().click();
    Selenide.sleep(1000);
    page.copySuccessMessage().shouldBe(visible);
  }

  private void pageHeaderShouldBeVisible() {
    page.heading().shouldBe(visible);
    page.heading().shouldHave(text("Enterprise Reporting"));
    page.description().shouldBe(visible);
    assertThat(page.description().innerText()).isNotEmpty();
  }

  private void assertReportContent(DashboardMetadataDTO dashboardMetadataDTO) {
    DashboardCard report = page.dashboardAt(dashboardMetadataDTO.dashboardId);
    SelenideElement icon = report.icon();
    ElementsCollection features = report.featureText();
    report.dashboardTitle().shouldHave(text(dashboardMetadataDTO.title));
    icon.isDisplayed();
    checkIconClassName(dashboardMetadataDTO.previewImageIcon, icon);
    report.dashboardDescription().shouldBe(text(dashboardMetadataDTO.description));
    report.dashboardButton().shouldHave(text(dashboardMetadataDTO.accessButtonText));
    assertThat(features.size()).isEqualTo(dashboardMetadataDTO.features.size());
    for (int i = 0; i < features.size(); i++) {
      features.get(i).shouldHave(text(dashboardMetadataDTO.features.get(i)));
    }
    if (dashboardMetadataDTO.spotlight) {
      report.spotlight().shouldBe(visible);
      if (StringUtils.isNotBlank(dashboardMetadataDTO.spotlightColor)) {
        report.spotlight().shouldHave(cssClass("nx-small-tag--" + dashboardMetadataDTO.spotlightColor));
      }
      else {
        if (StringUtils.equals(dashboardMetadataDTO.category, "enterprise")) {
          report.spotlight().shouldHave(cssClass("nx-small-tag--teal"));
        }
        else {
          report.spotlight().shouldHave(cssClass("nx-small-tag--purple"));
        }
      }
    }
    if (StringUtils.isNotBlank(dashboardMetadataDTO.spotlightText)) {
      report.spotlight().shouldBe(visible);
      report.spotlight().shouldHave(text(dashboardMetadataDTO.spotlightText));
    }

    if (StringUtils.isNotBlank(dashboardMetadataDTO.sinceIQVersion)) {
      String fullVersion = testCLMServer.getCLMServer().getInstance(VersionService.class).getLogDisplayVersion();
      int userVersion = Integer.parseInt(fullVersion.substring(0, fullVersion.indexOf("-")).replace(".0", ""));
      int sinceIqVersion = Integer.parseInt(dashboardMetadataDTO.sinceIQVersion);
      SelenideElement dashboardButton = report.dashboardButton();
      if (userVersion < sinceIqVersion) {
        dashboardButton.shouldBe(disabled);
      }
      else {
        dashboardButton.shouldBe(enabled);
      }
    }
  }

  private void checkIconClassName(String iconName, SelenideElement icon) {
    String regex = "(?=[A-Z])";
    String iconClassName = String.join("-", iconName.split(regex)).toLowerCase();
    icon.shouldHave(cssClass(iconClassName));
  }

  private void contactUsShouldBeVisible() {
    page.contactUsHeading().shouldHave(text("Contact Us"));

    ContactCard firstCard = page.contactCard(1);
    firstCard.contactTitle().shouldHave(text("Schedule a Discussion"));
    assertThat(firstCard.contactDescription().innerText()).isNotEmpty();
    firstCard.contactButton().shouldHave(attribute("href", "mailto:data-insights-pm@sonatype.com"));
    firstCard.contactButton().shouldHave(text("Email Us"));

    ContactCard secondCard = page.contactCard(2);
    secondCard.contactTitle().shouldHave(text("Suggest an Improvement"));
    assertThat(secondCard.contactDescription().innerText()).isNotEmpty();
    secondCard.contactButton()
        .shouldHave(attribute("href", "http://links.sonatype.com/products/nxiq/feedback/data-insights-ideas"));
    secondCard.contactButton().shouldHave(text("Explore the Ideas Portal"));

    ContactCard thirdCard = page.contactCard(3);
    thirdCard.contactTitle().shouldHave(text("Receive Technical Support"));
    assertThat(thirdCard.contactDescription().innerText()).isNotEmpty();
    thirdCard.contactButton()
        .shouldHave(attribute("href", "http://links.sonatype.com/products/nexus/pro/support"));
    thirdCard.contactButton().shouldHave(text("Explore Support"));
  }

  private void mockHDSResponses() {
    testCLMServer.getHdsServer()
        .respondWith(new EnterpriseReportingConfigDTO("sonatype.looker.com"))
        .atUri(ENTERPRISE_REPORTING_CONFIG_PATH);
  }

  private void setAdvancedReportingEnabled(Boolean enabled) {
    ApiConfigurationService configurationService =
        testCLMServer.getCLMServer().getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationNoAuthz(
        SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED,
        enabled);
  }

  private void setupTests(DashboardMetadataListDTO dashboardList, int version) {
    setFeatures(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);
    mockHDSResponses();
    testCLMServer.getHdsServer()
        .respondWith(new DashboardsVersionDTO(version))
        .atUri("/rest/enterpriseReporting/currentVersion");
    testCLMServer.getHdsServer()
        .respondWith(dashboardList)
        .atUri("/rest/enterpriseReporting/dashboards");
  }

  private static DashboardMetadataDTO mockDashboardMetadataDTOSpotlightDefaultColor() {
    return new DashboardMetadataDTO("id", "title", "enterprise", "description", Arrays.asList("feature 1", "feature 2"),
        "button text", "rolling-recap.svg", "faBrain", 1, true, "dashboards/rolling_recap::rolling_recap", null, null,
        "185");
  }

  private static DashboardMetadataDTO mockDashboardMetadataDTOSpotlightProvidedColor() {
    return new DashboardMetadataDTO("id2", "title 2", "enterprise", "description",
        Arrays.asList("feature 3", "feature 4"), "button text", "rolling-recap.svg", "faBrain", 2, true,
        "dashboards/rolling_recap::rolling_recap", "pink", null, "400");
  }

  private static DashboardMetadataDTO mockDashboardMetadataDTO() {
    return new DashboardMetadataDTO("id3", "title 3", "dataInsight", "description 2",
        Arrays.asList("feature 5", "feature 6"), "button text 2", "rolling-recap.svg", "faBrain", 3, false,
        "dashboards/rolling_recap::rolling_recap", null, null);
  }

  private static DashboardMetadataDTO mockDashboardMetadataDTOSpotlightTextProvided() {
    return new DashboardMetadataDTO("id4", "title 4", "dataInsight", "description",
        Arrays.asList("feature 7", "feature 8"), "button text", "rolling-recap.svg", "faThumbsUp", 4, true,
        "dashboards/rolling_recap::rolling_recap", null, "TEST");
  }

  private static DashboardMetadataDTO mockDashboardMetadataDTOSpotlightTextProvidedDisabledSpotlight() {
    return new DashboardMetadataDTO("id5", "title 5", "dataInsight", "description",
        Arrays.asList("feature 9", "feature 10"), "button text", "rolling-recap.svg", "faThumbsUp", 4, false,
        "dashboards/rolling_recap::rolling_recap", null, "TEST");
  }
}
