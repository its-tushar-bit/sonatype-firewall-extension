/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.nio.file.Path;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;

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

  /** Application Categories tile ({@code id="owner-pill-app-categories"}). */
  public static final String OWNER_PILL_APP_CATEGORIES = "owner-pill-app-categories";

  /** Data Retention tile ({@code id="owner-pill-retention"}). */
  public static final String OWNER_PILL_RETENTION = "owner-pill-retention";

  /** Artifactory Repository tile ({@code id="owner-pill-artifactory-repository"}). */
  public static final String OWNER_PILL_ARTIFACTORY_REPOSITORY = "owner-pill-artifactory-repository";

  /** {@code NxDropdown} root for the owner-summary "Actions" menu ({@code ActionDropdown.jsx}). */
  public static final String OWNER_ACTIONS_DROPDOWN_ID = "iq-owner-actions-dropdown";

  /** URL fragment prefix for organization owner summary routes. */
  public static final String ORG_URL_FRAGMENT = "/management/view/organization/";

  /** URL fragment prefix for application owner summary routes. */
  public static final String APP_URL_FRAGMENT = "/management/view/application/";

  /** URL fragment prefix for SBOM Manager organization owner summary routes. */
  public static final String SBOM_ORG_URL_FRAGMENT = "/sbomManager/management/view/organization/";

  /** URL fragment for the root organization owner summary. */
  public static final String ROOT_ORG_URL_FRAGMENT = ORG_URL_FRAGMENT + Organization.ROOT_ORGANIZATION_ID;

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

  public static String urlToRootOrg() {
    return url(Organization.ROOT_ORGANIZATION_ID);
  }

  /** SBOM Manager product context route for an organization summary. */
  public static String sbomManagerUrl(String orgId) {
    return "/assets/index.html#/sbomManager/management/view/organization/" + orgId;
  }

  /** SBOM Manager product context route for an application summary. */
  public static String sbomManagerAppUrl(String appPublicId) {
    return "/assets/index.html#/sbomManager/management/view/application/" + appPublicId;
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

  /**
   * Deep-link into the application edit shell (left sidebar + main content).
   *
   * @param appPublicId public application id
   * @param pathSuffix path after the app id, starting with {@code /} (e.g. {@code "/legacyViolations"}).
   */
  public static String editApplicationUrl(String appPublicId, String pathSuffix) {
    return "/assets/index.html#/management/edit/application/" + appPublicId + pathSuffix;
  }

  /**
   * Deep-link into the repository-manager container edit shell. The container is a singleton whose
   * owner id is the literal {@code REPOSITORY_CONTAINER_ID}.
   *
   * @param pathSuffix path after the owner id, starting with {@code /} (e.g. {@code "/access"}).
   */
  public static String editRepositoryContainerUrl(String pathSuffix) {
    return "/assets/index.html#/management/edit/repository_container/REPOSITORY_CONTAINER_ID" + pathSuffix;
  }

  /** SBOM Manager owner-summary route. */
  public static String sbomUrl(String ownerId) {
    return "/assets/index.html#/sbomManager/management/view/organization/" + ownerId;
  }

  /** Firewall owner-summary route. */
  public static String firewallUrl(String ownerId) {
    return "/assets/index.html#/firewall/management/view/organization/" + ownerId;
  }

  public Locator container() {
    return locator("#owner-summary");
  }

  public Locator ownerName() {
    return locator("#owner-summary .nx-h1");
  }

  /**
   * All rendered nav-pill list items ({@code li.iq-nav-pills-menu__pill}). Only pills whose feature
   * gate is satisfied are in the DOM, so the count reflects the visible pill set. See
   * {@code NavPills.jsx}.
   */
  public Locator navPills() {
    return locator(".iq-nav-pills-menu__list .iq-nav-pills-menu__pill");
  }

  /**
   * A single nav-pill button by its scroll target (e.g. {@code "owner-pill-retention"}); the button
   * id is {@code "{target}-button"}. Absent from the DOM when the pill's feature gate is off.
   */
  public Locator navPillButton(String target) {
    return locator("#" + target + "-button");
  }

  /**
   * The owner-summary tile/section element whose {@code id} is the nav-pill scroll {@code target}
   * (e.g. {@code "owner-pill-policy"}). These ids are the {@code NavPills} scroll anchors and always
   * exist for displayed sections, so they are stable across prod bundles (unlike {@code data-testid}).
   */
  public Locator ownerSummarySection(String target) {
    return locator("#" + target);
  }

  /**
   * "Add a Policy" button on the Policies tile (PoliciesTile.jsx, id="add-policy-button").
   * Clicking it routes the SPA to the new-policy editor for the current owner.
   */
  public Locator addPolicyButton() {
    return locator("#add-policy-button");
  }

  /**
   * "Import" button on the SBOMs tile of an application's OwnerSummary. Scoped through
   * {@link #sbomsTile()} so the role+name selector targets only the tile-local button and
   * cannot collide with the same-named "Import" submit button inside the Import Policies modal.
   */
  public Locator importSbomButton() {
    return sbomsTile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Import"));
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

  /**
   * Each tile locator below targets the {@code NxTile} section element whose {@code id} is the
   * nav-pill scroll target (e.g. {@code id="owner-pill-legacy-violations"}). {@code NxTile}
   * renders as {@code <section>} but does not set {@code aria-labelledby} automatically, so there
   * is no accessible name to match via {@code getByRole(REGION, name)}. The ID selector is the
   * only stable anchor for these containers; all interactions within them use role/name selectors.
   */
  public Locator legacyViolationsTile() {
    return locator("#owner-pill-legacy-violations");
  }

  public Locator continuousMonitoringTile() {
    return locator("#owner-pill-continuous-monitoring");
  }

  public Locator sbomsTile() {
    return locator("#owner-pill-sboms");
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

  public void clickSourceControlConfigurationLink() {
    sourceControlTile().getByRole(AriaRole.LINK).first().click();
  }

  public boolean isFeaturePillPresent(String pillTargetId) {
    return byTestId(pillTargetId + "-button").count() > 0;
  }

  public Locator autoWaiversTile() {
    return byTestId("iq-auto-waivers-tile");
  }

  /** See Javadoc on {@link #legacyViolationsTile()} for why NxTile sections use ID anchors. */
  public Locator innerSourceRepositoryTile() {
    return locator("#owner-pill-innersource-repository");
  }

  /** See Javadoc on {@link #legacyViolationsTile()} for why NxTile sections use ID anchors. */
  public Locator accessTile() {
    return locator("#access-tile-pill-access");
  }

  public Locator publicDataSourcesTile() {
    return byTestId("owner-pill-public-data-sources");
  }

  /** See Javadoc on {@link #legacyViolationsTile()} for why NxTile sections use ID anchors. */
  public Locator categoriesTile() {
    return locator("#owner-pill-app-categories");
  }

  /** See Javadoc on {@link #legacyViolationsTile()} for why NxTile sections use ID anchors. */
  public Locator dataRetentionTile() {
    return locator("#owner-pill-retention");
  }

  /** See Javadoc on {@link #legacyViolationsTile()} for why NxTile sections use ID anchors. */
  public Locator artifactoryRepositoryTile() {
    return locator("#owner-pill-artifactory-repository");
  }

  /** Repository URL block in the owner summary header ({@code div.page-repository-url}). */
  public Locator repositoryUrlBlock() {
    return container().locator(".page-repository-url");
  }

  public Locator accessTileLocalAccessList() {
    return locator("#iq-access-tile-local-access-list");
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
    Locator toggle = ownerActionsToggle();
    toggle.waitFor();
    toggle.click();
    ownerActionsMenu().waitFor();
  }

  /**
   * Clicks "Delete {ownerName}" in the Actions menu.
   * Button text includes the owner name at runtime, so the ID anchor is used instead of
   * a role/name selector to avoid fragility when the owner has a dynamic or UUID-based name.
   */
  public void clickDeleteOwnerMenuItem() {
    ownerActionsMenu().locator("#delete-owner-link").click();
  }

  /**
   * Clicks "Move {ownerName}" in the Actions menu.
   * Button text includes the owner name at runtime — ID anchor used for the same reason as
   * {@link #clickDeleteOwnerMenuItem()}.
   */
  public void clickMoveOwnerMenuItem() {
    ownerActionsMenu().locator("#owner-move-link").click();
  }

  /** Clicks "Change App ID" in the Actions menu (app context only). */
  public void clickChangeAppIdMenuItem() {
    ownerActionsMenu()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Change App ID").setExact(true))
        .click();
  }

  /** Clicks "Evaluate a File" in the Actions menu (app context only). */
  public void clickEvaluateFileMenuItem() {
    ownerActionsMenu()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Evaluate a File").setExact(true))
        .click();
  }

  /**
   * Clicks the Import Policies item in the Actions menu (org context only).
   * Button text is conditionally "Import Policies" (enterprise) or "Preview Import Policies"
   * (non-enterprise), so the ID anchor is used rather than a role/name selector.
   */
  public void clickImportPoliciesMenuItem() {
    ownerActionsMenu().locator("#import-policies-link").click();
  }

  /**
   * Clicks "Select Contact" in the Actions menu (app context only).
   * {@code setExact(true)} is required — without it, "Select Contact" would also match
   * "Move {appName}" and "Delete {appName}" when the app name contains "Select Contact".
   */
  public void clickSelectContactMenuItem() {
    ownerActionsMenu()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Select Contact").setExact(true))
        .click();
  }

  /**
   * Clicks "Copy {ownerType} ID to Clipboard" in the Actions menu.
   * Button text includes the owner type at runtime — ID anchor used for the same reason as
   * {@link #clickDeleteOwnerMenuItem()}.
   */
  public void clickCopyOrgIdMenuItem() {
    ownerActionsMenu().locator("#copy-org-id-link").click();
  }

  /** Clicks "Legacy existing violations" in the Actions menu (app context only). */
  public void clickGrantLegacyViolationsMenuItem() {
    ownerActionsMenu()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Legacy existing violations").setExact(true))
        .click();
  }

  /** Clicks "Revoke legacy status" in the Actions menu (app context only). */
  public void clickRevokeLegacyViolationMenuItem() {
    ownerActionsMenu()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Revoke legacy status").setExact(true))
        .click();
  }

  public String readClipboardText() {
    return (String) page.evaluate("navigator.clipboard.readText()");
  }

  /**
   * Clicks "Edit {ownerName} Name / Icon" in the Actions menu.
   * Button text includes the owner name at runtime — ID anchor used for the same reason as
   * {@link #clickDeleteOwnerMenuItem()}.
   */
  public void clickEditOwnerNameMenuItem() {
    ownerActionsMenu().locator("#app-org-link").click();
  }

  public Locator editOwnerModal() {
    return locator("#owner-editor");
  }

  /**
   * "Add New Organization" plus-button in the sidebar Organizations collapsible section
   * ({@code data-testid="organizations-add"}, rendered by {@code OwnerSideNav.jsx}).
   */
  public Locator addOrganizationButton() {
    return byTestId("organizations-add");
  }

  public void clickAddOrganizationButton() {
    addOrganizationButton().click();
    editOwnerModal().waitFor();
  }

  /**
   * "Add Application" icon dropdown toggle in the sidebar Applications collapsible section
   * ({@code NxStatefulIconDropdown title="Add Application"}, rendered by {@code OwnerSideNav.jsx}).
   */
  public Locator addApplicationDropdownToggle() {
    return locator("#applications-collapsible").locator("button.nx-icon-dropdown__toggle");
  }

  public void clickAddNewApplicationButton() {
    addApplicationDropdownToggle().click();
    locator("#applications-collapsible")
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("New Application"))
        .click();
    editOwnerModal().waitFor();
  }

  public void submitNewOwnerModal() {
    editOwnerModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create")).click();
    editOwnerModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public void clickCreateOwnerButton() {
    editOwnerModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create")).click();
  }

  public void typeOwnerName(String name) {
    editOwnerModal().locator("#editor-owner-name input").fill(name);
  }

  public void typeApplicationPublicId(String publicId) {
    editOwnerModal().locator("#editor-new-id input").fill(publicId);
  }

  public void submitEditOwnerModal() {
    editOwnerModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Update")).click();
    editOwnerModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public void cancelEditOwnerModal() {
    editOwnerModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
    editOwnerModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public Locator deleteOwnerModal() {
    return locator("#owner-delete-modal");
  }

  public void cancelDeleteOwnerModal() {
    deleteOwnerModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
    deleteOwnerModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public void confirmDeleteOwnerModal() {
    deleteOwnerModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete")).click();
  }

  public Locator moveOwnerModal() {
    return locator("#move-owner-modal");
  }

  public void selectMoveTarget(String parentOrgName) {
    moveOwnerModal().getByLabel("New Parent Organization")
        .selectOption(new SelectOption().setLabel(parentOrgName));
  }

  public void cancelMoveOwnerModal() {
    moveOwnerModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
    moveOwnerModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public void confirmMoveOwnerModal() {
    moveOwnerModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Move")).click();
  }

  public void confirmAndWaitForMoveOwnerModalToClose() {
    confirmMoveOwnerModal();
    moveOwnerModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public Locator changeAppIdModal() {
    return locator("#change-application-id-modal");
  }

  public void typeNewApplicationId(String newId) {
    changeAppIdModal().locator("#editor-new-id input").fill(newId);
  }

  public void confirmChangeAppIdModal() {
    changeAppIdModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Change")).click();
    changeAppIdModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public void cancelChangeAppIdModal() {
    changeAppIdModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
    changeAppIdModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public Locator evaluateFileModal() {
    return locator("#evaluate-application-modal");
  }

  public Locator evaluationStatusModal() {
    return locator("#evaluation-status-modal");
  }

  public void cancelEvaluateFileModal() {
    evaluateFileModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
    evaluateFileModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public void selectEvaluateFileStage(String stageTypeId) {
    evaluateFileModal().locator("select").selectOption(stageTypeId);
  }

  public void uploadEvaluateFile(Path filePath) {
    FileChooser fileChooser = page.waitForFileChooser(
        () -> evaluateFileModal().locator(".nx-file-upload__select-btn").click());
    fileChooser.setFiles(filePath);
  }

  public void submitEvaluateFileModal() {
    evaluateFileModal().locator(".nx-form__submit-btn").click();
  }

  public void closeEvaluationStatusModal() {
    locator("#evaluation-status-modal")
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close"))
        .click();
    locator("#evaluation-status-modal").waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN));
  }

  public Locator importPolicyModal() {
    return locator("#import-policy-modal");
  }

  public void cancelImportPolicyModal() {
    importPolicyModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
    importPolicyModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public void uploadImportPoliciesFile(Path filePath) {
    FileChooser fileChooser = page.waitForFileChooser(
        () -> importPolicyModal().locator(".nx-file-upload__select-btn").click());
    fileChooser.setFiles(filePath);
  }

  public void submitImportPoliciesModal() {
    importPolicyModal().locator(".nx-form__submit-btn").click();
    importPolicyModal().waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN)
        .setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  /**
   * Returns {@code true} when the server has the {@code custom-policies} enterprise entitlement
   * (the full ImportPoliciesModal with an Import submit button is shown). Returns {@code false}
   * when the server lacks the entitlement (preview-only modal with a Close button is shown).
   * Call this after opening the Import Policies modal.
   */
  public boolean isImportPoliciesInEnterpriseMode() {
    return importPolicyModal()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Import"))
        .isVisible();
  }

  public void closeImportPoliciesPreviewModal() {
    importPolicyModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close")).click();
    importPolicyModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public Locator selectContactModal() {
    return locator("#select-contact-modal");
  }

  public void searchContact(String username) {
    // Click first to establish focus (sets NxCombobox inputIsFocused=true so the dropdown renders),
    // then fill to trigger React onChange / onSearch with the query value.
    Locator input = selectContactModal().locator(".nx-combobox input");
    input.click();
    input.fill(username);
  }

  public void selectFirstContactResult() {
    // Wait directly for the first result button — the loading indicator (.nx-combobox__alert) may
    // be too brief to catch reliably when the embedded server responds quickly.
    Locator result = selectContactModal().locator(".nx-combobox__menu .nx-dropdown-button").first();
    result.waitFor(new Locator.WaitForOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
    result.click();
  }

  public void saveContact() {
    selectContactModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
    selectContactModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public void cancelSelectContactModal() {
    selectContactModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
    selectContactModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  /** Filter input rendered inside the OwnersTreeTile within InsufficientPermissionOwnerHierarchyTree. */
  public Locator insufficientPermissionTreeFilterInput() {
    return locator("#iq-owner-tree-filter-input");
  }

  /** Description paragraph rendered by InsufficientPermissionOwnerHierarchyTree. */
  public Locator insufficientPermissionTreeDescription() {
    return locator(".nx-page-title__description .nx-p");
  }
}
