/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.microsoft.playwright.Locator;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqSbomPolicyEditorPlaywrightTest
    extends AbstractMtiqUiTest
{
  private static final int THREAT_LEVEL = MtiqSbomPolicyTestConstants.THREAT_LEVEL;

  private Organization rootOrg;

  private Organization childOrg;

  private Application application;

  private PolicyEditorPage editor;

  private PolicyEditorPageAssertions assertions;

  @Before
  public void seedOrgTreeAndLogin() {
    rootOrg = tempEntity.newOrganization("MTIQ Policy Root " + tempEntity.uuid());
    childOrg = tempEntity.newOrganization("MTIQ Policy Child " + tempEntity.uuid(), rootOrg);
    application = tempEntity.newApplicationWithParent(childOrg);

    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_FOUNDATION);
    playwrightRefreshOrOpen("/");
    playwrightLogin();

    editor = new PolicyEditorPage();
    assertions = new PolicyEditorPageAssertions(editor);
  }

  @Test
  public void testSbomPolicyEditor_rootOrgLocalPolicy_rendersEditable() {
    Policy policy = tempEntity.newPolicy(rootOrg.getId(), "Root Local Policy " + tempEntity.uuid(), THREAT_LEVEL);

    playwrightRefreshOrOpen(PolicyEditorPage.url(rootOrg, policy));

    assertions.shouldBeInEditModeWithExpectedName(policy.getName());
  }

  @Test
  public void testSbomPolicyEditor_applicationInheritedPolicy_readOnly() {
    Policy parentPolicy =
        tempEntity.newPolicy(childOrg.getId(), "App-Inherited Policy " + tempEntity.uuid(), THREAT_LEVEL);

    playwrightRefreshOrOpen(PolicyEditorPage.url(application, parentPolicy));

    assertions.shouldBeInheritedReadOnlyView();
  }

  /** Save-persists round-trip is CLM-42839 — app-scoped Save PUT does not round-trip. */
  @Test
  public void testSbomPolicyEditor_applicationLocalPolicy_rendersEditable() {
    Policy policy = tempEntity.newPolicy(application.getId(), "App Local Policy " + tempEntity.uuid(), THREAT_LEVEL);

    playwrightRefreshOrOpen(PolicyEditorPage.url(application, policy));

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.saveButton()).hasText("Update");
    assertThat(editor.constraintsSection()).isVisible();
    assertThat(editor.actionsSection()).isVisible();
    assertThat(editor.notificationsSection()).isVisible();

    Locator policyNameInput = editor.container().getByLabel("Policy Name");
    String renamed = policy.getName() + " (edited)";
    policyNameInput.fill(renamed);
    policyNameInput.press("Tab");
    assertThat(policyNameInput).hasValue(renamed);
    assertThat(editor.saveButton()).isEnabled();
  }

  @Test
  public void testSbomPolicyEditor_appScopedUnderSbomOnly_rendersInReadOnlyMode() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    // Editor gates on the SBOM_MANAGER feature; without it the app renders the upsell page.
    setFeatures(LicensedFeature.SBOM_MANAGER);
    Policy policy =
        tempEntity.newPolicy(application.getId(), "SBOM-only App Policy " + tempEntity.uuid(), THREAT_LEVEL);

    // Full reload clears the module-level licenseInfoPromise cached from @Before's LIFECYCLE session;
    // without it the sbomManager route gate sees the stale license and redirects to learnMore.
    page.reload();
    playwrightRefreshOrOpen(PolicyEditorPage.sbomManagerUrl(application, policy));

    assertions.shouldBeInSbomManagerReadOnlyMode();
  }

  /**
   * Without an SBOM Manager product in the license (Lifecycle-only from @Before), navigating to an
   * org-scoped policy URL in the SBOM Manager context redirects to the upsell page.
   * The sbomManager route gate checks selectHasSbomManagerLicense (product-based); Lifecycle-only
   * yields false, triggering the redirect.
   */
  @Test
  public void testSbomPolicyEditor_orgScopedWithoutSbomLicense_redirectsToLearnMore() {
    // No setLicensedProducts call — use @Before's Lifecycle + Foundation license.
    Policy policy = tempEntity.newPolicy(rootOrg.getId(), "Non-SBOM Org Policy " + tempEntity.uuid(), THREAT_LEVEL);

    // Full reload clears the module-level licenseInfoPromise so the route gate fetches a fresh
    // Lifecycle-only license, correctly making selectHasSbomManagerLicense = false.
    page.reload();
    playwrightRefreshOrOpen(PolicyEditorPage.sbomManagerUrl(rootOrg, policy));

    assertThat(editor.playwrightPage()).hasURL(Pattern.compile("#/sbomManager/learnMore"));
  }

  @Test
  public void testSbomPolicyEditor_orgScopedWithBothLicenses_editableInLifecycleContext() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
        ProductLicenseDetails.PRODUCT_FOUNDATION,
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    Policy policy =
        tempEntity.newPolicy(rootOrg.getId(), "Both-License Org Policy " + tempEntity.uuid(), THREAT_LEVEL);

    playwrightRefreshOrOpen(PolicyEditorPage.url(rootOrg, policy));

    assertions.shouldBeInEditModeWithExpectedName(policy.getName());
  }

  @Test
  public void testSbomPolicyEditor_appScopedWithBothLicenses_editableInLifecycleContext() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
        ProductLicenseDetails.PRODUCT_FOUNDATION,
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    Policy policy =
        tempEntity.newPolicy(application.getId(), "Both-License App Policy " + tempEntity.uuid(), THREAT_LEVEL);

    playwrightRefreshOrOpen(PolicyEditorPage.url(application, policy));

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.saveButton()).hasText("Update");
    assertThat(editor.constraintsSection()).isVisible();
  }
}
