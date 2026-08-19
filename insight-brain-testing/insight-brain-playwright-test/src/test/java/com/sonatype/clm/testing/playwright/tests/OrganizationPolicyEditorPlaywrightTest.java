/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePage;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Playwright regression tests for the PolicyEditor at organization scope. */
public class OrganizationPolicyEditorPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "Ye Ole Organization";

  private static final String POLICY_NAME = "Test Policy";

  private static final String INHERITANCE_POLICY_NAME = "Test Inheritance";

  private static final int POLICY_THREAT_LEVEL = 5;

  private static final String CREATED_POLICY_NAME = "Test Critical CVE Policy";

  private static final int CREATED_POLICY_THREAT_LEVEL = 10;

  private static final String CREATED_POLICY_THREAT_LABEL = "Critical";

  private static final String CREATED_POLICY_CONSTRAINT_NAME = "Critical CVE constraint";

  private static final int CREATED_POLICY_AGE_IN_DAYS = 30;

  private static final String NEW_POLICY_HEADING = "Policy Settings";

  private static final String UPDATED_POLICY_SUFFIX = " Updated";

  private static final String DELETE_WRONG_INPUT = "WRONG";

  private static final String DELETE_CONFIRMATION_KEYWORD = "DELETE";

  private static final String DELETE_VALIDATION_MESSAGE = "Must type DELETE to confirm";

  private Organization organization;

  @BeforeEach
  public void seedOrgAndOpenAsAdmin() {
    String orgName = ORG_NAME_PREFIX + "-" + TemporaryEntity.uuid();
    organization = tempEntity.newOrganization(orgName);

    navigateAndWaitForUrl(OwnerSummaryPage.url(organization), OwnerSummaryPage.ORG_URL_FRAGMENT);
    playwrightLogin();
  }

  @Test
  @Tag("sanity")
  public void testPolicyEditorLoads() {
    // Given: an existing policy on the seeded organization.
    Policy policy = tempEntity.newPolicy(organization.getId(), POLICY_NAME, POLICY_THREAT_LEVEL);

    // When: the editor URL for that policy is opened.
    navigateAndWaitForUrl(PolicyEditorPage.url(organization, policy), PolicyEditorPage.EDIT_URL_FRAGMENT);

    // Then: the editor renders and pre-fills the policy name.
    PolicyEditorPage editorPage = new PolicyEditorPage();
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.policyName()).hasValue(POLICY_NAME);
  }

  @Test
  @Tag("sanity")
  public void testNewPolicyCreation() {
    // Given/When: the new-policy editor URL for the seeded org is opened.
    navigateAndWaitForUrl(PolicyEditorPage.newPolicyUrl(organization), PolicyEditorPage.EDIT_URL_FRAGMENT);

    // Then: the editor renders empty with the threat-level dropdown ready for input.
    PolicyEditorPage editorPage = new PolicyEditorPage();
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.policyName()).isEmpty();
    assertThat(editorPage.threatLevelDropdown()).isVisible();
  }

  @Test
  @Tag("sanity")
  public void testInheritanceSection() {
    // Given: an existing policy on the seeded organization. The inheritance section is always
    // rendered for organization-owned policies (see EditPolicyInheritance.jsx).
    Policy policy =
        tempEntity.newPolicy(organization.getId(), INHERITANCE_POLICY_NAME, POLICY_THREAT_LEVEL);
    // When: the editor URL is opened.
    navigateAndWaitForUrl(PolicyEditorPage.url(organization, policy), PolicyEditorPage.EDIT_URL_FRAGMENT);

    // Then: both the editor container and the inheritance section render.
    PolicyEditorPage editorPage = new PolicyEditorPage();
    LocatorAssertions.IsVisibleOptions visibleTimeout =
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);
    assertThat(editorPage.container()).isVisible(visibleTimeout);
    assertThat(editorPage.inheritanceSection()).isVisible(visibleTimeout);
  }

  /** End-to-end creation: fill name + threat + default constraint, submit, verify policy appears in owner summary. */
  @Test
  @Tag("sanity")
  public void testCreatePolicy_endToEndFromOwnerSummary() {
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage editorPage = new PolicyEditorPage();

    // Given: the user is on the owner summary and clicks "Add a Policy" on the Policies tile.
    // waitForNewPolicyFormReady() waits for network-idle + firstConstraintName, ensuring the
    // Redux policy + constraint slices finish loading before fills (prevents currentPolicy
    // being re-seeded and blowing away typed values).
    ownerSummary.openPoliciesSectionFromNavPills();
    ownerSummary.addPolicyButton().click();
    assertThat(editorPage.firstConstraintName()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    // Then: the new-policy form renders with all four editor sections (Summary, Inheritance,
    // Constraints, Actions, Notifications) and the heading reads "Policy Settings". Wait for the
    // first constraint's name input to become visible — that signals the constraint slice has
    // finished loading (loadConstraint resolves) and the SPA won't blow away our fills.
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.pageHeading()).hasText(NEW_POLICY_HEADING);
    assertThat(editorPage.policyName()).isEmpty();
    assertThat(editorPage.threatLevelDropdown()).isVisible();
    assertThat(editorPage.inheritanceSection()).isVisible();
    assertThat(editorPage.constraintsSection()).isVisible();
    assertThat(editorPage.actionsSection()).isVisible();
    assertThat(editorPage.notificationsSection()).isVisible();
    assertThat(editorPage.firstConstraintName()).isVisible();

    // When: the user fills the policy name + threat level + the default constraint (the form
    // ships with one empty AgeInDays constraint that must be valid before NxStatefulForm allows
    // submit) and clicks Create.
    editorPage.policyName().fill(CREATED_POLICY_NAME);
    editorPage.selectThreatLevel(CREATED_POLICY_THREAT_LEVEL, CREATED_POLICY_THREAT_LABEL);
    editorPage.fillDefaultConstraint(CREATED_POLICY_CONSTRAINT_NAME, CREATED_POLICY_AGE_IN_DAYS);
    editorPage.clickSubmit();
    waitForSubmitMask();

    // Then: after refreshing the owner summary, the new policy appears in the policies tile.
    // Re-open explicitly because Create may leave the editor on /policy or redirect to /policy/<id>
    // depending on build state — the list is the user-visible proof of persistence.
    playwrightRefreshOrOpen(OwnerSummaryPage.url(organization));
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTileRowByName(CREATED_POLICY_NAME)).isVisible();
  }

  /** Validation: empty name and duplicate name both show a form error without creating a policy. */
  @Test
  @Tag("sanity")
  public void testNewPolicyValidation_missingNameAndDuplicateName() {
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage editorPage = new PolicyEditorPage();

    // Given: the user opens the new-policy editor for the seeded organization. Wait for the
    // first constraint name to render so subsequent fills are not raced by the SPA's async
    // policy-slice / constraint-slice initialization.
    playwrightRefreshOrOpen(PolicyEditorPage.newPolicyUrl(organization));
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.firstConstraintName()).isVisible();

    // Case 1 — When: everything is filled except the policy name, and the user clicks Create.
    editorPage.selectThreatLevel(CREATED_POLICY_THREAT_LEVEL, CREATED_POLICY_THREAT_LABEL);
    editorPage.fillDefaultConstraint(CREATED_POLICY_CONSTRAINT_NAME, CREATED_POLICY_AGE_IN_DAYS);
    editorPage.clickSubmit();
    waitForSubmitMask();

    // Case 1 — Then: the URL stays on /policy and the page surfaces a "policy name is required"
    // error. NxStatefulForm in this build routes the 400 response through its loadError slot
    // rather than its submitError slot, so we assert on visible text rather than a specific
    // error-element class.
    assertThat(page).hasURL(Pattern.compile(".*/policy$"));
    assertThat(page.getByText("The policy name is required").first()).isVisible();
    playwrightRefreshOrOpen(OwnerSummaryPage.url(organization));
    // Filled fields (threat level + constraint) create "unsaved changes". The SPA router shows
    // an "Unsaved Changes" modal when navigating away; dismiss it so the owner summary renders.
    new UnsavedChangesModalComponent().dismissIfAppearsWithin(PlaywrightTiming.BRIEF_UI_TRANSITION_MS);
    // playwrightRefreshOrOpen only waits for the document LOAD state; the SPA still needs to
    // hydrate the owner-summary route (NavPills.jsx) before the nav-pill testid is in the DOM.
    // Without this gate the subsequent click flakes with a 30 s timeout on cold backends.
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTile().getByText("No local policies defined")).isVisible();

    // Case 2 — Given: a duplicate-name seed exists on the same organization.
    String duplicateName = "Dup Policy " + TemporaryEntity.uuid();
    tempEntity.newPolicy(organization.getId(), duplicateName, POLICY_THREAT_LEVEL);
    // Case 2 — When: the empty-name rejection from case 1 leaves NxStatefulForm in a load-error
    // state (the form children are replaced by an error+Retry banner). Reload the editor URL to
    // get a clean form, then fill name + threat + constraint and submit.
    playwrightRefreshOrOpen(PolicyEditorPage.newPolicyUrl(organization));
    assertThat(editorPage.firstConstraintName()).isVisible();
    editorPage.policyName().fill(duplicateName);
    editorPage.selectThreatLevel(CREATED_POLICY_THREAT_LEVEL, CREATED_POLICY_THREAT_LABEL);
    editorPage.fillDefaultConstraint(CREATED_POLICY_CONSTRAINT_NAME, CREATED_POLICY_AGE_IN_DAYS);
    editorPage.clickSubmit();
    waitForSubmitMask();

    assertThat(page).hasURL(Pattern.compile(".*/policy$"));
    assertThat(editorPage.validationErrors()).isVisible();
    playwrightRefreshOrOpen(OwnerSummaryPage.url(organization));
    new UnsavedChangesModalComponent().dismissIfAppearsWithin(PlaywrightTiming.BRIEF_UI_TRANSITION_MS);
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTileRowByName(duplicateName)).hasCount(1);
  }

  @Test
  @Tag("regression")
  public void testCreatePolicy_submitMaskSuccessAndPoliciesTileUpdate() {
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage editorPage = new PolicyEditorPage();

    ownerSummary.openPoliciesSectionFromNavPills();
    ownerSummary.addPolicyButton().click();
    editorPage.waitForNewPolicyFormReady();

    editorPage.policyName().fill(CREATED_POLICY_NAME);
    editorPage.selectThreatLevel(CREATED_POLICY_THREAT_LEVEL, CREATED_POLICY_THREAT_LABEL);
    editorPage.fillDefaultConstraint(CREATED_POLICY_CONSTRAINT_NAME, CREATED_POLICY_AGE_IN_DAYS);
    editorPage.clickSubmit();

    new PolicyEditorPageAssertions(editorPage).shouldShowSaveSuccessMask();
    navigateAndWaitForUrl(OwnerSummaryPage.url(organization), OwnerSummaryPage.ORG_URL_FRAGMENT);
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTileRowByName(CREATED_POLICY_NAME)).isVisible();
  }

  @Test
  @Tag("regression")
  public void testEditPolicy_updateFlowFromPoliciesTile() {
    OwnersTreePage treePage = new OwnersTreePage();
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage editorPage = new PolicyEditorPage();

    tempEntity.newPolicy(organization.getId(), POLICY_NAME, POLICY_THREAT_LEVEL);
    navigateAndWaitForUrl(OwnersTreePage.url(), OwnersTreePage.TREE_URL_FRAGMENT);
    new OwnersTreePageAssertions(treePage).shouldBeVisibleWithAtLeastOneItem();
    treePage.clickItemWithText(organization.getName());
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openPoliciesSectionFromNavPills();
    ownerSummary.policiesTileRowByName(POLICY_NAME).click();
    new PolicyEditorPageAssertions(editorPage).shouldBeInEditModeWithExpectedName(POLICY_NAME);

    String updatedName = POLICY_NAME + UPDATED_POLICY_SUFFIX;
    editorPage.policyName().fill(updatedName);
    editorPage.clickSubmit();

    new PolicyEditorPageAssertions(editorPage).shouldShowSaveSuccessMask();
    assertThat(editorPage.policyName()).hasValue(updatedName);
  }

  @Test
  @Tag("regression")
  public void testDeletePolicy_cancelAndConfirmFlow() {
    PolicyEditorPage editorPage = new PolicyEditorPage();

    Policy policy = tempEntity.newPolicy(organization.getId(), POLICY_NAME, POLICY_THREAT_LEVEL);
    navigateAndWaitForUrl(PolicyEditorPage.url(organization, policy), PolicyEditorPage.EDIT_URL_FRAGMENT);
    assertThat(editorPage.container()).isVisible();

    editorPage.deletePolicyButton().click();
    new PolicyEditorPageAssertions(editorPage).shouldShowDeleteModal();
    editorPage.deleteModalInput().fill(DELETE_WRONG_INPUT);
    assertThat(editorPage.deleteModalValidation()).hasText(DELETE_VALIDATION_MESSAGE);
    editorPage.cancelDeleteAndWaitForModalClose();

    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.deletePolicyButton()).isVisible();

    editorPage.deletePolicyButton().click();
    new PolicyEditorPageAssertions(editorPage).shouldShowDeleteModal();
    editorPage.deleteModalInput().fill(DELETE_CONFIRMATION_KEYWORD);
    assertThat(editorPage.deleteModalValidation()).isHidden();
    editorPage.confirmDeleteAndWaitForModalClose();

    navigateAndWaitForUrl(OwnerSummaryPage.url(organization), OwnerSummaryPage.ORG_URL_FRAGMENT);
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTileRowByName(POLICY_NAME)).hasCount(0);
  }

  /** Enterprise gate: without CUSTOM_POLICIES license, Custom mode enters preview and save is blocked. */
  @Test
  @Tag("regression")
  public void testEnterpriseFeatureGate_defaultCustomToggleAndSaveBlocked() {
    PolicyEditorPage editorPage = new PolicyEditorPage();

    Policy policy = tempEntity.newPolicy(organization.getId(), POLICY_NAME, POLICY_THREAT_LEVEL);
    setMissingFeature(LicensedFeature.CUSTOM_POLICIES);
    playwrightRefreshOrOpen(PolicyEditorPage.url(organization, policy));
    // The reload is intentional: installLicense() updates the server-side license state, but the
    // SPA fetches product features asynchronously on page load. In headless Chromium the first load
    // completes before the features response is processed, so the enterprise toggle does not render.
    // A second load guarantees the SPA re-fetches and reflects the updated license state.
    playwrightRefresh();

    assertThat(editorPage.defaultModeButton()).isVisible();
    assertThat(editorPage.customModeButton()).isVisible();
    assertThat(editorPage.customModeButtonLockIcon()).isVisible();
    assertThat(editorPage.enterprisePreviewAlert()).isHidden();

    editorPage.customModeButton().hover();
    assertThat(editorPage.enterpriseFeatureTooltip()).isVisible();

    editorPage.customModeButton().click();

    assertThat(editorPage.enterprisePreviewAlert()).isVisible();
    assertThat(editorPage.saveButton()).isHidden();
  }

  @Test
  @Tag("regression")
  public void testSbomManagerPolicy_noDeleteButtonAndInfoAlert() {
    PolicyEditorPage editorPage = new PolicyEditorPage();

    Policy policy = tempEntity.newPolicy(organization.getId(), POLICY_NAME, POLICY_THREAT_LEVEL);
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.POLICY_MONITORING);
    playwrightHardresetToBlank();

    playwrightRefreshOrOpen(OwnerSummaryPage.sbomManagerUrl(organization.getId()));
    playwrightLogin();
    playwrightWaitUntilUrlContains(OwnerSummaryPage.SBOM_ORG_URL_FRAGMENT);
    String sbomPolicyUrl = PolicyEditorPage.sbomManagerUrl(organization, policy);
    playwrightSpaNavigateToHashFragment(sbomPolicyUrl.substring(sbomPolicyUrl.indexOf('#')));
    playwrightWaitUntilUrlContains(PolicyEditorPage.SBOM_MANAGER_EDIT_URL_FRAGMENT);
    assertThat(editorPage.container()).isVisible();

    new PolicyEditorPageAssertions(editorPage).shouldBeInSbomManagerReadOnlyMode();
  }

  /** Back button on new-policy form navigates back to the owner summary without creating a policy. */
  @Test
  @Tag("regression")
  public void testBackButtonOnNewPolicy_returnsToOwnerSummary() {
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage editorPage = new PolicyEditorPage();

    ownerSummary.openPoliciesSectionFromNavPills();
    ownerSummary.addPolicyButton().click();
    editorPage.waitForNewPolicyFormReady();

    editorPage.backButton().click();

    assertThat(ownerSummary.container()).isVisible();
  }
}
