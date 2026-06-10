/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

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
}
