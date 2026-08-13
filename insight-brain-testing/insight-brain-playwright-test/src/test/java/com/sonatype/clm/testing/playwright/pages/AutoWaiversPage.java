/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

public class AutoWaiversPage
    extends BasePage
{
  private static final String ROOT_TESTID = "auto-waivers-configuration";

  private static final String MODAL_TESTID = "iq-auto-waiver-modal";

  private static final Locator.GetByRoleOptions AUTOMATED_WAIVERS_HEADING_OPTS =
      new Locator.GetByRoleOptions().setName("Automated Waivers");

  private static final Locator.GetByRoleOptions CONFIGURED_AUTO_WAIVERS_HEADING_OPTS =
      new Locator.GetByRoleOptions().setName("Configured Auto-Waivers");

  private static final Locator.GetByRoleOptions NEW_AUTO_WAIVER_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("New Auto-Waiver");

  private static final Locator.GetByRoleOptions NEW_AUTO_WAIVER_HEADING_OPTS =
      new Locator.GetByRoleOptions().setName("New Auto-Waiver");

  private static final Locator.GetByRoleOptions DELETE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName(Pattern.compile("[Dd]elete"));

  private static final Locator.GetByRoleOptions VIEW_LINK_OPTS =
      new Locator.GetByRoleOptions().setName(Pattern.compile("View"));

  private static final Locator.GetByRoleOptions CREATE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Create");

  private static final Locator.GetByRoleOptions DELETE_EXACT_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Delete");

  private static final Locator.GetByRoleOptions EDIT_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Edit");

  private static final Locator.GetByRoleOptions AUTO_WAIVER_DETAILS_HEADING_OPTS =
      new Locator.GetByRoleOptions().setName("Auto-Waiver Details");

  private static final Locator.GetByRoleOptions EDIT_MODAL_HEADING_OPTS =
      new Locator.GetByRoleOptions().setName(Pattern.compile("Edit.*Auto-Waiver"));

  private static final Locator.GetByRoleOptions UPDATE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Update");

  private static final Locator.GetByRoleOptions PREVIEW_ADD_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName(Pattern.compile("Preview Add Auto Waiver"));

  private static final Locator.FilterOptions DELETE_CONFIRMATION_FILTER =
      new Locator.FilterOptions().setHasText("You are about to permanently delete an auto-waiver");

  private static final Pattern THREAT_LEVEL_DROPDOWN_PATTERN =
      Pattern.compile("Threat level|\\d+ - ");

  private static final Pattern SCOPE_DROPDOWN_PATTERN = Pattern.compile("^(?:any|all|any\\/all)$");

  private static final String UPGRADE_PATH_CHECKBOX_TEXT = "non-violating component version";

  private static final String REACHABILITY_CHECKBOX_TEXT = "calls to the vulnerable method";

  public AutoWaiversPage() {
    super();
  }

  public static String url(String organizationId) {
    return "/assets/index.html#/management/edit/organization/" + organizationId + "/autowaivers";
  }

  public Locator container() {
    return byTestId(ROOT_TESTID);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING, AUTOMATED_WAIVERS_HEADING_OPTS);
  }

  public Locator pageSubtitle() {
    return container().getByText(
        Pattern.compile("Limit disruptions by deprioritizing low-threat violations"));
  }

  public Locator tableSectionHeading() {
    return container().getByRole(AriaRole.HEADING, CONFIGURED_AUTO_WAIVERS_HEADING_OPTS);
  }

  public Locator newAutoWaiverButton() {
    return container().getByRole(AriaRole.BUTTON, NEW_AUTO_WAIVER_BUTTON_OPTS);
  }

  public Locator activeTooltip() {
    return page.getByRole(AriaRole.TOOLTIP);
  }

  public Locator table() {
    return container().getByRole(AriaRole.TABLE);
  }

  public Locator tableColumnHeader(String name) {
    return table().locator("thead").getByText(name);
  }

  public Locator emptyStateRow() {
    return table().getByRole(AriaRole.CELL)
        .filter(
            new Locator.FilterOptions().setHasText("No automations to display"));
  }

  public Locator tableRows() {
    return table().locator("tbody tr.iq-auto-waiver-row");
  }

  public Locator tableRowByOwner(String ownerName) {
    return table().locator("tbody")
        .getByRole(AriaRole.ROW)
        .filter(
            new Locator.FilterOptions().setHasText(ownerName));
  }

  public Locator inheritedGroupHeader(String parentOrgName) {
    return table().locator("tbody")
        .getByRole(AriaRole.ROW)
        .filter(
            new Locator.FilterOptions().setHasText("Inherited from " + parentOrgName));
  }

  public Locator deleteButtonInRow(Locator row) {
    return row.getByRole(AriaRole.BUTTON, DELETE_BUTTON_OPTS);
  }

  public Locator viewEditLinkInRow(Locator row) {
    return row.getByRole(AriaRole.LINK, VIEW_LINK_OPTS);
  }

  public Locator modal() {
    return byTestId(MODAL_TESTID);
  }

  public Locator modalHeading() {
    return modal().getByRole(AriaRole.HEADING, NEW_AUTO_WAIVER_HEADING_OPTS);
  }

  public Locator threatLevelDropdown() {
    return modal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(THREAT_LEVEL_DROPDOWN_PATTERN));
  }

  public Locator scopeDropdown() {
    return modal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(SCOPE_DROPDOWN_PATTERN));
  }

  public Locator upgradePathCheckbox() {
    return modal().locator("label")
        .filter(
            new Locator.FilterOptions().setHasText(UPGRADE_PATH_CHECKBOX_TEXT));
  }

  public Locator reachabilityCheckbox() {
    return modal().locator("label")
        .filter(
            new Locator.FilterOptions().setHasText(REACHABILITY_CHECKBOX_TEXT));
  }

  public Locator validationErrorAlert() {
    return modal().getByRole(AriaRole.ALERT);
  }

  public Locator createButton() {
    return modal().getByRole(AriaRole.BUTTON, CREATE_BUTTON_OPTS);
  }

  public Locator cancelButton() {
    return modal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator deleteConfirmationModal() {
    return page.getByRole(AriaRole.DIALOG).filter(DELETE_CONFIRMATION_FILTER);
  }

  public Locator deleteConfirmButton() {
    return deleteConfirmationModal().getByRole(AriaRole.BUTTON, DELETE_EXACT_BUTTON_OPTS);
  }

  public Locator deleteCancelButton() {
    return deleteConfirmationModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator missingLicenseAlert() {
    return byTestId("iq-integrations__missing-license");
  }

  public Locator licenseLockScreen() {
    return page.getByRole(AriaRole.MAIN);
  }

  public Locator enterpriseBanner() {
    return container().getByText("Enterprise Feature");
  }

  public Locator previewAddAutoWaiverButton() {
    return container().getByRole(AriaRole.BUTTON, PREVIEW_ADD_BUTTON_OPTS);
  }

  public Locator scopeCell(Locator row) {
    return row.getByRole(AriaRole.CELL).nth(3);
  }

  public Locator threatBadge(Locator row) {
    return row.getByRole(AriaRole.CELL).nth(2);
  }

  public Locator threatLevelText(Locator row) {
    return row.getByRole(AriaRole.CELL).nth(2).locator("span").last();
  }

  public Locator previewRow() {
    return table().locator("tbody tr.iq-auto-waiver-preview-row");
  }

  public Locator detailsEditButton() {
    return autoWaiverDetailsRoot().getByRole(AriaRole.BUTTON, EDIT_BUTTON_OPTS);
  }

  public Locator detailsDeleteButton() {
    return autoWaiverDetailsRoot().getByRole(AriaRole.BUTTON, DELETE_EXACT_BUTTON_OPTS);
  }

  public Locator detailsHeading() {
    return autoWaiverDetailsRoot().getByRole(AriaRole.HEADING, AUTO_WAIVER_DETAILS_HEADING_OPTS);
  }

  public Locator editModalHeading() {
    return modal().getByRole(AriaRole.HEADING, EDIT_MODAL_HEADING_OPTS);
  }

  public Locator updateButton() {
    return modal().getByRole(AriaRole.BUTTON, UPDATE_BUTTON_OPTS);
  }

  public Locator autoWaiverDetailsRoot() {
    return byTestId("auto-waiver-details");
  }

  public void waitForAutoWaiverDetailsPage() {
    autoWaiverDetailsRoot().waitFor();
  }

  public Locator firstInheritedGroupHeaderRow() {
    return table().locator("tbody")
        .getByRole(AriaRole.ROW)
        .filter(
            new Locator.FilterOptions().setHasText("Inherited from"))
        .first();
  }
}
