/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponent;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.api.admin.service.TenantProvisioningService;
import com.sonatype.insight.brain.product.TestProductLicenseRule;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqTenantLifecyclePlaywrightTest
    extends AbstractMtiqUiTest
{
  private String deletedTenantSlug;

  @After
  public void cleanupDeletedTenantRecord() {
    if (deletedTenantSlug != null) {
      cleanupDeletedTenant(deletedTenantSlug);
    }
  }

  @Test
  public void testMtiqTenantLifecycle_provisionedTenant_rootOrgSummaryAndChildOrgVisible() {
    Organization childOrg = tempEntity.newOrganization();
    playwrightLoginAdminAt(OwnerSummaryPage.urlToRootOrg());
    new OwnerSummaryPageAssertions(new OwnerSummaryPage()).shouldBeVisible();
    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    assertThat(
        sidebar.organizationLinks()
            .filter(new Locator.FilterOptions().setHasText(childOrg.getName())))
                .isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  @Test
  public void testMtiqTenantLifecycle_deleteTenant_createsDeletedTenantRecord() {
    playwrightLoginAdminAt(DashboardPage.url());
    deletedTenantSlug = tenantUtil.getTenantSlug();
    lookup(TenantProvisioningService.class).markTenantForDeletion(deletedTenantSlug);
    Assertions.assertThat(lookup(DeletedTenantDAO.class).getTenantBySlug(deletedTenantSlug)).isNotNull();
  }

  @Test
  public void testMtiqTenantLifecycle_licenseUpdate_addsFirewallToSolutionSwitcher() {
    // Start Lifecycle-only. SolutionResolver reads both setFeatures and setLicensedProducts;
    // both must be updated together to control solution-switcher entries.
    setFeatures(
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.DASHBOARD,
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.POLICY_WAIVERS);
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(DashboardPage.url());

    // Add Firewall — mirrors a license-update event.
    setFeatures(
        LicensedFeature.FIREWALL,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.DASHBOARD,
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.POLICY_WAIVERS);
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
        ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    // Reload so the SPA re-fetches productFeatures with the updated TenantReference.
    playwrightRefresh();

    HeaderComponent header = new HeaderComponent();
    header.solutionSwitcherToggle().click();
    assertThat(header.solutionSwitcherLink("Repository Firewall")).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  @Test
  public void testMtiqTenantLifecycle_licenseCache_eachTenantSeesOnlyOwnFeatures() {
    // Set Tenant A's license WITH_FIREWALL before first login. bootTenant(A) on login reads the
    // MockKey and writes it to TenantReference[A]; the key state at boot is decisive.
    setFeatures(
        LicensedFeature.FIREWALL,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.DASHBOARD,
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.POLICY_WAIVERS);
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
        ProductLicenseDetails.PRODUCT_FIREWALL_V2);

    String tenantASlug = tenantUtil.getTenantSlug();
    String tenantBSlug = "tenant-b-" + TemporaryEntity.uuid();
    try {
      provisionSecondaryTenant(tenantBSlug);
      // Seed B's license so bootTenant(B) succeeds; without it loadLicense() throws 402.
      TenantTestHelper.testAsTenantAndInvalidate(tenantBSlug,
          t -> new TestProductLicenseRule(multiTenantDatabaseContainerRule).insertLicenseIfNeeded());
      playwrightLoginAdminAt(DashboardPage.url());
      HeaderComponent headerA = new HeaderComponent();
      headerA.solutionSwitcherToggle().click();
      assertThat(headerA.solutionSwitcherLink("Repository Firewall")).isVisible(PlaywrightTiming.VISIBLE_OPTS);

      playwrightLogout();
      // Update MockKey to NO_FIREWALL before Tenant B's first login. TenantReference[A] is already
      // written — changing the MockKey only affects future loadLicense() calls.
      setLicensedProducts(
          ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
          ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
          ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
      setFeatures(
          LicensedFeature.POLICY_MANAGEMENT,
          LicensedFeature.POLICY_READ_ONLY,
          LicensedFeature.COMPONENT_EVALUATION,
          LicensedFeature.DASHBOARD,
          LicensedFeature.WAIVERS_DASHBOARD,
          LicensedFeature.POLICY_WAIVERS);
      tenantUtil.setTenantSlug(tenantBSlug);
      playwrightLoginAdminAt(DashboardPage.url());
      playwrightRefresh();

      HeaderComponent headerB = new HeaderComponent();
      headerB.solutionSwitcherToggle().click();
      // Scope to "My Sonatype Solutions" — unlicensed products also appear in an "Explore" section.
      Locator mySolutionsSection = page.locator("#iq-solution-switcher")
          .getByRole(AriaRole.REGION, new Locator.GetByRoleOptions().setName("My Sonatype Solutions"));
      assertThat(mySolutionsSection).isVisible(PlaywrightTiming.VISIBLE_OPTS);
      assertThat(mySolutionsSection.getByRole(AriaRole.LINK,
          new Locator.GetByRoleOptions().setName("Repository Firewall").setExact(true)))
              .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    }
    finally {
      // Restore Tenant A so afterTest() cleanup runs on the correct tenant.
      tenantUtil.setTenantSlug(tenantASlug);
      deleteSecondaryTenant(tenantBSlug);
    }
  }

  @Test
  public void testMtiqTenantLifecycle_crossTenantIsolation_tenantBOrgNotAccessibleFromTenantA() {
    // tenantUtil stays set to Tenant A — browser/HTTP requests always route to Tenant A.
    String tenantBSlug = "tenant-b-" + TemporaryEntity.uuid();
    try {
      provisionSecondaryTenant(tenantBSlug);
      String[] tenantBOrgId = new String[1];
      TenantTestHelper.testAsNewTenant(tenantBSlug, tenant -> {
        Organization tenantBOrg = new Organization("TenantBOrg-" + TemporaryEntity.uuid());
        lookup(OrganizationDAO.class).insert(tenantBOrg);
        tenantBOrgId[0] = tenantBOrg.getId();
      });

      // All requests resolve to Tenant A; the org ID is unknown to A, so the API returns 404.
      playwrightLoginAdminAt(OwnerSummaryPage.url(tenantBOrgId[0]));

      // Positive gate: page loaded and user is authenticated (header is interactive).
      assertThat(new HeaderComponent().userMenu()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
      // ownerName() is only rendered on a successful org load — its absence confirms isolation.
      assertThat(new OwnerSummaryPage().ownerName())
          .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    }
    finally {
      deleteSecondaryTenant(tenantBSlug);
    }
  }

  private void provisionSecondaryTenant(String slug) {
    TenantTestHelper.testAsNewTenant(slug,
        t -> lookup(TenantProvisioningService.class).provisionTenant(slug));
  }

  private void deleteSecondaryTenant(String slug) {
    // A→B transition is blocked by TenantThreadLocal; GLOBAL_TENANT allows the switch.
    TenantTestHelper.testAsTenant(Tenant.GLOBAL_TENANT,
        t -> lookup(TenantProvisioningService.class).markTenantForDeletion(slug));
    cleanupDeletedTenant(slug);
  }

  private void cleanupDeletedTenant(String slug) {
    DeletedTenantDAO dao = lookup(DeletedTenantDAO.class);
    DeletedTenant entity = dao.getTenantBySlug(slug);
    if (entity != null) {
      try (TransactionContext tx = dao.createTransactionContext()) {
        dao.delete(tx, entity);
      }
    }
  }
}
