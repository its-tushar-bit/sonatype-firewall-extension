/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.codeborne.selenide.ElementsCollection;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingDashboardPage;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingDashboardPage.DataInsightRow;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingDashboardPage.EnterpriseRow;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingDashboardPage.NavigationListItem;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingLandingPage;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingLandingPage.DashboardCard;
import com.sonatype.insight.brain.enterprise.reporting.DashboardMetadataDTO;
import com.sonatype.insight.brain.enterprise.reporting.DashboardGroupMetadataDTO;
import com.sonatype.insight.brain.enterprise.reporting.DashboardMetadataListDTO;
import com.sonatype.insight.brain.enterprise.reporting.DashboardsVersionDTO;
import com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingConfigDTO;
import com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH;

public class EnterpriseReportingDashboardPageTest
    extends AbstractFunctionalTest
{
  private final EnterpriseReportingDashboardPage page = new EnterpriseReportingDashboardPage();

  private final EnterpriseReportingLandingPage landingPage = new EnterpriseReportingLandingPage();

  private static final String NO_IQ_VERSION = "";

  private static final String URL_DASHBOARD_ID =
      createDashboardMetadata(3, NO_IQ_VERSION).dashboardId;

  @BeforeClass
  public static void before() {
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));
    loginAsAdmin();
  }

  @After
  public void after() {
    EnterpriseReportingService enterpriseReportingService =
        testCLMServer.getCLMServer().getInstance(EnterpriseReportingService.class);
    enterpriseReportingService.currentDashboardsVersionSupplier.reset();
  }

  @Test
  public void testFeatureDisabled_RendersError() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(1);
    //The current page's dashboardMetadata must be mocked in this and subsequent tests to prevent navigation to
    //the landing page
    DashboardMetadataDTO dashboardMetadataCurrentPage =
        createDashboardMetadata(3, NO_IQ_VERSION);
    DashboardMetadataListDTO dashboardList =
        new DashboardMetadataListDTO(version, List.of(dashboardMetadataCurrentPage), List.of());
    setupTests(dashboardList, version);
    setFeatures();
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));
    page.enterpriseReportingNotEnabledAlert().shouldBe(visible);
    page.enterpriseReportingNotEnabledAlert().shouldHave(text("Enterprise Reporting feature not supported"));
  }

  @Test
  public void testNavigationBar() {
    var noGroupId = "";
    DashboardsVersionDTO version = new DashboardsVersionDTO(1);
    DashboardMetadataDTO dashboardMetadataGrouped =
        createDashboardMetadataWithCategoryAndGroup("enterprise", 1, NO_IQ_VERSION, "group-id");
    DashboardMetadataDTO dashboardMetadataCurrentPage =
        createDashboardMetadataWithCategoryAndGroup("dataInsight", 3, NO_IQ_VERSION, noGroupId);
    DashboardMetadataDTO dashboardMetadataActive =
        createDashboardMetadataWithCategoryAndGroup("dataInsight", 4, NO_IQ_VERSION, noGroupId);
    DashboardGroupMetadataDTO dashboardGroupMetadata = createDashboardGroupMetadata("group-id");
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(
        version,
        List.of(
            dashboardMetadataGrouped,
            dashboardMetadataCurrentPage,
            dashboardMetadataActive
        ),
        List.of(dashboardGroupMetadata));
    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));

    page.navigationBar().shouldBe(visible);
    page.enterpriseReportingNotEnabledAlert().shouldBe(hidden);

    EnterpriseRow enterpriseRow = page.enterpriseRow();
    enterpriseRow.shouldBe(visible);
    enterpriseRow.enterpriseTitle().shouldBe(text("Enterprise Dashboards"));
    enterpriseRow.enterpriseLinks().shouldHave(size(1));

    DataInsightRow dataInsightRow = page.dataInsightRow();
    dataInsightRow.shouldBe(visible);
    dataInsightRow.dataInsightTitle().shouldBe(text("Data Insights"));
    dataInsightRow.dataInsightLinks().shouldHave(size(2));

    SelenideElement activeLink = page.navigationListItem(dashboardMetadataActive.dashboardId).activeLink();
    activeLink.shouldBe(visible);
    activeLink.shouldHave(text(dashboardMetadataActive.title));

    SelenideElement activeGroupLink = page.navigationListItem(dashboardGroupMetadata.groupId).activeLink();
    activeGroupLink.shouldBe(visible);
    activeGroupLink.shouldHave(text(dashboardGroupMetadata.title));

    //a link with a matching group-id will not render in the nav bar
    NavigationListItem hiddenLinkItem = page.navigationListItem(dashboardMetadataGrouped.dashboardId);
    hiddenLinkItem.shouldBe(hidden);

    //assert that the <li> representing the current page's dashboard is a <span> instead of an <a>
    NavigationListItem currentPageListItem = page.navigationListItem(dashboardMetadataCurrentPage.dashboardId);
    currentPageListItem.currentPageLink().shouldBe(visible);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testNavigationLinkState_Dashboard() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(2);
    DashboardMetadataDTO dashboardMetadataDisabled = createDashboardMetadata(1, "500");
    DashboardMetadataDTO dashboardMetadataActiveWithVersion = createDashboardMetadata(2, "188");
    DashboardMetadataDTO dashboardMetadataCurrentPage = createDashboardMetadata(3, "188");
    DashboardMetadataDTO dashboardMetadataActive = createDashboardMetadata(4, NO_IQ_VERSION);
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(
        version,
        List.of(
            dashboardMetadataDisabled,
            dashboardMetadataActiveWithVersion,
            dashboardMetadataCurrentPage,
            dashboardMetadataActive
        ),
        List.of());
    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));

    String fullVersion = testCLMServer.getCLMServer().getInstance(VersionService.class).getLogDisplayVersion();
    int userVersion = Integer.parseInt(fullVersion.substring(0, fullVersion.indexOf("-")).replace(".0", ""));

    for (int i = 0; i < dashboardList.dashboardMetadata.size(); i++) {
      DashboardMetadataDTO dashboard = dashboardList.dashboardMetadata.get(i);
      NavigationListItem listItem = page.navigationListItem(dashboard.dashboardId);

      //only test links, do not test current page's <span> element
      if (!StringUtils.equals(dashboard.dashboardId, URL_DASHBOARD_ID)) {
        SelenideElement textLink = listItem.activeLink();
        Boolean isDisabled =
            StringUtils.isNotBlank(dashboard.sinceIQVersion) 
            && userVersion < Integer.parseInt(dashboard.sinceIQVersion);
        assertLinkState(textLink, isDisabled, "", dashboard.dashboardId);
      }
    }
  }

  @Test
  public void testNavigationLinkState_DashboardGroup() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(3);
    DashboardMetadataDTO dashboardMetadataCurrentPage = createDashboardMetadata(3,"188");
    DashboardMetadataDTO dashboardMetadataEnabledGroupFirst =
        createDashboardMetadataWithGroup(1, "400", "group-id");
    DashboardMetadataDTO dashboardMetadataEnabledGroupSecond =
        createDashboardMetadataWithGroup(2, NO_IQ_VERSION, "group-id");
    DashboardMetadataDTO dashboardMetadataDisabledGroupFirst =
        createDashboardMetadataWithGroup(4, "400", "disabled-group-id");
    DashboardMetadataDTO dashboardMetadataDisabledGroupSecond =
        createDashboardMetadataWithGroup(5,"405", "disabled-group-id");
    DashboardGroupMetadataDTO dashboardGroupMetadataEnabled = createDashboardGroupMetadata("group-id");
    DashboardGroupMetadataDTO dashboardGroupMetadataDisabled = createDashboardGroupMetadata("disabled-group-id");
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(
        version,
        List.of(
            dashboardMetadataCurrentPage,
            dashboardMetadataEnabledGroupFirst,
            dashboardMetadataEnabledGroupSecond,
            dashboardMetadataDisabledGroupFirst,
            dashboardMetadataDisabledGroupSecond
        ),
        List.of(dashboardGroupMetadataEnabled, dashboardGroupMetadataDisabled));
    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));

    String fullVersion = testCLMServer.getCLMServer().getInstance(VersionService.class).getLogDisplayVersion();
    int userVersion = Integer.parseInt(fullVersion.substring(0, fullVersion.indexOf("-")).replace(".0", ""));
    
    for (int i = 0; i < dashboardList.dashboardGroupMetadata.size(); i++) {
      DashboardGroupMetadataDTO dashboardGroup = dashboardList.dashboardGroupMetadata.get(i);
      NavigationListItem listItem = page.navigationListItem(dashboardGroup.groupId);
      SelenideElement textLink = listItem.activeLink();

      //dashboardGroup links should only be disabled if the user's IQ version is less than the sinceIQVersion of
      // ALL dashboards in dashboardMetadata with matching 'groupId'
      boolean allDashboardVersionsGreater = dashboardList.dashboardMetadata.stream()
                .filter(d -> Objects.equals(d.groupId, dashboardGroup.groupId))
                .map(d -> d.sinceIQVersion)
                .allMatch(sinceIQVersion ->
                    StringUtils.isNotBlank(sinceIQVersion) && userVersion < Integer.parseInt(sinceIQVersion)
                );

      assertLinkState(textLink,
                      allDashboardVersionsGreater,
                      dashboardGroup.groupId,
                      dashboardMetadataEnabledGroupSecond.dashboardId
                      );
    }
  }

  @Test
  public void testNavigationLinkState_DashboardVersionMatch() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(2);
    DashboardMetadataDTO dashboardMetadataMatchingVersion = createDashboardMetadata(2, "190");
    DashboardMetadataDTO dashboardMetadataCurrentPage = createDashboardMetadata(3, "188");
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(
        version,
        List.of(
            dashboardMetadataMatchingVersion,
            dashboardMetadataCurrentPage
        ),
        List.of());
    setupTests(dashboardList, version);
    VersionService versionService = testCLMServer.getCLMServer().getInstance(VersionService.class);
    versionService.setVersion("1.190.0-SNAPSHOT");
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));

    DashboardMetadataDTO dashboard = dashboardList.dashboardMetadata.get(0);
    SelenideElement textLink = page.navigationListItem(dashboard.dashboardId).activeLink();
    textLink.shouldBe(enabled);
    textLink.shouldHave(attribute("href", EnterpriseReportingDashboardPage.url(dashboard.dashboardId)));
  }

  @Test
  public void testPageNavigation() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(4);
    DashboardMetadataDTO dashboardMetadataEnterprise =
        createDashboardMetadata(1, "184");
    DashboardMetadataDTO dashboardMetadataEnterpriseGrouped =
        createDashboardMetadataWithGroup(2, NO_IQ_VERSION, "group-id");
    DashboardMetadataDTO dashboardMetadataDataInsight =
        createDashboardMetadata(3, NO_IQ_VERSION);
    DashboardGroupMetadataDTO dashboardGroupMetadata = createDashboardGroupMetadata("group-id");
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(version,
        List.of(
            dashboardMetadataEnterprise,
            dashboardMetadataEnterpriseGrouped,
            dashboardMetadataDataInsight),
        List.of(dashboardGroupMetadata));

    setupTests(dashboardList, version);
    setFeatures(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));

    SelenideElement link = page.navigationListItem(dashboardMetadataEnterprise.dashboardId).activeLink();
    link.click();
    waitUntilUrl(EnterpriseReportingDashboardPage.url(dashboardMetadataEnterprise.dashboardId));

    SelenideElement groupLink = page.navigationListItem(dashboardGroupMetadata.groupId).activeLink();
    groupLink.click();
    // when navigating to a Dashboard "Group" Page, the url takes 2 parameters: the dashboardGroupMetadata
    // groupId and a dashboardMetadata dashboardId
    waitUntilUrl(EnterpriseReportingDashboardPage.groupUrl(dashboardGroupMetadata.groupId,
                                                           dashboardMetadataEnterpriseGrouped.dashboardId));
  }

  @Test
  public void testNavigationFromLandingPage() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(6);
    DashboardMetadataDTO dashboardMetadataFirstGrouped =
        createDashboardMetadataWithGroup(2, "400", "group-id");
    DashboardMetadataDTO dashboardMetadataSecondGrouped =
        createDashboardMetadataWithGroup(4, NO_IQ_VERSION, "group-id");
    DashboardGroupMetadataDTO dashboardGroupMetadata =
        createDashboardGroupMetadata("group-id");
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(version,
        List.of(dashboardMetadataFirstGrouped, dashboardMetadataSecondGrouped),
        List.of(dashboardGroupMetadata));

    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingLandingPage.url());

    DashboardCard dashboardCard = landingPage.dashboardAt(dashboardGroupMetadata.groupId);
    SelenideElement dropdownOpenButton = dashboardCard.dashboardOpenDropdownButton();
    dropdownOpenButton.click();

    SelenideElement dropdownItemButton = dashboardCard.dashboardDropdownItemButton();
    dropdownItemButton.click();

    waitUntilUrl(EnterpriseReportingDashboardPage.groupUrl(dashboardGroupMetadata.groupId,
                                                           dashboardMetadataSecondGrouped.dashboardId));
    ElementsCollection groupTabs = page.groupTabs();
    groupTabs.shouldHave(size(2));

    SelenideElement secondDashboardTab = groupTabs.get(1);
    secondDashboardTab.shouldHave(attribute("aria-selected", "true"));

    refreshOrOpen(EnterpriseReportingLandingPage.url());
    dashboardCard.dashboardGroupButton().click();

    waitUntilUrl(EnterpriseReportingDashboardPage.groupUrl(dashboardGroupMetadata.groupId,
                                                           dashboardMetadataFirstGrouped.dashboardId));
    SelenideElement firstDashboardTab = groupTabs.get(0);
    firstDashboardTab.shouldHave(attribute("aria-selected", "true"));
  }

  @Test
  public void testTabVisibility() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(5);
    DashboardMetadataDTO dashboardMetadataFirstGrouped =
        createDashboardMetadataWithGroup(1, "500", "group-id");
    DashboardMetadataDTO dashboardMetadataSecondGrouped =
        createDashboardMetadataWithGroup(2, NO_IQ_VERSION, "group-id");
    DashboardMetadataDTO dashboardMetadataIndividual =
        createDashboardMetadata(3, "188");
    DashboardGroupMetadataDTO dashboardGroupMetadata = 
        createDashboardGroupMetadata("group-id");
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(version,
        List.of(
            dashboardMetadataFirstGrouped,
            dashboardMetadataSecondGrouped,
            dashboardMetadataIndividual),
        List.of(dashboardGroupMetadata));

    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));

    page.groupTabs().shouldHave(size(0));

    refreshOrOpen(EnterpriseReportingDashboardPage.groupUrl(dashboardGroupMetadata.groupId,
                                                            dashboardMetadataSecondGrouped.dashboardId));
    SelenideElement pageTitle = page.pageTitle();
    pageTitle.shouldBe(visible);
    pageTitle.shouldHave(text(dashboardGroupMetadata.title));

    ElementsCollection groupTabs = page.groupTabs();
    groupTabs.shouldHave(size(2));
    //both dashboards mocked with same accessButtonText
    String tabText = dashboardMetadataFirstGrouped.accessButtonText.replaceFirst("view ", "");

    SelenideElement firstDashboardTab = groupTabs.get(0);
    firstDashboardTab.shouldHave(text(tabText));
    firstDashboardTab.shouldHave(attribute("aria-selected", "false"));
    SelenideElement secondDashboardTab = groupTabs.get(1);
    secondDashboardTab.shouldHave(text(tabText));
    secondDashboardTab.shouldHave(attribute("aria-selected", "true"));
  }

  @Test
  public void testFilterBtnVisibility() {
    DashboardsVersionDTO version = new DashboardsVersionDTO(6);
    DashboardMetadataDTO dashboardMetadataDataInsight =
        createDashboardMetadata(3, "188");
    DashboardMetadataDTO dashboardMetadataEnterprise =
        createDashboardMetadataWithCategoryAndGroup("enterprise", 1, NO_IQ_VERSION, "");
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(version,
        List.of(
            dashboardMetadataDataInsight,
            dashboardMetadataEnterprise),
        List.of());

    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));

    SelenideElement pageTitle = page.pageTitle();
    SelenideElement filtersBtn = page.openFiltersBtn();
    pageTitle.shouldHave(text(dashboardMetadataDataInsight.title));
    filtersBtn.shouldBe(hidden);

    refreshOrOpen(EnterpriseReportingDashboardPage.url(dashboardMetadataEnterprise.dashboardId));

    pageTitle.shouldHave(text(dashboardMetadataEnterprise.title));
    filtersBtn.shouldBe(visible);
  }

  @Test
  public void testCopyToClipboard__Success() {
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));
    //need to mock the ClipboardApi writeText() function which needs to resolve before success message renders
    executeJavaScript( "navigator.clipboard = { writeText: function() { return Promise.resolve(); } }" );
    page.copyToClipboard().shouldBe(visible);
    page.copySuccessMessage().shouldBe(hidden);

    page.copyToClipboard().click();
    Selenide.sleep(1000);
    page.copySuccessMessage().shouldBe(visible);
  }

  private void mockHDSResponses() {
    testCLMServer.getHdsServer()
        .respondWith(new EnterpriseReportingConfigDTO("sonatype.looker.com"))
        .atUri(ENTERPRISE_REPORTING_CONFIG_PATH);
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

  private void assertLinkState(SelenideElement link, boolean isDisabled, String groupId, String id) {
    if (isDisabled) {
      link.shouldHave(attribute("aria-disabled", "true"));
      link.shouldNotHave(attribute("href"));
    }
    else {
      link.shouldBe(enabled);
      if (StringUtils.isNotBlank(groupId)) {
        link.shouldHave(attribute("href", EnterpriseReportingDashboardPage.groupUrl(groupId, id)));
      }
      else {
        link.shouldHave(attribute("href", EnterpriseReportingDashboardPage.url(id)));
      }
    }
  }

  private static DashboardMetadataDTO createDashboardMetadata(int order, String sinceIqVersion) {
    return new DashboardMetadataDTO("dashboard-id-" + order, null, "title" + order, "dataInsight", "description",
        Arrays.asList("feature 1", "feature 2"), "view dashboard", "rolling-recap.svg", "faBrain", order,
        false, "dashboards/rolling_recap::rolling_recap", null, null, sinceIqVersion);
  }

  private static DashboardMetadataDTO createDashboardMetadataWithGroup(int order,
                                                                       String sinceIqVersion,
                                                                       String groupId) 
  {
    return new DashboardMetadataDTO("dashboard-id-" + order, groupId, "title" + order, "dataInsight", "description",
        Arrays.asList("feature 1", "feature 2"), "view dashboard", "rolling-recap.svg", "faBrain", order,
        false, "dashboards/rolling_recap::rolling_recap", null, null, sinceIqVersion);
  }

  private static DashboardMetadataDTO createDashboardMetadataWithCategoryAndGroup(String category,
                                                                                  int order,
                                                                                  String sinceIqVersion,
                                                                                  String groupId) 
  {
    return new DashboardMetadataDTO("dashboard-id-" + order, groupId, "title" + order, category, "description",
        Arrays.asList("feature 1", "feature 2"), "view dashboard", "rolling-recap.svg", "faBrain", order,
        false, "dashboards/rolling_recap::rolling_recap", null, null, sinceIqVersion);
  }

  private static DashboardGroupMetadataDTO createDashboardGroupMetadata(String groupId) {
    return new DashboardGroupMetadataDTO(groupId, "description",
        Arrays.asList("group feature 1", "group feature 2"), "faShield", false,
        null, null, "group title");
  }
}
