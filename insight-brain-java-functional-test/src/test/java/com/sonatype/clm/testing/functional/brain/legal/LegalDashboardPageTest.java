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
          String groupId,
          String artifactId,
          String version,
          String hash,
          String... licenseIds)
  {
    final ComponentIdentifier componentIdentifier =
            ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    final ApplicationComponent applicationComponent =
            tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
                    componentIdentifier);
    Arrays.stream(licenseIds)
            .forEach(licenseId -> tempEntity.newApplicationComponentLicense(applicationComponent.getId(), licenseId));
  }

  @Before
  public void start() {
    app = tempEntity.newApplicationWithParent(LegalApplicationDetailsPage.class.getSimpleName(), "app", "org");

    for (int i = 1; i < 12; i++) {
      addComponentAndLicenses("org.package", "component" + i, i + ".0", "hash" + i, "Apache-2.0");
      tempEntity.newComponentObligation(ComponentIdentifier
                      .createMavenCoordinates("org.package", "component" + i, i + ".0"),
              app.getId(), "Inclusion of Notice", "comment", ObligationStatus.FULFILLED, "hash" + i);
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
    wait.until(ExpectedConditions.visibilityOf(ldp.componentItems().get(0)));
  }

  @Test
  public void testDisplayedComponents() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.componentItems().shouldHaveSize(10);
  }

  @Test
  public void testComponentsLinks() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.componentItems().shouldHaveSize(10);
    ldp.componentItems().get(0).click();
    waitUntilUrl(BaseUrl.resolvePageUrl("/legal/component/hash1"));
  }

  @Test public void testPaginationIsPresent() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.pageButtons().shouldHaveSize(2);
  }

  @Test public void testPaginationNextPage() {
    refreshOrOpen(LegalDashboardPage.url(true));
    LegalDashboardPage ldp = new LegalDashboardPage();
    this.changeToComponentsTab(ldp);
    ldp.pageButtons().get(1).should(Condition.exist);
    ldp.pageButtons().get(1).click();
    ldp.componentItems().shouldHaveSize(1);
  }
}
