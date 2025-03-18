/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AdministratorsEditPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList.RoleRow;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.functional.pages.AttributionReportFormPage;
import com.sonatype.clm.testing.functional.pages.BaseUrlConfigurationPage;
import com.sonatype.clm.testing.functional.pages.EmailConfigurationPage;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.clm.testing.functional.pages.LegalApplicationDetailsPage;
import com.sonatype.clm.testing.functional.pages.LegalDashboardPage;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;
import com.sonatype.clm.testing.functional.pages.ProxyConfigurationPage;
import com.sonatype.clm.testing.functional.pages.RoleManagementPage;
import com.sonatype.clm.testing.functional.pages.SamlConfigurationPage;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.clm.testing.functional.pages.SystemNoticeConfigurationPage;
import com.sonatype.clm.testing.functional.pages.UserManagementPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerDashboardPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
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
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ALP_FOR_SBOM_MANAGER;
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

  @Test
  public void testRouteProductLicenseValidator_nonSbomOnlyPermittedPath_isAllowedWithNonSbomOnlyLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER, ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.SOURCE_CONTROL);
    Application application = tempEntity.newApplicationWithParent("test-app");

    refresh(); // Force page reload to ensure the app is fully reloaded
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
  public void testRouteProductLicenseValidator_sbomOnlyLicense_legalDashboardPageIsPermitted() {
    systemConfigurationPropertyDAO.set(ALP_FOR_SBOM_MANAGER, "true");
    refreshOrOpen(LegalDashboardPage.sbomManagerUrl());
    final LegalDashboardPage legalDashboardPage = new LegalDashboardPage();
    legalDashboardPage.componentsTab().click();
    legalDashboardPage.componentsTab().shouldHave(Condition.cssClass("active"));
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_legalApplicationDetailsPageIsPermitted() {
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.ADVANCED_LEGAL_PACK);
    systemConfigurationPropertyDAO.set(ALP_FOR_SBOM_MANAGER, "true");
    Application application = tempEntity.newApplicationWithParent("test-app");
    refreshOrOpen(LegalApplicationDetailsPage.sbomManagerUrlToApplicationScope(application.getPublicId()));

    final SelenideElement title = LegalApplicationDetailsPage.title();
    title.shouldHave(text(application.getName() + " Obligations"));

    SelenideElement filterContainer = LegalApplicationDetailsPage.filterContainer();
    filterContainer.shouldNotBe(visible);
    SelenideElement filterButton = LegalApplicationDetailsPage.filterButton();
    filterButton.shouldHave(exactText("Filter"));
    filterButton.click();
    filterContainer.shouldBe(visible);
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_legalComponentDetailsPageIsPermitted()
      throws IOException
  {
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.ADVANCED_LEGAL_PACK);
    systemConfigurationPropertyDAO.set(ALP_FOR_SBOM_MANAGER, "true");
    Application application = tempEntity.newApplicationWithParent("app");
    generateDataForSbomManagerALP(application);
    refreshOrOpen(LegalApplicationDetailsPage.sbomManagerUrlToApplicationScope(application.getPublicId()));

    final SelenideElement title = LegalApplicationDetailsPage.title();
    title.shouldHave(text(application.getName() + " Obligations"));
  }

  @Test
  public void testRouteProductLicenseValidator_sbomOnlyLicense_legalAttributionReportPageIsPermitted() {
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.ADVANCED_LEGAL_PACK);
    systemConfigurationPropertyDAO.set(ALP_FOR_SBOM_MANAGER, "true");
    Application application = tempEntity.newApplicationWithParent("test-app");
    refreshOrOpen(AttributionReportFormPage.sbomManagerUrl(application.getPublicId()));
    AttributionReportFormPage attributionReportFormPage = new AttributionReportFormPage();
    attributionReportFormPage.getFormSubmitBtn().click();
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(attributionReportFormPage.getTitleInput()));
    attributionReportFormPage.getTitleInput().shouldHave(Condition.value("Attribution Report"));
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

  private void generateDataForSbomManagerALP(Application app) throws IOException {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    testCLMServer.getHdsServer().respondWith("[]").atUri("/rest/license/metadata");
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

    String[] licenses = {
        "Apache-1.0", "MIT", "Apache-2.0", "Better-Cms-LA", "BSL-1.0", "CC-BY-NC-3.0", "CMRL-1.0",
        "GPL-2.0+-LGPL-3.0+", "GreenSock-Commercial-License", "Gridifier-Developer-LA",
        "Grammatica-BSD-3-Clause-Variant"
    };
    ComponentIdentifier[] componentIdentifiers = new ComponentIdentifier[licenses.length];

    String currentComponentName = "";

    for (int i = 1; i < 21; i++) {

      currentComponentName = (i == 1 || i == 12) ? "#$%&/" : "component";

      String stageType = i % 3 == 0 ? BuildStageType.ID : (i % 5 == 0 ? SourceStageType.ID : ReleaseStageType.ID);

      addComponentAndLicenses(apps[i % apps.length], "org.package", currentComponentName,
          (i % licenses.length + 1) + ".0", "hash" + (i % licenses.length + 1),
          stageType, licenses[i % licenses.length]);

      componentIdentifiers[i % licenses.length] = componentIdentifiers[i % licenses.length] != null ?
          componentIdentifiers[i % licenses.length] : ComponentIdentifier
          .createMavenCoordinates("org.package", currentComponentName, (i % licenses.length + 1) + ".0");

      tempEntity.newComponentObligation(
          componentIdentifiers[i % licenses.length], apps[i % apps.length].getId(),
          "Inclusion of Notice", "comment", ObligationStatus.FULFILLED, "hash" + i);
    }
  }

  private void addEvaluationPoliciesToApplications(Application[] apps) {
    for (Application application : apps) {
      tempEntity.newPolicyEvaluation(application.getId(), "build", "", new Date());
    }
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
        tempEntity.newApplicationComponent(application.getId(), stageTypeId, hash, componentIdentifier);
    Arrays.stream(licenseIds)
        .forEach(licenseId -> tempEntity.newApplicationComponentLicense(applicationComponent.getId(), licenseId));
  }

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(10))
        .pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }
}
