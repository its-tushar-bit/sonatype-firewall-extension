/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PlaywrightUrlAssertions;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import org.assertj.core.api.Assertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertions companion for {@link OwnerSummaryPage}.
 */
public class OwnerSummaryPageAssertions
{
  private final OwnerSummaryPage page;

  public OwnerSummaryPageAssertions(OwnerSummaryPage page) {
    this.page = page;
  }

  public void shouldShowPoliciesTile() {
    Locator tile = page.policiesTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Policies")))
        .isVisible();
    assertThat(
        tile.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add a Policy")))
            .isVisible();
    assertThat(tile.locator("table").or(tile.locator(".nx-list"))).isVisible();
  }

  public void shouldShowInnerSourceRepositoryTile() {
    Locator tile = page.innerSourceRepositoryTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("InnerSource Repositories")))
        .isVisible();
    assertThat(tile.getByText(Pattern.compile("Configure repositories to identify InnerSource components")))
        .isVisible();
    assertThat(tile.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit"))).isVisible();
  }

  public void shouldShowAccessTile() {
    Locator tile = page.accessTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Access"))).isVisible();
    assertThat(tile.getByText(Pattern.compile(".+ users by role\\."))).isVisible();
    assertThat(tile.locator("#add-role-button")).isVisible();
    assertThat(tile.locator(".nx-tile-content").getByText(Pattern.compile("Local to .+"))).isVisible();
    assertThat(tile.locator("#iq-access-tile-local-access-list")).isVisible();
  }

  public void shouldShowPublicDataSourcesTile() {
    Locator tile = page.publicDataSourcesTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Public Data Sources")))
        .isVisible();
    Locator list = tile.locator("#public-data-sources");
    assertThat(list).isVisible();
    Locator summaryLink = list.getByRole(AriaRole.LINK);
    assertThat(summaryLink).isVisible();
    assertThat(summaryLink).hasAttribute("href", Pattern.compile(".+/publicDataSourcesEditor.*"));
  }

  public void shouldShowLegacyViolationsTile() {
    Locator tile = page.legacyViolationsTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Legacy Violations")))
        .isVisible();
    Locator list = tile.locator("#legacy-violations");
    assertThat(list).isVisible();
    Locator summaryLink = list.getByRole(AriaRole.LINK);
    assertThat(summaryLink).isVisible();
    assertThat(summaryLink).hasAttribute("href", Pattern.compile(".+/legacyViolations.*"));
  }

  public void shouldShowContinuousMonitoringTile() {
    Locator tile = page.continuousMonitoringTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Continuous monitoring")))
        .isVisible();
    Locator list = tile.locator("#continuous-monitoring");
    assertThat(list).isVisible();
    Locator summaryLink = list.getByRole(AriaRole.LINK);
    assertThat(summaryLink).isVisible();
    assertThat(summaryLink).hasAttribute("href", Pattern.compile(".+/monitoring.*"));
  }

  public void shouldShowProprietaryComponentsTile() {
    Locator tile = page.proprietaryComponentsTile();
    assertThat(tile).isVisible();
    assertThat(
        tile.getByRole(AriaRole.HEADING,
            new Locator.GetByRoleOptions().setName("Proprietary Component Configuration")))
                .isVisible();
    Locator matchers = tile.locator("#proprietary-component-matchers");
    assertThat(matchers).isVisible();
    Locator summaryLink = matchers.getByRole(AriaRole.LINK);
    assertThat(summaryLink).isVisible();
    assertThat(summaryLink).hasAttribute("href", Pattern.compile(".+/proprietary.*"));
    assertThat(matchers).containsText("local");
  }

  public void shouldShowComponentLabelsTile() {
    Locator tile = page.componentLabelsTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Component Labels")))
        .isVisible();
    assertThat(tile.getByText(Pattern.compile("available to .+ policies")))
        .isVisible();
    assertThat(tile.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add a Label")))
        .isVisible();
    assertThat(tile.locator(".nx-tile-content").getByText(Pattern.compile("Local to .+"))).isVisible();
  }

  public void shouldShowLicenseThreatGroupsTile() {
    Locator tile = page.licenseThreatGroupsTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("License Threat Groups")))
        .isVisible();
    assertThat(tile.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add a Threat Group")))
        .isVisible();
    assertThat(tile.locator("#add-ltg-button")).isVisible();
    Locator tableRegion = tile.locator(".iq-ltg-summary-table");
    assertThat(tableRegion).isVisible();
    assertThat(tableRegion.getByText("THREAT", new Locator.GetByTextOptions().setExact(true))).isVisible();
    assertThat(tableRegion.getByText("NAME", new Locator.GetByTextOptions().setExact(true))).isVisible();
  }

