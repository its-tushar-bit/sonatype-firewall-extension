/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class WaiverDetailsPage
    extends BasePage
{
  private static final String ROOT = "#waiver-details-page";

  public WaiverDetailsPage() {
    super();
  }

  public static String url(String ownerType, String ownerId, String waiverId) {
    return "/assets/index.html#/waiver/" + ownerType + "/" + ownerId + "/" + waiverId;
  }

  public static String urlWithQueryParams(
      String ownerType,
      String ownerId,
      String waiverId,
      String type,
      String sidebarReference)
  {
    return "/assets/index.html#/waiver/" + ownerType + "/" + ownerId + "/" + waiverId
        + "?type=" + type + "&sidebarReference=" + sidebarReference;
  }

  public static String urlWithQueryParams(
      String ownerType,
      String ownerId,
      String waiverId,
      String type,
      String sidebarReference,
      int pageNum)
  {
    return urlWithQueryParams(ownerType, ownerId, waiverId, type, sidebarReference)
        + "&page=" + pageNum;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator detailsTileHeader() {
    return locator(ROOT + " #iq-waiver-details-header");
  }

  public Locator detailsPolicy() {
    return locator(ROOT + " .iq-waiver-details__policy .nx-read-only__data");
  }

  public Locator detailsConstraint() {
    return locator(
        ROOT + " .iq-waiver-details__constraint .nx-read-only__data:not(.iq-waiver-details__constrain-conditions)");
  }

  public Locator detailsConditions() {
    return locator(
        ROOT + " .iq-waiver-details__constraint .nx-read-only__data.iq-waiver-details__constrain-conditions");
  }

  public Locator vulnerabilityDetailsButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Vulnerability Details"));
  }

  public Locator vulnerabilityDetailsModal() {
    return locator("#vulnerability-details-modal");
  }

  public Locator vulnerabilityDetailsModalCloseButton() {
    return vulnerabilityDetailsModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close"));
  }

  public Locator detailsScope() {
    return locator(ROOT + " .iq-waiver-details__scope .nx-read-only__data");
  }

  public Locator detailsComponent() {
    return locator(ROOT + " .iq-waiver-details__components .nx-read-only__data");
  }

  public Locator detailsExpiration() {
    return locator(ROOT + " .iq-waiver-details__expiration .nx-read-only__data");
  }

  public Locator detailsComment() {
    return locator(ROOT + " .iq-waiver-details__comments .nx-read-only__data");
  }

  public Locator detailsCreatedBy() {
    return locator(ROOT + " .iq-waiver-details__created-by .nx-read-only__data");
  }

  public Locator detailsDateCreated() {
    return locator(ROOT + " .iq-waiver-details__date-created .nx-read-only__data");
  }

  public Locator detailsReason() {
    return locator(ROOT + " .iq-waiver-details__reason .nx-read-only__data");
  }

  public Locator detailsVersion() {
    return locator(ROOT + " .iq-waiver-details__version .nx-read-only__data");
  }

  public Locator deleteWaiverButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete Waiver"));
  }

  public Locator upgradeAvailableIndicator() {
    return locator(ROOT + " .iq-upgrade-available-indicator");
  }

  public Locator sidebarNavTitle() {
    return locator("#sidebar-nav-list .nx-h4");
  }

  public Locator sidebarNavItems() {
    return locator("#sidebar-nav-list li");
  }

  public Locator sidebarNavItem(int index) {
    return locator("#sidebar-nav-list li:nth-child(" + (index + 1) + ")");
  }

  public Locator sidebarNavItemThreatIndicator(int index) {
    return sidebarNavItem(index).locator(".nx-threat-indicator");
  }

  public Locator sidebarNavItemPolicyName(int index) {
    return sidebarNavItem(index).locator(".nx-list__text");
  }

  public Locator sidebarNavItemComponentName(int index) {
    return sidebarNavItem(index).locator(".nx-list__subtext .iq-component-display-text");
  }

  public Locator sidebarNavItemOrgName(int index) {
    return sidebarNavItem(index).locator(".nx-list__subtext .nx-truncate-ellipsis:nth-child(2)");
  }

  public Locator deleteWaiverModal() {
    return locator("#delete-waiver-modal");
  }

  public Locator deleteWaiverModalYesButton() {
    return deleteWaiverModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete Waiver"));
  }

  public Locator deleteWaiverModalCancelButton() {
    return deleteWaiverModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"));
  }

  public void clickVulnerabilityDetailsButton() {
    vulnerabilityDetailsButton().click();
  }

  public void clickDeleteWaiverButton() {
    deleteWaiverButton().click();
  }

  public void clickDeleteWaiverModalYesButton() {
    deleteWaiverModalYesButton().click();
  }

  public void deleteWaiverAndConfirm() {
    detailsPolicy().waitFor();
    clickDeleteWaiverButton();
    deleteWaiverModal().waitFor();
    clickDeleteWaiverModalYesButton();
  }

  public void clickVulnerabilityDetailsModalCloseButton() {
    vulnerabilityDetailsModalCloseButton().click();
  }

}
