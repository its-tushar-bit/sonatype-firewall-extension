/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.GettingStartedPage;
import com.sonatype.clm.testing.playwright.pages.GettingStartedPageAssertions;
import com.sonatype.clm.testing.playwright.pages.LdapPage;
import com.sonatype.clm.testing.playwright.pages.LdapPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ProductLicensePage;
import com.sonatype.clm.testing.playwright.pages.ProductLicensePageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

/**
 * Coverage for System Preferences: Product License, EULA, and LDAP screens.
 */
public class ProductLicenseAndLdapPlaywrightTest
    extends AbstractIqUiTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private static final String LDAP_SERVER_NAME_PREFIX = "pw-ldap-server";

  private ProductLicensePage licensePage;

  private ProductLicensePageAssertions licenseAssertions;

  private GettingStartedPage gettingStartedPage;

  private GettingStartedPageAssertions gettingStartedAssertions;

  private LdapPage ldapPage;

  private LdapPageAssertions ldapAssertions;

  private String ldapServerId;

  @Before
  public void setUp() {
    LdapServer server = tempEntity.newLdapServer(LDAP_SERVER_NAME_PREFIX + TemporaryEntity.uuid());
    tempEntity.newLdapConnection(server.getId());
    tempEntity.newLdapUserMapping(server.getId());
    ldapServerId = server.getId();

    playwrightRefreshOrOpen(ProductLicensePage.url());
    playwrightLogin();

    licensePage = new ProductLicensePage();
    licenseAssertions = new ProductLicensePageAssertions(licensePage);
    gettingStartedPage = new GettingStartedPage();
    gettingStartedAssertions = new GettingStartedPageAssertions(gettingStartedPage);
    ldapPage = new LdapPage();
    ldapAssertions = new LdapPageAssertions(ldapPage);
  }

  // Split into two independent @After methods so a failure in one does not suppress the other —
  // JUnit 4 runs each @After regardless of the other's outcome.

  @After
  public void restoreLicenseState() {
    // uninstallLicense() in any test leaks "no license" state into the shared JVM session,
    // breaking subsequent license-gated assertions. installLicense() is idempotent.
    installLicense();
  }

  @After
  public void clearLifecycleTier() {
    lookup(SystemConfigurationPropertyDAO.class).set(SystemConfigurationProperty.LIFECYCLE_TIER, null);
  }

  @Test
  @Category(RegressionTest.class)
  public void testProductLicensePage_renders() {
    licenseAssertions.shouldShowPageHeading();
    licenseAssertions.shouldShowLicenseDetails();
    licenseAssertions.shouldShowExpirationDate();
    licenseAssertions.shouldShowLicenseTypes();
    licenseAssertions.shouldShowInstallButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testProductLicense_eulaModalOnInstall() {
    licenseAssertions.shouldShowInstallButton();
    licensePage.licenseFileInput().setInputFiles(createTempLicenseFile());

    licenseAssertions.shouldShowEulaModal();
    licenseAssertions.shouldShowEulaHeading();
    licenseAssertions.shouldShowEulaAcceptButton();
    licenseAssertions.shouldShowEulaDeclineButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testProductLicensePage_showsProTierForProLifecycleLicense() {
    seedLifecycleTier("Pro");
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);

    playwrightRefreshOrOpen(ProductLicensePage.url());
    licenseAssertions.shouldShowLicenseTierWithText("Pro");
  }

  @Test
  @Category(RegressionTest.class)
  public void testProductLicensePage_showsEnterpriseTierForEnterpriseLifecycleLicense() {
    seedLifecycleTier("Enterprise");
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);

    playwrightRefreshOrOpen(ProductLicensePage.url());
    licenseAssertions.shouldShowLicenseTierWithText("Enterprise");
  }

  @Test
  @Category(RegressionTest.class)
  public void testProductLicensePage_omitsTierRowForLegacyLifecycleLicense() {
    // No LIFECYCLE_TIER set → productEdition is "Lifecycle" (legacy); the Tier row only
    // renders for "Lifecycle Pro" / "Lifecycle Enterprise".
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);

    playwrightRefreshOrOpen(ProductLicensePage.url());
    licenseAssertions.shouldShowPageHeading();
    licenseAssertions.shouldNotShowLicenseTier();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFreshInstall_unlicensedDashboardShowsLicenseRequiredError() {
    uninstallLicense();
    playwrightRefreshOrOpen(DashboardPage.url());
    // SPA's exact surface varies with bootstrap state — both regex branches handle it.
    PlaywrightAssertions.assertThat(page.locator("body"))
        .containsText(Pattern.compile("No valid product license installed|No product licenses to display"));
  }

  @Test
  @Category(RegressionTest.class)
  public void testFreshInstall_dismissingEulaModalKeepsServerUnlicensed() {
    uninstallLicense();
    playwrightRefreshOrOpen(ProductLicensePage.url());
    licenseAssertions.shouldShowInstallButton();

    licensePage.licenseFileInput().setInputFiles(createTempLicenseFile());
    licenseAssertions.shouldShowEulaModal();

    licensePage.eulaDeclineButton().click();
    PlaywrightAssertions.assertThat(licensePage.eulaModal()).isHidden();
    licenseAssertions.shouldShowInstallButton();
  }

  /**
   * Diverges from manual row 7 ("no EULA modal appears"): the modal opens client-side
   * regardless of file content; validation is server-side after Accept.
   */
  @Test
  @Category(RegressionTest.class)
  public void testFreshInstall_invalidLicenseFileShowsErrorAfterEulaAccept() {
    uninstallLicense();
    playwrightRefreshOrOpen(ProductLicensePage.url());

    licensePage.licenseFileInput().setInputFiles(createTempLicenseFile());
    licenseAssertions.shouldShowEulaModal();
    licensePage.eulaAcceptButton().click();

    PlaywrightAssertions.assertThat(licensePage.eulaModal()).isHidden();
    licenseAssertions.shouldShowInstallError();
    licenseAssertions.shouldShowInstallButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testGettingStartedPage_rendersAllAlwaysPresentSections() {
    navigateAndWaitForUrl(GettingStartedPage.url(), "/gettingStarted");
    gettingStartedPage.container().waitFor();
    gettingStartedAssertions.shouldShowAllAlwaysPresentSections();
  }

  @Test
  @Category(RegressionTest.class)
  public void testGettingStartedPage_productLicenseSummaryTileShowsLicenseDetails() {
    navigateAndWaitForUrl(GettingStartedPage.url(), "/gettingStarted");
    gettingStartedPage.container().waitFor();
    gettingStartedAssertions.shouldShowProductLicenseSummaryTile();
    gettingStartedAssertions.shouldShowLicenseSummaryDetails();
  }

  @Test
  @Category(RegressionTest.class)
  public void testProductLicensePage_uninstallReturnsToUnlicensedState() {
    playwrightRefreshOrOpen(ProductLicensePage.url());
    licensePage.uninstallLicenseButton().click();
    licensePage.uninstallConfirmSubmitButton().click();

    licenseAssertions.shouldShowInstallButton();

    playwrightRefreshOrOpen(DashboardPage.url());
    PlaywrightAssertions.assertThat(page.locator("body"))
        .containsText(Pattern.compile("No valid product license installed|No product licenses to display"));
  }

  private void seedLifecycleTier(String tier) {
    lookup(SystemConfigurationPropertyDAO.class).set(SystemConfigurationProperty.LIFECYCLE_TIER, tier);
  }

  private Path createTempLicenseFile() {
    try {
      Path tempFile = tempDir.newFile("test-license.lic").toPath();
      Files.writeString(tempFile, "dummy-license-content");
      return tempFile;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapServersList_renders() {
    playwrightRefreshOrOpen(LdapPage.listUrl());

    ldapAssertions.shouldShowListHeading();
    ldapAssertions.shouldShowTileHeading();
    ldapAssertions.shouldShowAddServerButton();
    ldapAssertions.shouldShowServerList();
    ldapAssertions.shouldHaveServerCount(1);
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapCreateServer_formRenders() {
    playwrightRefreshOrOpen(LdapPage.createUrl());

    ldapAssertions.shouldShowCreateContainer();
    ldapAssertions.shouldShowCreateHeading();
    ldapAssertions.shouldShowCreateForm();
    ldapAssertions.shouldShowServerNameInput();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapEditConnection_fieldsRender() {
    playwrightRefreshOrOpen(LdapPage.editConnectionUrl(ldapServerId));

    ldapAssertions.shouldShowEditorContainer();
    ldapAssertions.shouldShowEditorHeading();
    ldapAssertions.shouldShowServerNameInput();
    ldapAssertions.shouldShowHostnameInput();
    ldapAssertions.shouldShowPortInput();
    ldapAssertions.shouldShowSearchBaseInput();
    ldapAssertions.shouldShowProtocolSelector();
    ldapAssertions.shouldShowAuthMethodSelector();
    ldapAssertions.shouldShowTestConnectionButton();
    ldapAssertions.shouldShowRemoveServerButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapEditUserMapping_fieldsRender() {
    playwrightRefreshOrOpen(LdapPage.editUserMappingUrl(ldapServerId));

    ldapAssertions.shouldShowEditorContainer();
    ldapAssertions.shouldShowUserBaseDnInput();
    ldapAssertions.shouldShowUserObjectClassInput();
    ldapAssertions.shouldShowUserIdAttributeInput();
    ldapAssertions.shouldShowUserRealNameAttributeInput();
    ldapAssertions.shouldShowUserEmailAttributeInput();
    ldapAssertions.shouldShowGroupMappingTypeSelector();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapRemoveServer_modalRenders() {
    playwrightRefreshOrOpen(LdapPage.editConnectionUrl(ldapServerId));

    ldapAssertions.shouldShowRemoveServerButton();
    ldapPage.removeServerButton().click();

    ldapAssertions.shouldShowRemoveModal();
    ldapAssertions.shouldShowRemoveModalHeading();
    ldapAssertions.shouldShowRemoveModalWarning();
    ldapAssertions.shouldShowRemoveModalDeleteButton();
    ldapAssertions.shouldShowRemoveModalCancelButton();
  }
}
