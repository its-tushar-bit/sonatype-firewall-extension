/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.BaseUrlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.DeveloperRiskTablePage;
import com.sonatype.clm.testing.playwright.pages.ScmWizardPage;
import com.sonatype.clm.testing.playwright.pages.ScmWizardPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Tests for the SCM Integrations modal's wizard card (opened from the risk-table SCM button). */
public class ScmWizardPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String GITHUB_TOKEN_URL = "https://github.com/settings/tokens";

  private static final String CUSTOM_BASE_URL = "http://iq.example.com:8070";

  private Organization org;

  private Application app;

  private String appName;

  private String originalBaseUrl;

  @BeforeEach
  public void seedAppWithoutScmConfiguration() {
    org = tempEntity.newOrganization("scm-" + TemporaryEntity.uuid());
    appName = "scm-app-" + TemporaryEntity.uuid();
    app = tempEntity.newApplication(appName, appName, org.getId());
    originalBaseUrl = lookup(SystemConfigurationPropertyDAO.class).get(SystemConfigurationProperty.BASE_URL);
    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();
  }

  @AfterEach
  public void restoreBaseUrl() {
    // Restore via the same UI path so the IQ server's config cache is invalidated correctly.
    playwrightRefreshOrOpen(BaseUrlConfigurationPage.url());
    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();
    if (originalBaseUrl == null || originalBaseUrl.isEmpty()) {
      assertThat(configPage.baseUrlAttribute()).isVisible();
      if (configPage.deleteButton().isEnabled()) {
        configPage.openDeleteModal();
        configPage.deleteModalSubmitButton().click();
      }
    }
    else {
      assertThat(configPage.baseUrlAttribute()).isVisible();
      configPage.baseUrlAttribute().clear();
      configPage.baseUrlAttribute().fill(originalBaseUrl);
      configPage.saveButton().click();
      waitForSubmitMaskSuccess();
    }
  }

  private ScmWizardPageAssertions openScmModalForSeededApp() {
    DeveloperRiskTablePage riskTable = new DeveloperRiskTablePage();
    riskTable.scmConfigureButtonInRow(riskTable.rowByAppName(appName)).click();
    ScmWizardPage wizardPage = new ScmWizardPage();
    ScmWizardPageAssertions assertions = new ScmWizardPageAssertions(wizardPage);
    assertions.shouldShowModal();
    return assertions;
  }

  private void setBaseUrlViaUi(String baseUrl) {
    playwrightRefreshOrOpen(BaseUrlConfigurationPage.url());
    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();
    assertThat(configPage.baseUrlAttribute()).isVisible();
    configPage.baseUrlAttribute().clear();
    configPage.baseUrlAttribute().fill(baseUrl);
    configPage.saveButton().click();
    waitForSubmitMaskSuccess();
  }

  private void clearBaseUrlViaUi() {
    playwrightRefreshOrOpen(BaseUrlConfigurationPage.url());
    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();
    assertThat(configPage.baseUrlAttribute()).isVisible();
    if (configPage.deleteButton().isEnabled()) {
      configPage.openDeleteModal();
      configPage.deleteModalSubmitButton().click();
    }
  }

  @Test
  @Tag("regression")
  public void testScmWizard_modalRendersWizardCardWithCoreSectionsAndLinks() {
    ScmWizardPageAssertions assertions = openScmModalForSeededApp();

    assertions.shouldShowWizardCardWithCoreSections();
    assertions.shouldShowAutomaticSourceControlLinkInNewTab();
    assertions.shouldShowApplicationSourceControlLinkInNewTab();
  }

  @Test
  @Tag("regression")
  public void testScmWizard_githubTokenUrlRenders() {
    ScmWizardPageAssertions assertions = openScmModalForSeededApp();

    assertions.shouldShowTokenUrl(GITHUB_TOKEN_URL);
  }

  @Test
  @Tag("regression")
  public void testScmWizard_configureBaseUrlSectionShownWhenBaseUrlNotSet() {
    clearBaseUrlViaUi();
    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());

    ScmWizardPageAssertions assertions = openScmModalForSeededApp();
    assertions.shouldShowConfigureBaseUrlSection();
  }

  @Test
  @Tag("regression")
  public void testScmWizard_configureBaseUrlSectionHiddenWhenBaseUrlSet() {
    setBaseUrlViaUi(CUSTOM_BASE_URL);
    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());

    ScmWizardPageAssertions assertions = openScmModalForSeededApp();
    assertions.shouldNotShowConfigureBaseUrlSection();
  }
}
