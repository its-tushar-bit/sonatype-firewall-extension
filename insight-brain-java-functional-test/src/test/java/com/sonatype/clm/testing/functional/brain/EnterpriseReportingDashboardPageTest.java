/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingDashboardPage;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingDashboardPage.DataInsightRow;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingDashboardPage.EnterpriseRow;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingDashboardPage.NavigationListItem;
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

  private static final String URL_DASHBOARD_ID = createDashboardMetadata("dataInsight", 3, "", "").dashboardId;

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
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));
    page.enterpriseReportingNotEnabledAlert().shouldBe(visible);
    page.enterpriseReportingNotEnabledAlert().shouldHave(text("Enterprise Reporting feature not supported"));
  }

  @Test
  public void testNavigationBarLinks() {
    final var noGroupId = "";
    final var noIqVersion = "";
    DashboardsVersionDTO version = new DashboardsVersionDTO(1);
    DashboardMetadataDTO dashboardMetadataEnterpriseNoVersion =
        createDashboardMetadata("enterprise", 1, noIqVersion, noGroupId);
    DashboardMetadataDTO dashboardMetadataEnterpriseVersion =
        createDashboardMetadata("enterprise", 2, "500", noGroupId);
    DashboardMetadataDTO dashboardMetadataDataInsight =
        createDashboardMetadata("dataInsight", 3, noIqVersion, noGroupId);
    DashboardMetadataDTO dashboardMetadataDataInsightNoVersion =
        createDashboardMetadata("dataInsight", 4, noIqVersion, noGroupId);
    DashboardMetadataDTO dashboardMetadataDataInsightVersion =
        createDashboardMetadata("dataInsight", 5, "500", noGroupId);
    DashboardGroupMetadataDTO dashboardGroupMetadata = createDashboardGroupMetadata();
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(version,
        List.of(
            dashboardMetadataEnterpriseNoVersion,
            dashboardMetadataEnterpriseVersion,
            dashboardMetadataDataInsight,
            dashboardMetadataDataInsightNoVersion,
            dashboardMetadataDataInsightVersion
        ),
        List.of(dashboardGroupMetadata));

    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));
    page.navigationBar().shouldBe(visible);
    page.enterpriseReportingNotEnabledAlert().shouldBe(hidden);

    EnterpriseRow enterpriseRow = page.enterpriseRow();
    enterpriseRow.shouldBe(visible);
    enterpriseRow.enterpriseTitle().shouldBe(text("Enterprise Dashboards"));
    enterpriseRow.enterpriseLinks().shouldHave(size(2));

    DataInsightRow dataInsightRow = page.dataInsightRow();
    dataInsightRow.shouldBe(visible);
    dataInsightRow.dataInsightTitle().shouldBe(text("Data Insights"));
    dataInsightRow.dataInsightLinks().shouldHave(size(3));

    for (int i = 0; i < dashboardList.dashboardMetadata.size(); i++) {
      DashboardMetadataDTO dashboard = dashboardList.dashboardMetadata.get(i);
      NavigationListItem listItem = page.navigationListItem(dashboard.dashboardId);
      listItem.shouldHave(text(dashboard.title));
      if (!StringUtils.equals(dashboard.dashboardId, URL_DASHBOARD_ID)) {
        SelenideElement textLink = listItem.activeLink();
        textLink.shouldBe(visible);

        if (StringUtils.isNotBlank(dashboard.sinceIQVersion)) {
          String fullVersion = testCLMServer.getCLMServer().getInstance(VersionService.class).getLogDisplayVersion();
          int userVersion = Integer.parseInt(fullVersion.substring(0, fullVersion.indexOf("-")).replace(".0", ""));
          int sinceIqVersion = Integer.parseInt(dashboard.sinceIQVersion);
          if (userVersion < sinceIqVersion) {
            textLink.shouldHave(attribute("aria-disabled", "true"));
            textLink.shouldNotHave(attribute("href"));
          }
          else {
            textLink.shouldBe(enabled);
            textLink.shouldHave(attribute("href", EnterpriseReportingDashboardPage.url(dashboard.dashboardId)));
          }
        }
      }
      else {
        listItem.disabledLink().shouldBe(visible);
      }
    }
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testPageNavigation() {
    final var noGroupId = "";
    final var noIqVersion = "";
    DashboardsVersionDTO version = new DashboardsVersionDTO(2);
    DashboardMetadataDTO dashboardMetadataEnterpriseNoVersion =
        createDashboardMetadata("enterprise", 1, noIqVersion, noGroupId);
    DashboardMetadataDTO dashboardMetadataEnterpriseVersion =
        createDashboardMetadata("enterprise", 2, "500", noGroupId);
    DashboardMetadataDTO dashboardMetadataDataInsight =
        createDashboardMetadata("dataInsight", 3, noIqVersion, noGroupId);
    DashboardGroupMetadataDTO dashboardGroupMetadata = createDashboardGroupMetadata();
    DashboardMetadataListDTO dashboardList = new DashboardMetadataListDTO(version,
        List.of(
            dashboardMetadataEnterpriseNoVersion,
            dashboardMetadataEnterpriseVersion,
            dashboardMetadataDataInsight),
        List.of(dashboardGroupMetadata));

    setupTests(dashboardList, version);
    refreshOrOpen(EnterpriseReportingDashboardPage.url(URL_DASHBOARD_ID));
    SelenideElement link = page.navigationListItem(dashboardMetadataEnterpriseNoVersion.dashboardId).activeLink();
    link.click();
    waitUntilUrl(EnterpriseReportingDashboardPage.url(dashboardMetadataEnterpriseNoVersion.dashboardId));
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

  private static DashboardMetadataDTO createDashboardMetadata(String category,
                                                              int order,
                                                              String sinceIqVersion,
                                                              String groupId) 
  {
    return new DashboardMetadataDTO("dashboard-id-" + order, groupId, "title" + order, category, "description",
        Arrays.asList("feature 1", "feature 2"), "button text", "rolling-recap.svg", "faBrain", order, true,
        "dashboards/rolling_recap::rolling_recap", null, null, sinceIqVersion);
  }

  private static DashboardGroupMetadataDTO createDashboardGroupMetadata() {
    return new DashboardGroupMetadataDTO("group-id", "description",
        Arrays.asList("group feature 1", "group feature 2"), "faShield", false,
        null, null, "group title");
  }
}
