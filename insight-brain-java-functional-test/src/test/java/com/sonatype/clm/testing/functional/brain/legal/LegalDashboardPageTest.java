/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AttributionReportFormPage;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.functional.pages.LegalApplicationDetailsPage;
import com.sonatype.clm.testing.functional.pages.LegalDashboardPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;

import com.codeborne.selenide.Condition;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public class LegalDashboardPageTest
    extends AbstractFunctionalTest
{
  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(LegalDashboardPage.url(true));
    loginAsAdmin();
  }

  private void addComponentAndLicenses(
      Application application,
      String groupId,
      String artifactId,
      String version,
      String hash,
      String stageTypeId,
      String... licenseIds)
  {
    final ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    final ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(application.getId(), stageTypeId, hash,
            componentIdentifier);
    Arrays.stream(licenseIds)
        .forEach(licenseId -> tempEntity.newApplicationComponentLicense(applicationComponent.getId(), licenseId));
  }

  private void addEvaluationPoliciesToApplications(Application[] apps) {
    for (Application application : apps) {
      tempEntity.newPolicyEvaluation(application.getId(), "build", "", new Date());
    }
  }

  @Before
  public void start() throws IOException {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    testCLMServer.getHdsServer().respondWith("[]").atUri("/rest/license/metadata");
    app = tempEntity.newApplicationWithParent(LegalApplicationDetailsPage.class.getSimpleName(), "app", "org");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/file");

    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetails.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetailsList.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails/list");

    Application app1 = tempEntity.newApplicationWithParent(LegalApplicationDetailsPage.class.getSimpleName() + "1",
        "app1", "org1");
    Application app2 = tempEntity.newApplicationWithParent(LegalApplicationDetailsPage.class.getSimpleName() + "2",
        "app2", "org2");
    Application[] apps = {app, app1, app2};
    addEvaluationPoliciesToApplications(apps);

    String[] licenses = {"Apache-1.0", "MIT", "Apache-2.0", "Better-Cms-LA", "BSL-1.0", "CC-BY-NC-3.0", "CMRL-1.0",
      "GPL-2.0+-LGPL-3.0+", "GreenSock-Commercial-License", "Gridifier-Developer-LA",
      "Grammatica-BSD-3-Clause-Variant"};
    ComponentIdentifier[] componentIdentifiers = new ComponentIdentifier[licenses.length];

    String currentComponentName = "";

    for (int i = 1; i < 21; i++) {

      currentComponentName = (i == 1 || i == 12) ? "#$%&/" : "component";

      String stageType = i % 3 == 0 ? BuildStageType.ID : (i % 5 == 0 ? SourceStageType.ID : ReleaseStageType.ID);

      addComponentAndLicenses(apps[i % apps.length], "org.package", currentComponentName,
          (i % licenses.length + 1) + ".0", "hash" + (i % licenses.length + 1),
          stageType, licenses[i % licenses.length]);

      componentIdentifiers[i % licenses.length] = componentIdentifiers[i % licenses.length] != null
          ? componentIdentifiers[i % licenses.length]
          : ComponentIdentifier
              .createMavenCoordinates("org.package", currentComponentName, (i % licenses.length + 1) + ".0");

      tempEntity.newComponentObligation(
          componentIdentifiers[i % licenses.length], apps[i % apps.length].getId(),
          "Inclusion of Notice", "comment", ObligationStatus.FULFILLED, "hash" + i);
    }
  }

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }

  @Test
  public void testComponentsTabChange() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    ldp.componentsTab().click();
    ldp.componentsTab().shouldHave(Condition.cssClass("active"));
  }

  private void changeToComponentsTab(LegalDashboardPage ldp) {
    refreshOrOpen(LegalDashboardPage.url(true));
    ldp.componentsTab().click();
    ldp.componentsTab().shouldHave(Condition.cssClass("active"));
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
  }

  @Test
  public void testDisplayedComponents() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    eyesWatcher.eyesCheck();
    ldp.tableRows().shouldHave(size(10));
  }

  @Test
  public void testComponentsLinks() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.tableRows().shouldHave(size(10));
    ldp.tableRows().get(0).click();
    waitUntilUrl(BaseUrl.resolvePageUrl("/legal/component/hash2"));
  }

  @Test
  public void testComponentsPaginationIsPresent() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.pageButtons().shouldHave(size(2));
  }

  @Test
  public void testComponentsPaginationNextPage() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.pageButtons().get(1).should(Condition.exist);
    ldp.pageButtons().get(1).click();
    ldp.tableRows().shouldHave(size(1));
  }

  @Test
  public void testComponentsTableSortingByComponentName() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);

    ldp.componentsTableComponentNameCols().get(0).shouldHave(Condition.text("org.package : #$%&/ : 2.0"));
    ldp.componentsTableComponentNameHeader().shouldHave(Condition.attribute("aria-sort", "none"));
    ldp.componentsTableComponentNameHeaderSortBtn().click();
    ldp.componentsTableComponentNameCols().get(0).shouldHave(Condition.text("org.package : #$%&/ : 2.0"));
    ldp.componentsTableComponentNameHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    ldp.componentsTableComponentNameHeaderSortBtn().click();
    ldp.componentsTableComponentNameCols().get(0).shouldHave(Condition.text("org.package : component : 9.0"));
    ldp.componentsTableComponentNameHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
  }

  @Test
  public void testComponentsTableSortingByLicenseName() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);

    ldp.componentsTableLicenseNameCols().get(0).shouldHave(Condition.text("MIT"));
    ldp.componentsTableLicenseNameHeader().shouldHave(Condition.attribute("aria-sort", "none"));
    ldp.componentsTableLicenseNameHeaderSortBtn().click();
    ldp.componentsTableLicenseNameCols().get(0).shouldHave(Condition.text("Apache-1.0"));
    ldp.componentsTableLicenseNameHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    ldp.componentsTableLicenseNameHeaderSortBtn().click();
    ldp.componentsTableLicenseNameCols().get(0).shouldHave(Condition.text("MIT"));
    ldp.componentsTableLicenseNameHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
  }

  @Test
  public void testComponentsTableSortingByApplicationCount() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);

    ldp.componentsTableApplicationCountCols().get(0).shouldHave(Condition.text("2"));
    ldp.componentsTableApplicationCountHeader().shouldHave(Condition.attribute("aria-sort", "none"));
    ldp.componentsTableApplicationCountHeaderSortBtn().click();
    ldp.componentsTableApplicationCountCols().get(0).shouldHave(Condition.text("1"));
    ldp.componentsTableApplicationCountHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    ldp.componentsTableApplicationCountHeaderSortBtn().click();
    ldp.componentsTableApplicationCountCols().get(0).shouldHave(Condition.text("2"));
    ldp.componentsTableApplicationCountHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
  }

  @Test
  public void testComponentsDisabledSearchButtonOnTableLoad() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);

    Wait<WebDriver> wait = getWebDriverAwait();
    // Wait until the tab page is fully loaded.
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    wait.until(ExpectedConditions.elementToBeClickable(ldp.componentsSearchButton()));

    ldp.componentsSearchInput().setValue("package");
    ldp.componentsSearchInput().pressEnter();
    ldp.componentsSearchButton().shouldBe(Condition.disabled);

    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.componentsSearchButton().shouldBe(Condition.enabled);
  }

  @Test
  public void testComponentsSearchInputValidationFor3CharsMin() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.componentsSearchInput().setValue("1.");
    ldp.componentsSearchButton().shouldBe(Condition.disabled);
    ldp.componentsSearchInput().parent().parent().shouldHave(Condition.cssClass("invalid"));
    ldp.componentsSearchInputErrorMessage().has(Condition.text("You must input at least three characters to search"));
  }

  @Test
  public void testComponentsSearch() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.componentsSearchInput().setValue("1.0");
    ldp.componentsSearchButton().click();
    ldp.componentsTableApplicationCountCols().shouldHave(size(2));
    ldp.pageButtons().shouldHave(size(1));
  }

  @Test
  public void testComponentsSearchByEnterKeystroke() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.componentsSearchInput().setValue("1.0");
    ldp.componentsSearchInput().pressEnter();
    ldp.componentsTableApplicationCountCols().shouldHave(size(2));
    ldp.pageButtons().shouldHave(size(1));
  }

  @Test
  public void testComponentsSearchWithEmptyString() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.componentsSearchInput().setValue("");
    ldp.componentsSearchButton().click();
    ldp.componentsTableApplicationCountCols().shouldHave(size(10));
    ldp.pageButtons().shouldHave(size(2));
  }

  @Test
  public void testComponentsSearchWithNotMatchableString() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.componentsSearchInput().setValue("not matchable string");
    ldp.componentsSearchButton().click();
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.noComponentsFoundMessage()));
  }

  @Test
  public void testComponentsPaginationResetOnSearchStringChange() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.componentsSearchInput().setValue("");
    ldp.componentsSearchButton().click();
    ldp.componentsTableApplicationCountCols().shouldHave(size(10));
    ldp.pageButtons().shouldHave(size(2));
    ldp.pageButtons().get(1).click();
    ldp.componentsSearchInput().setValue(": 1.0");
    ldp.componentsSearchInput().pressEnter();
    ldp.selectedPaginationPage().shouldHave(Condition.text("1"));
  }

  @Test
  public void testComponentsSearchWithSpecialCharsString() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.componentsSearchInput().setValue("#$%&/");
    ldp.componentsSearchButton().click();
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
  }

  @Test
  public void testComponentLegalOverviewBackButtonFromComponentsDashboard() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.tableRows().get(0).click();
    ComponentLegalOverviewPage.AttributionSummaryTile attributionSummaryTile =
        new ComponentLegalOverviewPage.AttributionSummaryTile();
    wait.until(ExpectedConditions.visibilityOf(attributionSummaryTile.getElement()));
    ComponentLegalOverviewPage.backLink().click();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
  }

  @Test
  public void testComponentsFilterByOrganizations() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.filterButton().click();
    ldp.filterCollapsibleItems().get(0).click();
    ldp.filterOrganizationsCheckBoxes().get(3).click();
    ldp.filterApplyButton().click();
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.selectedPaginationPage().shouldHave(Condition.text("1"));
    ldp.tableRows().shouldHave(size(7));
    ldp.tableRows().get(0).shouldHave(Condition.text("org.package : component : 1.0 Apache-1.0 1 - / -"));
    ldp.tableRows()
        .get(6)
        .shouldHave(
            Condition.text("org.package : component : 9.0 GreenSock-Commercial-License 1 - / -"));
    ldp.filterCollapsibleItems().get(0).click();
    ldp.filterOrganizationsCheckBoxes().get(3).click();
    ldp.filterApplyButton().click();
  }

  @Test
  public void testComponentsFilterByApplication() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.filterButton().click();
    ldp.filterCollapsibleItems().get(1).click();
    ldp.filterApplicationsCheckBoxes().get(1).click();
    ldp.filterApplyButton().click();
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.selectedPaginationPage().shouldHave(Condition.text("1"));
    ldp.tableRows().shouldHave(size(6));
    ldp.tableRows().get(0).shouldHave(Condition.text("org.package : #$%&/ : 2.0 MIT 1 0 / 4"));
    ldp.tableRows().get(4).shouldHave(Condition.text("org.package : component : 7.0 CMRL-1.0 1 - / -"));
    ldp.filterCollapsibleItems().get(1).click();
    ldp.filterApplicationsCheckBoxes().get(1).click();
    ldp.filterApplyButton().click();
  }

  @Test
  public void testComponentsFilterByApplicationCategories() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.filterButton().click();
    ldp.filterCollapsibleItems().get(2).click();
    ldp.filterApplicationCategoriesCheckBoxes().get(1).click();
    ldp.filterApplyButton().click();
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.selectedPaginationPage().shouldHave(Condition.text("1"));
    ldp.tableRows().shouldHave(size(10));
    ldp.tableRows().get(0).shouldHave(Condition.text("org.package : #$%&/ : 2.0 MIT 2 0 / 4"));
    ldp.tableRows()
        .get(9)
        .shouldHave(
            Condition.text("org.package : component : 8.0 GPL-2.0+ or LGPL-3.0+ 2 - / -"));
    ldp.filterCollapsibleItems().get(2).click();
    ldp.filterApplicationCategoriesCheckBoxes().get(1).click();
    ldp.filterApplyButton().click();
  }

  @Test
  public void testComponentsFilterByStages() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.filterButton().click();
    ldp.filterCollapsibleItems().get(3).click();
    ldp.filterStagesCheckBoxes().get(1).click();
    ldp.filterApplyButton().click();
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.selectedPaginationPage().shouldHave(Condition.text("1"));
    ldp.tableRows().shouldHave(size(3));
    ldp.tableRows().get(0).shouldHave(Condition.text("org.package : component : 10.0 Gridifier-Developer-LA 1 - / -"));
    ldp.tableRows().get(2).shouldHave(Condition.text("org.package : component : 6.0 CC-BY-NC-3.0 1 - / -"));

    ldp.filterStagesCheckBoxes().get(1).click();
    ldp.filterStagesCheckBoxes().get(2).click();
    ldp.filterApplyButton().click();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.selectedPaginationPage().shouldHave(Condition.text("1"));
    ldp.tableRows().shouldHave(size(6));
    ldp.tableRows().get(0).shouldHave(Condition.text("org.package : #$%&/ : 2.0 MIT 1 0 / 4"));
    ldp.tableRows().get(5).shouldHave(Condition.text("org.package : component : 8.0 GPL-2.0+ or LGPL-3.0+ 1 - / -"));

    ldp.filterStagesCheckBoxes().get(2).click();
    ldp.filterStagesCheckBoxes().get(4).click();
    ldp.filterApplyButton().click();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.selectedPaginationPage().shouldHave(Condition.text("1"));
    ldp.tableRows().shouldHave(size(9));
    ldp.tableRows().get(0).shouldHave(Condition.text("org.package : #$%&/ : 2.0 MIT 1 0 / 4"));
    ldp.tableRows()
        .get(8)
        .shouldHave(
            Condition.text("org.package : component : 9.0 GreenSock-Commercial-License 2 - / -"));
    ldp.filterCollapsibleItems().get(3).click();
    ldp.filterStagesCheckBoxes().get(4).click();
    ldp.filterApplyButton().click();
  }

  @Test
  public void testComponentsFilterByReviewProgress() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.filterButton().click();
    ldp.filterCollapsibleItems().get(4).click();
    ldp.filterReviewProgressCheckBoxes().get(2).click();
    ldp.filterApplyButton().click();
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(ldp.tableRows().get(0)));
    ldp.selectedPaginationPage().shouldHave(Condition.text("1"));
    ldp.tableRows().shouldHave(size(9));
    ldp.tableRows().get(0).shouldHave(Condition.text("org.package : component : 1.0 Apache-1.0 1 - / -"));
    ldp.tableRows()
        .get(8)
        .shouldHave(
            Condition.text("org.package : component : 9.0 GreenSock-Commercial-License  2 - / -"));
    ldp.filterCollapsibleItems().get(4).click();
    ldp.filterReviewProgressCheckBoxes().get(2).click();
    ldp.filterApplyButton().click();
  }

  @Test
  public void testCreateAttributionReportButtonClick() {
    refreshOrOpen(LegalDashboardPage.url(true));
    Wait<WebDriver> wait = getWebDriverAwait();
    LegalDashboardPage ldp = new LegalDashboardPage();
    ldp.applicationsTab().click();
    eyesWatcher.eyesCheck();
    ldp.tableRows().shouldHave(size(3));
    ldp.createAttributionReportButton().click();
    ldp.generateAttributionReportButton().click();
    AttributionReportFormPage arfp = new AttributionReportFormPage();
    wait.until(ExpectedConditions.visibilityOf(arfp.getTitleInput()));
    arfp.getTitleInput().shouldHave(Condition.value("Attribution Report"));
  }
}