  public void shouldShowApplicationCategoriesTile() {
    Locator tile = page.categoriesTile();
    assertThat(tile).isVisible();
    assertThat(
        tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Application Categories")))
            .isVisible();
    assertThat(tile.getByText(Pattern.compile("available to apps in .+")))
        .isVisible();
    assertThat(tile.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add a Category")))
        .isVisible();
  }

  public void shouldShowSourceControlTile() {
    Locator tile = page.sourceControlTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Source Control")))
        .isVisible();
    assertThat(tile.getByText(Pattern.compile("integration with an external SCM", Pattern.CASE_INSENSITIVE)))
        .isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Configuration")))
        .isVisible();
    Locator configurationLink = tile.locator(".nx-tile-content").getByRole(AriaRole.LINK);
    assertThat(configurationLink).isVisible();
    assertThat(configurationLink).hasAttribute("href", Pattern.compile(".+/source-control.*"));
  }

  public void shouldShowAutoWaiversTile() {
    Locator tile = page.autoWaiversTile();
    assertThat(tile).isVisible();
    assertThat(tile.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Auto-Waivers")))
        .isVisible();
    Locator list = tile.locator(".nx-list");
    assertThat(list).isVisible();
    Locator summaryLink = list.getByRole(AriaRole.LINK);
    assertThat(summaryLink).isVisible();
    assertThat(summaryLink).hasAttribute("href", Pattern.compile(".+/autowaivers.*"));
    assertThat(tile.getByText(Pattern.compile("\\d+\\s+local"))).isVisible();
  }

  public void shouldShowOrganizationActionsMenu() {
    Locator menu = page.ownerActionsMenu();
    assertThat(menu).isVisible();
    assertThat(menu.locator("#copy-org-id-link")).isVisible();
    assertThat(menu.locator("#copy-org-id-link")).containsText("Org ID to Clipboard");
    assertThat(menu.locator("#app-org-link")).isVisible();
    assertThat(menu.locator("#app-org-link")).containsText("Edit Org Name / Icon");
    assertThat(menu.locator("#import-policies-link")).isVisible();
    assertThat(menu.locator("#import-policies-link")).containsText("Import Policies");
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowLocalAccessRoleMember(String roleName, String memberDisplayName) {
    assertThat(page.accessTileLocalAccessList()).containsText(roleName);
    assertThat(page.accessTileLocalAccessList()).containsText(memberDisplayName);
  }

  public void shouldNotHaveLocalAccessRole(String roleName) {
    Locator list = page.accessTileLocalAccessList();
    assertThat(list)
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
    assertThat(list).not().containsText(roleName);
  }

  public void shouldShowRepositoryUrl(String expectedUrl) {
    assertThat(page.repositoryUrlBlock()).isVisible();
    assertThat(page.repositoryUrlBlock()).containsText(expectedUrl);
  }

  public void shouldShowChildOrganizationActionsMenu() {
    Locator menu = page.ownerActionsMenu();
    assertThat(menu).isVisible();
    assertThat(menu.locator("#delete-owner-link")).isVisible();
    assertThat(menu.locator("#owner-move-link")).isVisible();
    assertThat(menu.locator("#copy-org-id-link")).isVisible();
    assertThat(menu.locator("#app-org-link")).isVisible();
    assertThat(menu.locator("#import-policies-link")).isVisible();
  }

  public void shouldShowApplicationActionsMenu() {
    Locator menu = page.ownerActionsMenu();
    assertThat(menu).isVisible();
    assertThat(menu.locator("#select-contact-link")).isVisible();
    assertThat(menu.locator("#eval-file-link")).isVisible();
    assertThat(menu.locator("#change-app-id-link")).isVisible();
    assertThat(menu.locator("#delete-owner-link")).isVisible();
    assertThat(menu.locator("#owner-move-link")).isVisible();
  }

  public void shouldShowNewApplicationModal() {
    Locator modal = page.editOwnerModal();
    assertThat(modal).isVisible();
    assertThat(modal.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("New Application")))
        .isVisible();
  }

  public void shouldShowNewOrganizationModal() {
    Locator modal = page.editOwnerModal();
    assertThat(modal).isVisible();
    assertThat(modal.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("New Organization")))
        .isVisible();
  }

  public void shouldShowNewOwnerModalNameRequiredError() {
    assertThat(page.editOwnerModal().locator(".nx-form__validation-errors")).isVisible();
  }

  public void shouldShowCreateApplicationDuplicateIdError() {
    assertThat(page.editOwnerModal().locator("#editor-new-id .nx-text-input.invalid")).isVisible();
  }

  public void shouldShowEditOwnerModal() {
    Locator modal = page.editOwnerModal();
    assertThat(modal).isVisible();
    assertThat(modal.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Update"))).isVisible();
  }

  public void shouldShowOwnerName(String expectedName) {
    assertThat(page.ownerName()).containsText(expectedName);
  }

  public void shouldShowImportPoliciesMenuItemAsPreview() {
    Locator item = page.ownerActionsMenu().locator("#import-policies-link");
    assertThat(item).isVisible();
    assertThat(item).containsText("Preview");
  }

  public void shouldShowDeleteOwnerModal() {
    Locator modal = page.deleteOwnerModal();
    assertThat(modal).isVisible();
    assertThat(modal.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete"))).isVisible();
  }

  public void shouldShowMoveOwnerModal() {
    Locator modal = page.moveOwnerModal();
    assertThat(modal).isVisible();
    assertThat(modal.getByLabel("New Parent Organization")).isVisible();
  }

  public void shouldShowChangeAppIdModal() {
    Locator modal = page.changeAppIdModal();
    assertThat(modal).isVisible();
    assertThat(modal.locator("#editor-new-id input")).isVisible();
  }

  public void shouldShowChangeAppIdValidationError() {
    assertThat(page.changeAppIdModal().locator("#editor-new-id .nx-text-input.invalid")).isVisible();
  }

  public void shouldShowEvaluateFileModal() {
    Locator modal = page.evaluateFileModal();
    assertThat(modal).isVisible();
    assertThat(modal.locator("select")).isVisible();
    assertThat(modal.locator(".nx-file-upload__select-btn")).isVisible();
  }

  public void shouldShowEvaluationStatusModal() {
    assertThat(page.evaluationStatusModal())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
    assertThat(page.evaluationStatusModal().locator(".nx-progress-bar"))
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ASYNC_EVALUATION_TIMEOUT_MS));
  }

  public void shouldShowImportPolicyModal() {
    Locator modal = page.importPolicyModal();
    assertThat(modal).isVisible();
    assertThat(modal.locator(".nx-file-upload__select-btn")).isVisible();
    assertThat(modal.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Import"))).isVisible();
  }

  public void shouldShowImportPoliciesFileSelected(String filename) {
    assertThat(page.importPolicyModal().locator(".nx-selected-file__name")).containsText(filename);
  }

  public void shouldShowImportPoliciesPreviewModal() {
    Locator modal = page.importPolicyModal();
    assertThat(modal).isVisible();
    assertThat(modal.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close"))).isVisible();
    assertThat(modal.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Import")))
        .not()
        .isVisible();
  }

  public void shouldShowSelectContactModal() {
    Locator modal = page.selectContactModal();
    assertThat(modal).isVisible();
    assertThat(modal.locator(".nx-combobox input")).isVisible();
    assertThat(modal.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save"))).isVisible();
  }

  public void shouldShowContactName(String displayName) {
    assertThat(page.container()).containsText(displayName);
  }

  public void shouldShowInsufficientPermissionTree(String expectedDescription) {
    assertThat(page.insufficientPermissionTreeFilterInput()).isVisible();
    assertThat(page.insufficientPermissionTreeDescription()).containsText(expectedDescription);
  }

  public void shouldShowOnlySbomTiles() {
    assertThat(page.policiesTile()).isVisible();
    assertThat(page.continuousMonitoringTile()).isVisible();
    assertThat(page.accessTile()).isVisible();
    assertThat(page.sbomsTile()).not().isVisible();
    assertThat(page.legacyViolationsTile()).not().isVisible();
    assertThat(page.proprietaryComponentsTile()).not().isVisible();
    assertThat(page.componentLabelsTile()).not().isVisible();
    assertThat(page.licenseThreatGroupsTile()).not().isVisible();
    assertThat(page.sourceControlTile()).not().isVisible();
    assertThat(page.autoWaiversTile()).not().isVisible();
    assertThat(page.innerSourceRepositoryTile()).not().isVisible();
  }

  public void shouldShowOnlySbomAppTiles() {
    assertThat(page.policiesTile()).isVisible();
    assertThat(page.continuousMonitoringTile()).isVisible();
    assertThat(page.accessTile()).isVisible();
    assertThat(page.sbomsTile()).isVisible();
    assertThat(page.legacyViolationsTile()).not().isVisible();
    assertThat(page.proprietaryComponentsTile()).not().isVisible();
    assertThat(page.componentLabelsTile()).not().isVisible();
    assertThat(page.licenseThreatGroupsTile()).not().isVisible();
    assertThat(page.sourceControlTile()).not().isVisible();
    assertThat(page.autoWaiversTile()).not().isVisible();
    assertThat(page.innerSourceRepositoryTile()).not().isVisible();
  }

  /** Asserts navigation landed on a URL containing {@code urlFragment} (SPA hash routes). */
  public void shouldHaveUrlContaining(String urlFragment) {
    PlaywrightUrlAssertions.assertUrlContaining(page.playwrightPage(), urlFragment);
  }

  /** Asserts the system clipboard contains {@code expected} after a copy action. */
  public void shouldHaveClipboardText(String expected) {
    Assertions.assertThat(page.readClipboardText()).isEqualTo(expected);
  }
}
