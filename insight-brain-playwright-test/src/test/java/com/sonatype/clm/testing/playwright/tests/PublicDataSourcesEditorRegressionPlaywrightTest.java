/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.PublicDataSourcesEditorPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

/**
 * Regression tests for the Public Data Sources (CPE matching) editor.
 * <p>
 * All tests require {@code CPE_MATCHING}; {@code @Before} enables it. Tests that override the
 * license call {@code setLicensedProducts}/{@code setFeatures} before navigating;
 * {@link AbstractIqUiTest#afterTest()} auto-resets the license after each test.
 */
public class PublicDataSourcesEditorRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String CHILD_ORG_PREFIX = "PDS-Child-Org";

  private static final String APP_PREFIX = "PDS-App";

  private static final String APP_PUBLIC_ID_PREFIX = "pds-app-";

  private static final String PARENT_ORG_PREFIX = "PDS-Parent-Org";

  private static final String OVERRIDE_CHILD_ORG_PREFIX = "PDS-Override-Child";

  private static final String LICENSE_ERROR_TEXT = "not supported by your license";

  private static final String SBOM_INFO_TEXT = "Public Data Sources are configured within Lifecycle";

  @Before
  public void enableCpeMatchingAndLogin() {
    setFeatures(LicensedFeature.CPE_MATCHING);
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEditorRendersWithAllRadiosWhenCpeMatchingEnabled() {
    Organization childOrg = tempEntity.newOrganization(CHILD_ORG_PREFIX + "-" + TemporaryEntity.uuid());
    PublicDataSourcesEditorPage editor = new PublicDataSourcesEditorPage();

    navigateAndWaitForUrl(
        PublicDataSourcesEditorPage.orgUrl(childOrg.getId()),
        childOrg.getId() + PublicDataSourcesEditorPage.PUBLIC_DATA_SOURCES_URL_FRAGMENT);

    assertThat(editor.settingsTile()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.inheritRadio().label()).isVisible();
    assertThat(editor.enabledRadio().label()).isVisible();
    assertThat(editor.disabledRadio().label()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testInheritRadioAbsentOnRootOrg() {
    PublicDataSourcesEditorPage editor = new PublicDataSourcesEditorPage();

    navigateAndWaitForUrl(
        PublicDataSourcesEditorPage.orgUrl(ROOT_ORGANIZATION_ID),
        ROOT_ORGANIZATION_ID + PublicDataSourcesEditorPage.PUBLIC_DATA_SOURCES_URL_FRAGMENT);

    assertThat(editor.settingsTile()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.inheritRadio().label()).isHidden();
    assertThat(editor.enabledRadio().label()).isVisible();
    assertThat(editor.disabledRadio().label()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAllowOverrideCheckboxAbsentForApplication() {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(APP_PREFIX + "-org-" + suffix);
    Application app = tempEntity.newApplication(APP_PREFIX + "-" + suffix, APP_PUBLIC_ID_PREFIX + suffix, org.getId());

    PublicDataSourcesEditorPage editor = new PublicDataSourcesEditorPage();

    navigateAndWaitForUrl(
        PublicDataSourcesEditorPage.appUrl(app.getPublicId()),
        app.getPublicId() + PublicDataSourcesEditorPage.PUBLIC_DATA_SOURCES_URL_FRAGMENT);

    assertThat(editor.settingsTile()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.enabledRadio().label()).isVisible();
    assertThat(editor.allowOverrideCheckbox().label()).isHidden();
  }

  @Test
  @Category(RegressionTest.class)
  public void testRadiosDisabledWhenParentOverrideNotAllowed() {
    Organization parentOrg = tempEntity.newOrganization(PARENT_ORG_PREFIX + "-" + TemporaryEntity.uuid());
    Organization childOrg = tempEntity.newOrganization(
        OVERRIDE_CHILD_ORG_PREFIX + "-" + TemporaryEntity.uuid(), parentOrg);
    tempEntity.newCpeMatchingConfiguration(parentOrg.getId(), true, false);

    PublicDataSourcesEditorPage editor = new PublicDataSourcesEditorPage();

    navigateAndWaitForUrl(
        PublicDataSourcesEditorPage.orgUrl(childOrg.getId()),
        childOrg.getId() + PublicDataSourcesEditorPage.PUBLIC_DATA_SOURCES_URL_FRAGMENT);

    assertThat(editor.settingsTile()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.inheritRadio().input()).isDisabled();
    assertThat(editor.enabledRadio().input()).isDisabled();
    assertThat(editor.disabledRadio().input()).isDisabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testSettingsTileHiddenForSbomManagerOnlyLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.CPE_MATCHING);
    PublicDataSourcesEditorPage editor = new PublicDataSourcesEditorPage();

    playwrightRefresh();
    navigateAndWaitForUrl(
        PublicDataSourcesEditorPage.sbomManagerOrgUrl(ROOT_ORGANIZATION_ID),
        ROOT_ORGANIZATION_ID + PublicDataSourcesEditorPage.PUBLIC_DATA_SOURCES_URL_FRAGMENT);

    assertThat(editor.settingsTile()).isHidden();
    assertThat(editor.licenseErrorAlert()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.licenseErrorAlert()).containsText(LICENSE_ERROR_TEXT);
  }

  @Test
  @Category(RegressionTest.class)
  public void testSbomManagerInfoAlertVisibleWhenCpeMatchingEnabled() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.CPE_MATCHING);
    PublicDataSourcesEditorPage editor = new PublicDataSourcesEditorPage();

    playwrightRefresh();
    navigateAndWaitForUrl(
        PublicDataSourcesEditorPage.sbomManagerOrgUrl(ROOT_ORGANIZATION_ID),
        ROOT_ORGANIZATION_ID + PublicDataSourcesEditorPage.PUBLIC_DATA_SOURCES_URL_FRAGMENT);

    assertThat(editor.sbomManagerInfoAlert()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.sbomManagerInfoAlert()).containsText(SBOM_INFO_TEXT);
  }

  /** Submit button hidden via CSS {@code hidden} class when accessed via the {@code sbomManager.*} route. */
  @Test
  @Category(RegressionTest.class)
  public void testSubmitButtonHiddenWhenNavigatedViaSbomManagerRoute() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.CPE_MATCHING);
    PublicDataSourcesEditorPage editor = new PublicDataSourcesEditorPage();

    playwrightRefresh();
    navigateAndWaitForUrl(
        PublicDataSourcesEditorPage.sbomManagerOrgUrl(ROOT_ORGANIZATION_ID),
        ROOT_ORGANIZATION_ID + PublicDataSourcesEditorPage.PUBLIC_DATA_SOURCES_URL_FRAGMENT);

    assertThat(editor.settingsTile()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.submitButton()).isHidden();
  }
}
