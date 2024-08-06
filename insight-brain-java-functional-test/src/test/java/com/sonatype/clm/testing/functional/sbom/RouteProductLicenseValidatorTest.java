/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AdministratorsEditPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList.RoleRow;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.functional.pages.BaseUrlConfigurationPage;
import com.sonatype.clm.testing.functional.pages.EmailConfigurationPage;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;
import com.sonatype.clm.testing.functional.pages.ProxyConfigurationPage;
import com.sonatype.clm.testing.functional.pages.RoleManagementPage;
import com.sonatype.clm.testing.functional.pages.SamlConfigurationPage;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.clm.testing.functional.pages.SystemNoticeConfigurationPage;
import com.sonatype.clm.testing.functional.pages.UserManagementPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerDashboardPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;
import static com.sonatype.insight.brain.model.security.Role.POLICY_ADMIN_ROLE_ID;

public class RouteProductLicenseValidatorTest
    extends AbstractFunctionalTest
{
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void beforeEachMethod() {
    systemConfigurationPropertyDAO = lookup(SystemConfigurationPropertyDAO.class);
    systemConfigurationPropertyDAO.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "true"));

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen(IndexPage.url());
  }

  @Test
  public void testRouteProductLicenseValidator_nonPermittedPathRedirectsToSbomManagerDashboard() {
    final SbomManagerDashboardPage sbomManagerDashboardPage = new SbomManagerDashboardPage();
    Application application = tempEntity.newApplicationWithParent("test-app");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    waitUntilUrl(SbomManagerDashboardPage.url());

    sbomManagerDashboardPage.title()
        .shouldBe(visible)
        .shouldHave(text("SBOM Manager Dashboard"));
  }

  @Ignore("CLM-31101")
  @Test
  public void testRouteProductLicenseValidator_nonSbomOnlyPermittedPath_isAllowedWithNonSbomOnlyLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER, ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.SOURCE_CONTROL);
    Application application = tempEntity.newApplicationWithParent("test-app");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    SourceControlEditorPage.title()
        .shouldBe(visible)
        .shouldHave(text("Source Control Configuration"));
  }

  @Test
  public void testRouteProductLicenseValidator_alwaysPermittedPaths() {
    refreshOrOpen(GettingStartedPage.url());

    final GettingStartedPage gettingStartedPage = new GettingStartedPage();
    gettingStartedPage.productLicenseSummary().shouldBe(visible);

    refreshOrOpen(ProductLicensePage.url());
    ProductLicensePage.licensedSboms().shouldBe(visible);
    ProductLicensePage.products().shouldHave(texts("Sonatype SBOM Manager"));
  }

  @Test
  public void testRouteProductLicenseValidator_redirectsToSbomDashboard() {
    refreshOrOpen(IndexPage.url());
    waitUntilUrl(SbomManagerDashboardPage.url());
    final SbomManagerDashboardPage sbomManagerDashboardPage = new SbomManagerDashboardPage();
    sbomManagerDashboardPage.title().shouldBe(visible).shouldHave(text("SBOM Manager Dashboard"));
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_administratorsPageIsPermitted() {
    refreshOrOpen(AdministratorsPage.url());
    AdministratorsMappingList mapping = AdministratorsPage.administratorsMappingList();
    mapping.rows().shouldHave(size(2));

    RoleRow policyAdministratorRow = AdministratorsPage.administratorsMappingList().row(0);
    policyAdministratorRow.shouldBe(visible);
    policyAdministratorRow.click();

    waitUntilUrl(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    final AdministratorsEditPage administratorsEditPage = new AdministratorsEditPage();
    administratorsEditPage.roleDetails().shouldBe(visible);
    administratorsEditPage.roleDetails().name().shouldHave(text("Policy Administrator"));
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_advancedSearchPageIsPermitted() {
    refreshOrOpen(AdvancedSearchPage.sbomManagerUrl());
    final AdvancedSearchPage advancedSearchPage = new AdvancedSearchPage();
    advancedSearchPage.searchInput().shouldBe(empty);
    advancedSearchPage.resultCount().shouldBe(text("0"));
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_systemNoticeConfigurationPageIsPermitted() {
    refreshOrOpen(SystemNoticeConfigurationPage.url());
    final SystemNoticeConfigurationPage systemNoticeConfigurationPage = new SystemNoticeConfigurationPage();
    systemNoticeConfigurationPage.explanation().shouldBe(visible);
    systemNoticeConfigurationPage.explanation().shouldNotBe(empty);
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_baseUrlConfigurationPageIsPermitted() {
    refreshOrOpen(BaseUrlConfigurationPage.url());
    final BaseUrlConfigurationPage baseUrlConfigurationPage = new BaseUrlConfigurationPage();
    baseUrlConfigurationPage.baseUrlAttribute().shouldHave(value(""));
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_ldapServerListPageIsPermitted() {
    refreshOrOpen(LdapServerListPage.url());
    final LdapServerListPage ldapServerListPage = new LdapServerListPage();
    ldapServerListPage.shouldBe(visible);
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_samlConfigurationPageIsPermitted() {
    final SamlConfigurationPage samlConfigurationPage = new SamlConfigurationPage();
    refreshOrOpen(samlConfigurationPage.url());
    samlConfigurationPage.scrollToTop();
    samlConfigurationPage.identityProviderName().shouldBe(value("identity provider"));
    samlConfigurationPage.identityProviderMetadataXmlTextArea().shouldBe(empty);
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_proxyConfigurationPageIsPermitted() {
    refreshOrOpen(ProxyConfigurationPage.url());
    final ProxyConfigurationPage proxyConfigurationPage = new ProxyConfigurationPage();
    proxyConfigurationPage.hostName().shouldBe(empty);
    proxyConfigurationPage.port().shouldBe(empty);
    proxyConfigurationPage.username().shouldBe(empty);
    proxyConfigurationPage.password().shouldBe(empty);
    proxyConfigurationPage.excludeHosts().shouldBe(empty);
    proxyConfigurationPage.delete().shouldBe(disabled);
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_emailConfigurationPageIsPermitted() {
    refreshOrOpen(EmailConfigurationPage.url());
    final EmailConfigurationPage emailConfigurationPage = new EmailConfigurationPage();
    emailConfigurationPage.hostName().shouldBe(empty);
    emailConfigurationPage.port().shouldBe(empty);
    emailConfigurationPage.username().shouldBe(empty);
    emailConfigurationPage.password().shouldBe(empty);
    emailConfigurationPage.systemEmail().shouldBe(empty);
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_userManagementPageIsPermitted() {
    refreshOrOpen(UserManagementPage.url());
    final UserManagementPage userManagementPage = new UserManagementPage();
    userManagementPage.newUserButton().shouldBe(visible);
    userManagementPage.newUserForm().shouldBe(hidden);
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_roleManagementPageIsPermitted() {
    refreshOrOpen(RoleManagementPage.url());
    final RoleManagementPage roleManagementPage = new RoleManagementPage();
    roleManagementPage.componentTitle().shouldBe(visible).shouldHave(text("Configure Roles"));
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_sbomManagerDashboardPageIsPermitted() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    final SbomManagerDashboardPage sbomManagerDashboardPage = new SbomManagerDashboardPage();
    sbomManagerDashboardPage.title()
        .shouldBe(visible)
        .shouldHave(text("SBOM Manager Dashboard"));
  }
}
