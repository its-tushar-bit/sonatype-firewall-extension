/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.time.Duration;
import java.util.Arrays;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.LegalApplicationDetailsPage;
import com.sonatype.clm.testing.functional.pages.LegalDashboardPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

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
          String... licenseIds)
  {
    final ComponentIdentifier componentIdentifier =
            ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    final ApplicationComponent applicationComponent =
            tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, hash,
                    componentIdentifier);
    Arrays.stream(licenseIds)
            .forEach(licenseId -> tempEntity.newApplicationComponentLicense(applicationComponent.getId(), licenseId));
  }

  @Before
  public void start() {
    testCLMServer.getHdsServer().respondWith("[]").atUri("/rest/license/metadata");
    app = tempEntity.newApplicationWithParent(LegalApplicationDetailsPage.class.getSimpleName(), "app", "org");
    Application app1 = tempEntity.newApplicationWithParent(LegalApplicationDetailsPage.class.getSimpleName() + "1",
            "app1", "org1");
    Application app2 = tempEntity.newApplicationWithParent(LegalApplicationDetailsPage.class.getSimpleName() + "2",
            "app2", "org2");
    Application[] apps = {app, app1, app2};
    String[] licenses = {"Apache-1.0", "MIT", "Apache-2.0", "Better-Cms-LA", "BSL-1.0", "CC-BY-NC-3.0", "CMRL-1.0",
        "GPL-2.0+-LGPL-3.0+", "GreenSock-Commercial-License", "Gridifier-Developer-LA", 
        "Grammatica-BSD-3-Clause-Variant"};
    ComponentIdentifier[] componentIdentifiers = new ComponentIdentifier[licenses.length];

    for (int i = 1; i < 21; i++) {
      addComponentAndLicenses(apps[ i % apps.length ], "org.package", "component",
              (i % licenses.length + 1 ) + ".0", "hash" + (i % licenses.length + 1), licenses[ i % licenses.length ]);

      componentIdentifiers[ i % licenses.length ] = componentIdentifiers[ i % licenses.length ] != null ?
              componentIdentifiers[ i % licenses.length ] : ComponentIdentifier
                .createMavenCoordinates("org.package", "component", (i % licenses.length + 1) + ".0");

      tempEntity.newComponentObligation(
              componentIdentifiers[ i % licenses.length ], apps[ i % apps.length ].getId(),
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
    ldp.tableRows().shouldHaveSize(10);
  }

  @Test
  public void testComponentsLinks() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.tableRows().shouldHaveSize(10);
    ldp.tableRows().get(0).click();
    waitUntilUrl(BaseUrl.resolvePageUrl("/legal/component/hash1"));
  }

  @Test
  public void testComponentsPaginationIsPresent() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.pageButtons().shouldHaveSize(2);
  }

  @Test
  public void testComponentsPaginationNextPage() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.pageButtons().get(1).should(Condition.exist);
    ldp.pageButtons().get(1).click();
    ldp.tableRows().shouldHaveSize(1);
  }

  @Test
  public void testComponentsTableSortingByComponentName() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);

    ldp.componentsTableComponentNameCols().get(0).shouldHave(Condition.text("org.package : component : 1.0"));
    ldp.componentsTableComponentNameHeader().shouldHave(Condition.attribute("aria-sort", "none"));
    ldp.componentsTableComponentNameHeaderSortBtn().click();
    ldp.componentsTableComponentNameCols().get(0).shouldHave(Condition.text("org.package : component : 1.0"));
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

    ldp.componentsTableLicenseNameCols().get(0).shouldHave(Condition.text("Apache-1.0"));
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

    ldp.componentsTableApplicationCountCols().get(0).shouldHave(Condition.text("1"));
    ldp.componentsTableApplicationCountHeader().shouldHave(Condition.attribute("aria-sort", "none"));
    ldp.componentsTableApplicationCountHeaderSortBtn().click();
    ldp.componentsTableApplicationCountCols().get(0).shouldHave(Condition.text("1"));
    ldp.componentsTableApplicationCountHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    ldp.componentsTableApplicationCountHeaderSortBtn().click();
    ldp.componentsTableApplicationCountCols().get(0).shouldHave(Condition.text("2"));
    ldp.componentsTableApplicationCountHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
  }
}
