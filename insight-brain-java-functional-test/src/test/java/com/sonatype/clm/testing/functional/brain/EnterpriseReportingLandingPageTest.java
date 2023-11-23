/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.EnterpriseReportingLandingPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.enterprise.reporting.DashboardMetadataDTO;
import com.sonatype.insight.brain.enterprise.reporting.DashboardMetadataListDTO;
import com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingConfigDTO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.attribute;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.INTEGRATED_ENTERPRISE_REPORTING;

public class EnterpriseReportingLandingPageTest
    extends AbstractFunctionalTest
{
  private final EnterpriseReportingLandingPage page = new EnterpriseReportingLandingPage();

  private final SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(EnterpriseReportingLandingPage.url());
    loginAsAdmin();
  }

  @Test
  public void testFeatureDisabled_ShowsError() {
    pageHeaderShouldBeVisible();
    page.enterpriseReportingNotEnabledError().shouldBe(visible);
    page.reports().shouldBe(hidden);
  }

  @Test
  public void testFeatureEnabled_Success() throws IOException {
    mockHDSResponses();
    DashboardMetadataDTO spotlightDashboardMetadataDTO = mockDashboardMetadataDTOSpotlight();
    DashboardMetadataDTO nonSpotlightDashboardMetadataDTO = mockDashboardMetadataDTO();
    testCLMServer.getHdsServer()
        .respondWith(new DashboardMetadataListDTO(Arrays.asList(spotlightDashboardMetadataDTO,
            nonSpotlightDashboardMetadataDTO)))
        .atUri("/rest/enterpriseReporting/dashboards");
    enableEnterpriseReporting();
    refreshOrOpen(EnterpriseReportingLandingPage.url());
    page.enterpriseReportingNotEnabledError().shouldBe(hidden);
    pageHeaderShouldBeVisible();
    page.reports().shouldBe(visible);
    assertReportContent(spotlightDashboardMetadataDTO);
    assertReportContent(nonSpotlightDashboardMetadataDTO);
    contactusShouldBeVisible();
    eyesWatcher.eyesCheck();
  }

  private void pageHeaderShouldBeVisible() {
    page.heading().shouldBe(visible);
    page.heading().shouldHave(text("Data Insights"));
    page.subtitle().shouldBe(visible);
    page.subtitle().shouldHave(text("Experimental and Collaborative Ideation"));
    page.description().shouldBe(visible);
    assertThat(page.description().innerText()).isNotEmpty();
  }

  private void assertReportContent(DashboardMetadataDTO dashboardMetadataDTO) {
    String imageSource = BaseUrl.resolveRestUrl("/enterpriseReporting/dashboard/icons/{iconName}",
        dashboardMetadataDTO.previewImage);
    int index = dashboardMetadataDTO.priority - 1;
    SelenideElement report = page.reports().findAll(".iq-enterprise-reporting__dashboard").get(index);
    SelenideElement icon = report.$(By.tagName("img"));
    ElementsCollection features =
        report.$(".iq-enterprise-reporting__dashboard-data__features").findAll(".nx-list__item");
    report.$(".iq-enterprise-reporting__dashboard__header-title .nx-h2").shouldHave(text(dashboardMetadataDTO.title));
    icon.shouldHave(attribute("src", imageSource));
    icon.isDisplayed();
    report.$(".nx-p").shouldBe(text(dashboardMetadataDTO.description));
    report.$(".iq-enterprise-reporting__dashboard__btn").shouldHave(text(dashboardMetadataDTO.accessButtonText));
    assertThat(features.size()).isEqualTo(dashboardMetadataDTO.features.size());
    for (int i = 0; i < features.size(); i++) {
      features.get(i).shouldHave(text(dashboardMetadataDTO.features.get(i)));
    }
    if (dashboardMetadataDTO.spotlight) {
      report.$(".iq-enterprise-reporting__dashboard__spotlight").is(visible);
    }
  }

  private void contactusShouldBeVisible() {
    page.contactus().shouldBe(visible);
    page.contactus().$(".nx-tile-header__title .nx-h2").shouldHave(text("Contact Us"));
    ElementsCollection subsections = page.contactus().$(".nx-tile-content").findAll(".nx-tile-subsection");
    SelenideElement firstSubSection = subsections.get(0);
    SelenideElement secondSubSection = subsections.get(1);
    SelenideElement thirdSubSection = subsections.get(2);
    firstSubSection.$(".nx-h3").shouldHave(text("Schedule a Discussion"));
    assertThat(firstSubSection.$(".nx-p").innerText()).isNotEmpty();
    firstSubSection.$(".nx-text-link")
        .shouldHave(attribute("href", "mailto:data-insights-pm@sonatype.com"));
    firstSubSection.$(By.tagName("span")).shouldHave(text("data-insights-pm@sonatype.com"));
    secondSubSection.$(".nx-h3").shouldHave(text("Suggest an Improvement"));
    assertThat(secondSubSection.$(".nx-p").innerText()).isNotEmpty();
    secondSubSection.$(".nx-text-link")
        .shouldHave(attribute("href", "http://links.sonatype.com/products/nxiq/feedback/data-insights-ideas"));
    secondSubSection.$(By.tagName("span")).shouldHave(text("Sonatype Ideas Portal - Data Insights"));
    thirdSubSection.$(".nx-h3").shouldHave(text("Receive Technical Support"));
    assertThat(thirdSubSection.$(".nx-p").innerText()).isNotEmpty();
    thirdSubSection.$(".nx-text-link")
        .shouldHave(attribute("href", "http://links.sonatype.com/products/nexus/pro/support"));
    thirdSubSection.$(By.tagName("span")).shouldHave(text("support.sonatype.com"));
  }

  private void enableEnterpriseReporting() {
    dao.insert(new SystemConfigurationProperty(INTEGRATED_ENTERPRISE_REPORTING, "true"));
  }

  private void mockHDSResponses() throws IOException {
    testCLMServer.getHdsServer()
        .respondWith(new EnterpriseReportingConfigDTO("sonatype.looker.com"))
        .atUri(ENTERPRISE_REPORTING_CONFIG_PATH);
    testCLMServer.getHdsServer()
        .respondWith(Files.readAllBytes(Paths.get(getClass()
            .getResource("/EnterpriseReporting/icons_svg.zip").getPath())))
        .atUri(ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);
  }

  private static DashboardMetadataDTO mockDashboardMetadataDTOSpotlight() {
    return new DashboardMetadataDTO("id", "title", "description", Arrays.asList("feature 1","feature 2"),
        "button text", "rolling-recap.svg", 1, true);
  }

  private static DashboardMetadataDTO mockDashboardMetadataDTO() {
    return new DashboardMetadataDTO("id 2", "title 2", "description 2", Arrays.asList("feature 3","feature 4"),
        "button text 2", "rolling-recap.svg", 2, false);
  }
}
