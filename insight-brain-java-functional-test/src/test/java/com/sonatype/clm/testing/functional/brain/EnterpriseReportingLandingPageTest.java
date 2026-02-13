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
import com.sonatype.insight.brain.enterprise.reporting.DashboardGroupMetadataDTO;
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
import org.junit.Ignore;
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
    setFeatures();
    refreshOrOpen(EnterpriseReportingLandingPage.url());
    pageHeaderShouldBeVisible();
    page.enterpriseReportingNotEnabledError().shouldBe(visible);
    page.enterpriseReports().shouldBe(hidden);
    page.insightsReports().shouldBe(hidden);
  }

  @Test
  public void testPageContents() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(1);
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(version, List.of(), List.of());
    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingLandingPage.url());

    page.enterpriseReportingNotEnabledError().shouldBe(hidden);
    pageHeaderShouldBeVisible();
    page.infoAlert().shouldBe(visible);
    page.helpLink().shouldBe(visible);
    page.helpLink().shouldHave(attribute("href", "https://links.sonatype.com/products/nxiq/doc/enterprise-reporting"));
    contactUsShouldBeVisible();
  }

  @Ignore // See CLM-38696
  @Test
  public void testFeatureEnabled_ReportContent() {
    var noGroupId = "";
    DashboardsVersionDTO version = new DashboardsVersionDTO(2);
    DashboardMetadataDTO dashboardVisibleMetadataDTO = mockDashboardMetadata(1, noGroupId, "enterprise");
    DashboardMetadataDTO dashboardHiddenMetadataDTO = mockDashboardMetadata(2, "group-id", "dataInsight");
    DashboardGroupMetadataDTO dashboardGroupMetadataDTO = mockDashboardGroupMetadata("group-id", false);
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(
        version,
        List.of(dashboardVisibleMetadataDTO, dashboardHiddenMetadataDTO),
        List.of(dashboardGroupMetadataDTO)
    );
    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingLandingPage.url());

    page.enterpriseReports().shouldBe(visible);
    page.insightsReports().shouldBe(visible);

    DashboardCard visibleDashboardCard = page.dashboardAt(dashboardVisibleMetadataDTO.dashboardId);
    DashboardCard hiddenDashboardCard = page.dashboardAt(dashboardHiddenMetadataDTO.dashboardId);
    DashboardCard dashboardGroupCard = page.dashboardAt(dashboardGroupMetadataDTO.groupId);

    //Dashboards that have a groupId matching one of the dashboardGroupMetadataDTO groupId's will not render.
    //This still needs to be mocked to allow the dashboardGroup to be rendered
    hiddenDashboardCard.shouldBe(hidden);
    testReportContent(visibleDashboardCard,
                      dashboardVisibleMetadataDTO.title,
                      dashboardVisibleMetadataDTO.description,
                      dashboardVisibleMetadataDTO.previewImageIcon,
                      dashboardVisibleMetadataDTO.features);
    testReportContent(dashboardGroupCard,
                      dashboardGroupMetadataDTO.title,
                      dashboardGroupMetadataDTO.description,
                      dashboardGroupMetadataDTO.previewImageIcon,
                      dashboardGroupMetadataDTO.features);
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testFeatureEnabled_Spotlight() {
    var noSpotlightText = "";
    var noGroupId = "";
    DashboardsVersionDTO version = new DashboardsVersionDTO(3);
    DashboardMetadataDTO spotlightTrueDashboardMetadataDTO =
        mockSpotlightDashboardMetadata(1, true, noSpotlightText, noGroupId);
    DashboardMetadataDTO spotlightTextProvidedDashboardMetadataDTO =
        mockSpotlightDashboardMetadata(2, false, "TEST", noGroupId);
    DashboardMetadataDTO nonSpotlightDashboardMetadataDTO =
        mockSpotlightDashboardMetadata(3, false, noSpotlightText, noGroupId);
    DashboardMetadataDTO groupedDashboardMetadataDTO =
        mockSpotlightDashboardMetadata(4, false, noSpotlightText, "group-id");
    DashboardGroupMetadataDTO spotlightColorProvidedGroupDashboardMetadataDTO =
        mockDashboardGroupMetadata("group-id", true);
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(
        version,
        List.of(
            spotlightTrueDashboardMetadataDTO,
            spotlightTextProvidedDashboardMetadataDTO,
            nonSpotlightDashboardMetadataDTO,
            groupedDashboardMetadataDTO
        ),
        List.of(spotlightColorProvidedGroupDashboardMetadataDTO)
    );

    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingLandingPage.url());

    dashboardList.dashboardMetadata.forEach(dashboard -> {
      DashboardCard card = page.dashboardAt(dashboard.dashboardId);
      testSpotlightFeature(card, 
                           dashboard.spotlight,
                           dashboard.spotlightText,
                           dashboard.spotlightColor,
                           dashboard.category);
    });
    dashboardList.dashboardGroupMetadata.forEach(dashboard -> {
      DashboardCard card = page.dashboardAt(dashboard.groupId);
      testSpotlightFeature(card,
                           dashboard.spotlight,
                           dashboard.spotlightText,
                           dashboard.spotlightColor,
                           null);
    });
  }

  @Test
  public void testFeatureEnabled_ButtonState() {
    var noGroupId = "";
    DashboardsVersionDTO version = new DashboardsVersionDTO(4);
    DashboardMetadataDTO enabledDashboardMetadataDTO = mockVersionedDashboardMetadata(1, "188", noGroupId);
    DashboardMetadataDTO disabledDashboardMetadataDTO = mockVersionedDashboardMetadata(2, "400", noGroupId);
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(
        version,
        List.of(enabledDashboardMetadataDTO, disabledDashboardMetadataDTO),
        List.of()
    );

    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingLandingPage.url());

    String fullVersion = testCLMServer.getCLMServer().getInstance(VersionService.class).getLogDisplayVersion();
    int userVersion = Integer.parseInt(fullVersion.substring(0, fullVersion.indexOf("-")).replace(".0", ""));

    testButtonState(enabledDashboardMetadataDTO, userVersion);
    testButtonState(disabledDashboardMetadataDTO, userVersion);
  }

  @Test 
  public void testFeatureEnabled_GroupButtonState() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(5);
    DashboardMetadataDTO firstGroupedDashboardMetadataDTO = mockVersionedDashboardMetadata(3, "188", "group-id");
    DashboardMetadataDTO secondGroupedDashboardMetadataDTO = mockVersionedDashboardMetadata(4, "405", "group-id");
    DashboardMetadataDTO firstDisabledGroupedDashboardMetadataDTO =
        mockVersionedDashboardMetadata(5, "400", "disabled-group-id");
    DashboardMetadataDTO secondDisabledGroupedDashboardMetadataDTO =
        mockVersionedDashboardMetadata(6, "405", "disabled-group-id");
    DashboardGroupMetadataDTO enabledDashboardGroupMetadataDTO = mockDashboardGroupMetadata("group-id", false);
    DashboardGroupMetadataDTO disabledDashboardGroupMetadataDTO =
        mockDashboardGroupMetadata("disabled-group-id", false);
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(
        version,
        List.of(
            firstGroupedDashboardMetadataDTO,
            secondGroupedDashboardMetadataDTO,
            firstDisabledGroupedDashboardMetadataDTO,
            secondDisabledGroupedDashboardMetadataDTO
        ),
        List.of(enabledDashboardGroupMetadataDTO, disabledDashboardGroupMetadataDTO)
      );

    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingLandingPage.url());

    String fullVersion = testCLMServer.getCLMServer().getInstance(VersionService.class).getLogDisplayVersion();
    int userVersion = Integer.parseInt(fullVersion.substring(0, fullVersion.indexOf("-")).replace(".0", ""));

    //to establish if button is disabled, need to check sinceIQVersion of related dashboards in dashboardMetadata
    List<String> enabledGroupedVersions = List.of(
        firstGroupedDashboardMetadataDTO.sinceIQVersion,
        secondGroupedDashboardMetadataDTO.sinceIQVersion
    );
    List<String> disabledGroupedVersions = List.of(
        firstDisabledGroupedDashboardMetadataDTO.sinceIQVersion,
        secondDisabledGroupedDashboardMetadataDTO.sinceIQVersion
    );
    testGroupButtonState(enabledDashboardGroupMetadataDTO,
                         userVersion,
                         enabledGroupedVersions,
                         firstGroupedDashboardMetadataDTO.accessButtonText);
    testGroupButtonState(disabledDashboardGroupMetadataDTO,
                         userVersion,
                         disabledGroupedVersions,
                         firstDisabledGroupedDashboardMetadataDTO.accessButtonText);
  }

  @Test
  public void testFeatureEnabled_DashboardVersionMatch() {
    var noGroupId = "";
    DashboardsVersionDTO version = new DashboardsVersionDTO(6);
    DashboardMetadataDTO enabledDashboardMetadataDTO = mockVersionedDashboardMetadata(1, "190", noGroupId);
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(
        version,
        List.of(enabledDashboardMetadataDTO),
        List.of()
    );

    setupTests(dashboardList, version);
    VersionService versionService = testCLMServer.getCLMServer().getInstance(VersionService.class);
    versionService.setVersion("1.190.0-SNAPSHOT");
    refreshOrOpen(EnterpriseReportingLandingPage.url());

    DashboardMetadataDTO metadata = dashboardList.dashboardMetadata.get(0);
    DashboardCard card = page.dashboardAt(metadata.dashboardId);
    SelenideElement button = card.dashboardButton();
    button.shouldBe(enabled);
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

  private void testReportContent(DashboardCard report,
                                 String title,
                                 String description,
                                 String iconName,
                                 List<String> featuresList)
  {
    report.dashboardTitle().shouldHave(text(title));
    report.dashboardDescription().shouldBe(text(description));
  
    SelenideElement icon = report.icon();
    icon.isDisplayed();
    String regex = "(?=[A-Z])";
    String iconClassName = String.join("-", iconName.split(regex)).toLowerCase();
    icon.shouldHave(cssClass(iconClassName));

    ElementsCollection features = report.featureText();
    assertThat(features.size()).isEqualTo(featuresList.size());
    for (int i = 0; i < features.size(); i++) {
      features.get(i).shouldHave(text(featuresList.get(i)));
    }
  }

  private void testSpotlightFeature(DashboardCard card,
                                    Boolean spotlight,
                                    String spotlightText,
                                    String spotlightColor,
                                    String category)
  {
    if (spotlight) {
      card.spotlight().shouldBe(visible);
      if (StringUtils.isNotBlank(spotlightColor)) {
        card.spotlight().shouldHave(cssClass("nx-small-tag--" + spotlightColor));
      }
      else {
        if (StringUtils.equals(category, "enterprise")) {
          card.spotlight().shouldHave(cssClass("nx-small-tag--teal"));
        }
        else {
          card.spotlight().shouldHave(cssClass("nx-small-tag--purple"));
        }
      }
    }

    // Spotlight will also render if spotlight is false but spotlightText has a value
    if (StringUtils.isNotBlank(spotlightText)) {
      card.spotlight().shouldBe(visible);
      card.spotlight().shouldHave(text(spotlightText));
    }
  }

  private void testButtonState(DashboardMetadataDTO metadata, int userVersion) {
    DashboardCard card = page.dashboardAt(metadata.dashboardId);
    SelenideElement button = card.dashboardButton();
    button.shouldHave(text(metadata.accessButtonText));

    if (StringUtils.isNotBlank(metadata.sinceIQVersion)) {
      int sinceIqVersion = Integer.parseInt(metadata.sinceIQVersion);
      SelenideElement dashboardButton = card.dashboardButton();
      if (userVersion < sinceIqVersion) {
        dashboardButton.shouldBe(disabled);
      }
      else {
        dashboardButton.shouldBe(enabled);
      }
    }
  }

  private void testGroupButtonState(DashboardGroupMetadataDTO metadata,
                                    int userVersion,
                                    List<String> groupedVersions,
                                    String buttonText)
  {
    DashboardCard card = page.dashboardAt(metadata.groupId);
    SelenideElement button = card.dashboardGroupButton();
    button.shouldHave(text(buttonText));

    // Buttons on dashboardGroup cards should only be disabled if the user's IQ version is less than
    // the sinceIQVersion of ALL dashboards in dashboardMetadata with matching 'groupId'
    boolean allDisabled = groupedVersions.stream()
          .filter(StringUtils::isNotBlank)
          .mapToInt(Integer::parseInt)
          .allMatch(version -> userVersion < version);

    if (allDisabled) {
      button.shouldBe(disabled);
    }
    else {
      button.shouldBe(enabled);
    }
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

  private void setupTests(DashboardMetadataListDTO dashboardList, DashboardsVersionDTO version) {
    setFeatures(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);
    mockHDSResponses();
    testCLMServer.getHdsServer()
        .respondWith(version)
        .atUri("/rest/enterpriseReporting/currentVersion");
    testCLMServer.getHdsServer()
        .respondWith(dashboardList)
        .atUri("/rest/enterpriseReporting/dashboards");
  }

  private static DashboardMetadataDTO mockDashboardMetadata(int order, String groupId, String category) {
    return new DashboardMetadataDTO("id" + order, groupId, "title", category, "description",
        Arrays.asList("feature 1", "feature 2"), "button text", "rolling-recap.svg", "faBrain", order, false,
        "dashboards/rolling_recap::rolling_recap", null, null);
  }

  private static DashboardMetadataDTO mockSpotlightDashboardMetadata(int order,
                                                                     Boolean spotlight,
                                                                     String spotlightText,
                                                                     String groupId)
  {
    return new DashboardMetadataDTO("id" + order, groupId, "title", "enterprise", "description",
        Arrays.asList("feature 1", "feature 2"), "button text", "rolling-recap.svg", "faBrain", order, spotlight,
        "dashboards/rolling_recap::rolling_recap", null, spotlightText);
  }

  private static DashboardMetadataDTO mockVersionedDashboardMetadata(int order, String version, String groupId) {
    return new DashboardMetadataDTO("id" + order, groupId, "title", "enterprise", "description",
        Arrays.asList("feature 1", "feature 2"), "button text", "rolling-recap.svg", "faBrain", order, false,
        "dashboards/rolling_recap::rolling_recap", null, null, version);
  }

  private static DashboardGroupMetadataDTO mockDashboardGroupMetadata(String groupId, Boolean spotlight) {
    return new DashboardGroupMetadataDTO(groupId, "description",
        Arrays.asList("group feature 1", "group feature 2"), "faShield", spotlight,
        "pink", null, "group title");
  }
}
