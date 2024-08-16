/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar.OwnerItem;
import com.sonatype.clm.testing.functional.elements.OwnerSummaryTile;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerDashboardPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SbomManagerOwnerSummaryPageTest
    extends AbstractMtiqFunctionalTest
{
  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private Organization parentOrganization;

  private Organization childOrganization;

  private Application childApplication1;

  private Organization childOrganization2;

  private Application childApplication2;

  @Before
  public void init() {
    parentOrganization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    childOrganization = tempEntity.newOrganization("1st Child organization", parentOrganization);
    childOrganization2 = tempEntity.newOrganization("2nd Child organization", parentOrganization);
    childApplication1 = tempEntity.newApplicationWithParent(childOrganization);
    childApplication2 = tempEntity.newApplicationWithParent(childOrganization2);

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Test
  public void testNavigateToOrganizations() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    SidebarNavigation.sbomManagerOrganizationsNavigationButton().click();

    OwnerSummaryTile ownerSummaryTile = OwnerSummaryPage.summaryTile();
    ownerSummaryTile.shouldBe(visible);
    isSbomManagerPage();

    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();
    checkEntityVisibility(orgsAndPoliciesSidebar.getOrganizationLink(0), parentOrganization, 0, 2);
    checkEntityVisibility(orgsAndPoliciesSidebar.getOrganizationLink(0), childOrganization, 1, 0);
    checkEntityVisibility(orgsAndPoliciesSidebar.getApplicationLink(0), childApplication1, 1, 0);
    SidebarNavigation.sbomManagerOrganizationsNavigationButton().click();
    checkEntityVisibility(orgsAndPoliciesSidebar.getOrganizationLink(0), parentOrganization, 0, 2);
    checkEntityVisibility(orgsAndPoliciesSidebar.getOrganizationLink(1), childOrganization2, 1, 0);
    checkEntityVisibility(orgsAndPoliciesSidebar.getApplicationLink(0), childApplication2, 1, 0);
  }

  @Test
  public void testNavigateToOrganizations_sbomOnlyLicense_onlyOrgsAndAppsVisible() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    SidebarNavigation.sbomManagerOrganizationsNavigationButton().click();

    OwnerSummaryTile ownerSummaryTile = OwnerSummaryPage.summaryTile();
    ownerSummaryTile.shouldBe(visible);
    isSbomManagerPage();

    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();
    checkEntityVisibility(orgsAndPoliciesSidebar.getOrganizationLink(0), parentOrganization, 0, 2);
    checkEntityVisibility(orgsAndPoliciesSidebar.getOrganizationLink(0), childOrganization, 1, 0);
    checkEntityVisibility(orgsAndPoliciesSidebar.getApplicationLink(0), childApplication1, 1, 0);
    SidebarNavigation.sbomManagerOrganizationsNavigationButton().click();
    checkEntityVisibility(orgsAndPoliciesSidebar.getOrganizationLink(0), parentOrganization, 0, 2);
    checkEntityVisibility(orgsAndPoliciesSidebar.getOrganizationLink(1), childOrganization2, 1, 0);
    checkEntityVisibility(orgsAndPoliciesSidebar.getApplicationLink(0), childApplication2, 1, 0);

    orgsAndPoliciesSidebar.getRepositoryList().shouldNotBe(visible);
    orgsAndPoliciesSidebar.getRepoManagerList().shouldNotBe(visible);
  }

  @Test
  public void testSbomManager_ImportApplications_NotVisible() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    SidebarNavigation.sbomManagerOrganizationsNavigationButton().click();
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();
    orgsAndPoliciesSidebar.getOrganizationLink(0).click();
    orgsAndPoliciesSidebar.getApplicationPlusIcon().click();
    orgsAndPoliciesSidebar.getImportApplicationsButton().shouldNotBe(visible);
  }

  private void isSbomManagerPage() {
    SidebarNavigation.productLogo().shouldHave(attribute("alt", "sonatype sbom manager"));
  }

  private void checkEntityVisibility(
      OwnerItem owner,
      Owner ownerEntity,
      int applicationAmount,
      int organizationAmount)
  {
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();
    OwnerSummaryTile ownerSummaryTile = OwnerSummaryPage.summaryTile();
    owner.click();
    ownerSummaryTile.shouldBe(visible);
    ownerSummaryTile.name().shouldHave(text(ownerEntity.getName()));
    isSbomManagerPage();
    orgsAndPoliciesSidebar.getApplicationList().children().shouldHave(size(applicationAmount));
    orgsAndPoliciesSidebar.getOrganizationList().children().shouldHave(size(organizationAmount));
  }
}
