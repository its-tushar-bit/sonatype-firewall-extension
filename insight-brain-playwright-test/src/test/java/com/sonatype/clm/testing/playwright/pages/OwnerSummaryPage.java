/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Owner Summary page (organization/application management).
 */
public class OwnerSummaryPage
    extends BasePage
{
  /** Owner summary horizontal nav / tile id — Legacy Violations ({@code LegacyViolationsTile.jsx}). */
  public static final String OWNER_PILL_LEGACY_VIOLATIONS = "owner-pill-legacy-violations";

  /** Continuous monitoring summary tile. */
  public static final String OWNER_PILL_CONTINUOUS_MONITORING = "owner-pill-continuous-monitoring";

  /** Proprietary Component Configuration tile. */
  public static final String OWNER_PILL_PROPRIETARY_COMPONENTS = "owner-pill-component-configuration";

  /** Component Labels tile. */
  public static final String OWNER_PILL_COMPONENT_LABELS = "owner-pill-comp-labels";

  /** License Threat Groups tile. */
  public static final String OWNER_PILL_LICENSE_THREAT_GROUPS = "owner-pill-ltgs";

  /** Source Control tile. */
  public static final String OWNER_PILL_SOURCE_CONTROL = "owner-pill-source-control";

  /** Auto-Waivers tile ({@code data-testid="iq-auto-waivers-tile"}). */
  public static final String OWNER_PILL_AUTO_WAIVERS = "owner-pill-auto-waivers-configuration";

  /** InnerSource Repositories tile ({@code InnerSourceRepositoryTile.jsx}). */
  public static final String OWNER_PILL_INNERSOURCE_REPOSITORY = "owner-pill-innersource-repository";

  /** Access tile ({@code AccessTile.jsx}, pill target {@code access-tile-pill-access}). */
  public static final String OWNER_PILL_ACCESS = "access-tile-pill-access";

  /** Public Data Sources tile ({@code data-testid="owner-pill-public-data-sources"}). */
  public static final String OWNER_PILL_PUBLIC_DATA_SOURCES = "owner-pill-public-data-sources";

  /** {@code NxDropdown} root for the owner-summary "Actions" menu ({@code ActionDropdown.jsx}). */
  public static final String OWNER_ACTIONS_DROPDOWN_ID = "iq-owner-actions-dropdown";

  public OwnerSummaryPage() {
    super();
  }

  public static String url(Owner owner) {
    String ownerId = owner.getType().equals(OwnerType.REPOSITORY) ? owner.getId() : owner.getPublicId();
    return url(owner.getType(), ownerId);
  }

  public static String url(OwnerType ownerType, String id) {
    return "/assets/index.html#/management/view/" + ownerType.name().toLowerCase() + "/" + id;
  }

  public static String url(String ownerId) {
    return "/assets/index.html#/management/view/organization/" + ownerId;
  }

  /** Organization edit route (left sidebar + edit subroutes like Access). */
  public static String editUrl(String ownerId) {
    return "/assets/index.html#/management/edit/organization/" + ownerId;
  }

  public static String applicationUrl(String appId) {
    return "/assets/index.html#/management/view/application/" + appId;
  }

  public Locator container() {
    return locator("#owner-summary");
  }

  public Locator ownerName() {
    return locator("#owner-summary .nx-h1");
  }

  /**
   * "Add a Policy" button on the Policies tile (PoliciesTile.jsx, id="add-policy-button").
   * Clicking it routes the SPA to the new-policy editor for the current owner.
   */
  public Locator addPolicyButton() {
    return locator("#add-policy-button");
  }

  public Locator policyList() {
    return locator("#policy-list");
  }

  public Locator policyListItems() {
    return locator("#policy-list .nx-list__item");
  }

  /**
   * Policies tile on organization/application owner summary ({@code PoliciesTile.jsx},
   * {@code data-testid="policies-tile"}).
   */
  public Locator policiesTile() {
    return byTestId("policies-tile");
  }

  /**
   * Horizontal nav pill that scrolls the owner summary to the Policies tile ({@code NavPills.jsx},
   * {@code data-testid="owner-pill-policy-button"}).
   */
  public void openPoliciesSectionFromNavPills() {
    openOwnerSummarySectionFromNavPills("owner-pill-policy");
  }

  /**
   * Clicks the nav pill whose {@code data-scroll} target is {@code pillTargetId} (see
   * {@code NavPills.jsx}); pill {@code id} is {@code {pillTargetId}-button}.
   */
  public void openOwnerSummarySectionFromNavPills(String pillTargetId) {
    // NavPills sets both data-testid and id to the same value. Use the id selector here
    // because data-testid attributes may be stripped in the production bundle, whereas the
    // id attribute is always preserved in the DOM.
    locator("#" + pillTargetId + "-button").click();
  }

  /**
   * The "Local to {ownerName}" tbody section of the Policies tile (PoliciesTable.jsx,
   * {@code className="iq-policy-table iq-policy-table-local-section"}). Note: when the owner has
   * no local policies, PoliciesTile.jsx renders an {@code NxList} "No local policies defined"
   * empty state instead of a {@code PoliciesTable}, so this locator's count is zero.
   */
  public Locator policiesTileLocalSection() {
    return policiesTile().locator("tbody.iq-policy-table-local-section");
  }

  /**
   * Inherited-policies tbody sections of the Policies tile (PoliciesTable.jsx,
   * {@code className="iq-policy-table iq-policy-table-inherited-section"}). One section per
   * ancestor org that has policies; section's {@code aria-label} is {@code "Inherited from
   * {ownerName}"}. The collection is empty when there are no inherited policies (e.g. when the
   * current owner is the root organization).
   */
  public Locator policiesTileInheritedSections() {
    return policiesTile().locator("tbody.iq-policy-table-inherited-section");
  }

  /**
   * The single inherited-policies section sourced from the given parent owner. Targets via
   * the section's {@code aria-label="Inherited from {parentOwnerName}"}.
   */
  public Locator policiesTileInheritedSectionFor(String parentOwnerName) {
    return policiesTile().locator(
        "tbody.iq-policy-table-inherited-section[aria-label=\"Inherited from " + parentOwnerName + "\"]");
  }

  /**
   * A clickable policy row in the Policies tile identified by its name cell text. NxTable.Row
   * forwards {@code clickAccessibleLabel="Edit {name} policy"} as the row's accessible label, so
   * we target it via {@code getByLabel} for a stable, semantic selector.
   */
  public Locator policiesTileRowByName(String policyName) {
    return policiesTile().getByLabel("Edit " + policyName + " policy");
  }

  public Locator legacyViolationsTile() {
    return locator("#owner-pill-legacy-violations");
  }

  public Locator continuousMonitoringTile() {
    return locator("#owner-pill-continuous-monitoring");
  }

  public Locator proprietaryComponentsTile() {
    return locator("#owner-pill-component-configuration");
  }

  public Locator componentLabelsTile() {
    return locator("#owner-pill-comp-labels");
  }

  public Locator licenseThreatGroupsTile() {
    return locator("#owner-pill-ltgs");
  }

  public Locator sourceControlTile() {
    return locator("#owner-pill-source-control");
  }

  public Locator autoWaiversTile() {
    return byTestId("iq-auto-waivers-tile");
  }

  public Locator innerSourceRepositoryTile() {
    return locator("#owner-pill-innersource-repository");
  }

  public Locator accessTile() {
    return locator("#access-tile-pill-access");
  }

  public Locator publicDataSourcesTile() {
    return byTestId("owner-pill-public-data-sources");
  }

  /** Owner-summary "Actions" {@code NxDropdown} container ({@code ActionDropdown.jsx}). */
  public Locator ownerActionsDropdown() {
    return locator("#" + OWNER_ACTIONS_DROPDOWN_ID);
  }

  /** Toggle button labelled "Actions" inside {@link #ownerActionsDropdown()}. */
  public Locator ownerActionsToggle() {
    return ownerActionsDropdown().locator("button.nx-dropdown__toggle");
  }

  /** Open menu container rendered after clicking the Actions toggle. */
  public Locator ownerActionsMenu() {
    return ownerActionsDropdown().locator(".nx-dropdown-menu");
  }

  public void openOwnerActionsDropdown() {
    ownerActionsToggle().click();
    assertThat(ownerActionsMenu()).isVisible();
  }

  public Locator addApplicationButton() {
    return locator("#create-application");
  }

  public Locator applicationList() {
    return locator("#application-list");
  }

  public Locator organizationList() {
    return locator("#organization-list");
  }

  /** Access tile "Add a Role" action button. */
  public Locator accessTileAddRoleButton() {
    return locator("#add-role-button");
  }

  public Locator accessTileLocalAccessList() {
    return locator("#iq-access-tile-local-access-list");
  }

  public static String urlToRootOrg() {
    return url(Organization.ROOT_ORGANIZATION_ID);
  }

  /**
   * Deep-link into the organization and policies edit shell (left sidebar + main content). Paths
   * match OrgsAndPolicies route config and the Owner Detail sidebar links.
   *
   * @param organizationId internal organization id (e.g. {@link Organization#ROOT_ORGANIZATION_ID})
   * @param pathSuffix path after the org id, starting with {@code /} (e.g. {@code "/legacyViolations"}).
   */
  public static String editOrganizationUrl(String organizationId, String pathSuffix) {
    return "/assets/index.html#/management/edit/organization/" + organizationId + pathSuffix;
  }
}
