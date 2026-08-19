/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.List;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.CiCdConfigurationModalPage;
import com.sonatype.clm.testing.playwright.pages.CiCdConfigurationModalPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DeveloperRiskTablePage;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the CI/CD Configuration modal's Jenkins wizard. The wizard is opened by the
 * "Configure" CI/CD button on a Developer Dashboard risk-table row whose
 * {@code ciIntegrationEnabled} is false (fresh applications by default).
 */
public class CiCdWizardJenkinsPlaywrightTest
    extends AbstractIqUiTest
{
  private Organization org;

  private Application app;

  private String appName;

  @BeforeEach
  public void seedAppWithoutCiConfiguration() {
    org = tempEntity.newOrganization("cicd-" + TemporaryEntity.uuid());
    appName = "cicd-app-" + TemporaryEntity.uuid();
    app = tempEntity.newApplication(appName, appName, org.getId());
    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();
  }

  private CiCdConfigurationModalPageAssertions openCiCdModalForSeededApp() {
    DeveloperRiskTablePage riskTable = new DeveloperRiskTablePage();
    riskTable.cicdConfigureButtonInRow(riskTable.rowByAppName(appName)).click();
    CiCdConfigurationModalPage modalPage = new CiCdConfigurationModalPage();
    CiCdConfigurationModalPageAssertions assertions = new CiCdConfigurationModalPageAssertions(modalPage);
    assertions.shouldShowModal();
    return assertions;
  }

  @Test
  @Tag("regression")
  public void testCiCdWizardJenkins_modalRendersWithStepCardsAndDocLinks() {
    CiCdConfigurationModalPageAssertions assertions = openCiCdModalForSeededApp();

    assertions.shouldShowWizardWithStepCards();
    assertions.shouldHaveThreeViewDocumentationLinksOpeningInNewTabs();
    assertions.shouldShowMoreInfoLinkOpeningInNewTab();
  }

  @Test
  @Tag("regression")
  public void testCiCdWizardJenkins_pipelineSnippetAndParameterDescriptionRender() {
    CiCdConfigurationModalPageAssertions assertions = openCiCdModalForSeededApp();

    assertions.shouldShowPipelineSnippet();
    assertions.shouldShowAllParameterTerms();
  }

  @Test
  @Tag("regression")
  public void testCiCdWizardJenkins_copyToClipboardWritesSnippetWithRowApplicationAndOrganization() {
    // Permissions are scoped to the per-test BrowserContext (AbstractPlaywrightTest:370), no cleanup needed.
    context.grantPermissions(List.of("clipboard-read", "clipboard-write"));

    CiCdConfigurationModalPageAssertions assertions = openCiCdModalForSeededApp();
    CiCdConfigurationModalPage modalPage = new CiCdConfigurationModalPage();
    assertions.shouldShowPipelineSnippet();

    modalPage.copyToClipboardButton().click();

    // navigator.clipboard.writeText is async, so poll readText until it is non-empty before
    // asserting. Waiting for "non-empty" (not for the substrings) keeps the wait and the
    // assertion from checking the same thing — AssertJ owns the substring check and its
    // failure diagnostic prints the actual clipboard contents.
    String clipboardText = readClipboardOnceSettled();
    assertThat(clipboardText)
        .contains("iqApplication: '" + app.getPublicId() + "'")
        .contains("iqOrganization: '" + org.getId() + "'");
  }

  private String readClipboardOnceSettled() {
    page.waitForFunction("() => navigator.clipboard.readText().then(t => t.length > 0)");
    return (String) page.evaluate("navigator.clipboard.readText()");
  }
}
