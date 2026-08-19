/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AutoWaiversPageAssertions
{
  private final AutoWaiversPage page;

  public AutoWaiversPageAssertions(AutoWaiversPage page) {
    this.page = page;
  }

  public void shouldRenderPage() {
    page.container().waitFor();
    assertThat(page.pageHeading()).isVisible();
    assertThat(page.pageSubtitle()).isVisible();
    assertThat(page.tableSectionHeading()).isVisible();
    assertThat(page.table()).isVisible();
    assertThat(page.newAutoWaiverButton()).isVisible();
  }

  public void shouldShowTableColumns() {
    assertThat(page.tableColumnHeader("Created")).isVisible();
    assertThat(page.tableColumnHeader("Owner")).isVisible();
    assertThat(page.tableColumnHeader("Max. Threat")).isVisible();
    assertThat(page.tableColumnHeader("Scope")).isVisible();
    assertThat(page.tableColumnHeader("Details")).isVisible();
    assertThat(page.tableColumnHeader("Delete")).isVisible();
  }

  public void shouldShowNewAutoWaiverModal() {
    assertThat(page.modal()).isVisible();
    assertThat(page.modalHeading()).isVisible();
    assertThat(page.modal()
        .getByLabel(
            "No newer, non-violating component version is available",
            new Locator.GetByLabelOptions().setExact(false))).not().isChecked();
    assertThat(page.modal()
        .getByLabel(
            "Application does not execute any calls to the vulnerable method",
            new Locator.GetByLabelOptions().setExact(false))).not().isChecked();
  }

  public void shouldShowValidationErrorAfterCreateAttempt() {
    page.createButton().click();
    assertThat(page.validationErrorAlert()).isVisible();
    assertThat(page.validationErrorAlert())
        .containsText("Either 'Upgrade Path is not available' or 'Vulnerability is not reachable' is required");
  }

  public void shouldShowNewAutoWaiverButtonDisabled(int localRowCount) {
    assertThat(page.tableRows()).hasCount(localRowCount);
    assertThat(page.newAutoWaiverButton()).isDisabled();
  }

  public void shouldShowInheritedGroupHeader(String parentOrgName) {
    assertThat(page.inheritedGroupHeader(parentOrgName)).isVisible();
  }

  public void shouldShowDisabledDeleteOnInheritedRow(Locator inheritedRow) {
    Locator btn = page.deleteButtonInRow(inheritedRow);
    assertThat(btn).isVisible();
    assertThat(btn).isDisabled();
  }

  public void shouldShowDeleteConfirmationModal() {
    assertThat(page.deleteConfirmationModal()).isVisible();
    assertThat(page.deleteConfirmationModal()).containsText("You are about to permanently delete an auto-waiver");
    assertThat(page.deleteConfirmationModal()).containsText("This action cannot be undone.");
  }

  public void shouldHideDeleteConfirmationModal() {
    assertThat(page.deleteConfirmationModal()).isHidden();
  }

  public void shouldShowEmptyState(String message) {
    assertThat(page.emptyStateRow()).isVisible();
    assertThat(page.emptyStateRow()).containsText(message);
  }

  public void shouldShowMissingLicenseAlert() {
    assertThat(page.missingLicenseAlert()).isVisible();
  }

  public void shouldHideModal() {
    assertThat(page.modal()).isHidden();
  }

  public void shouldShowWaiverRows() {
    assertThat(page.tableRows().first()).isVisible();
  }

  public void shouldShowRowContent(Locator row, int threatLevel, String scopeText) {
    assertThat(page.threatBadge(row)).isVisible();
    assertThat(page.threatLevelText(row)).hasText(String.valueOf(threatLevel));
    assertThat(page.scopeCell(row)).hasText(scopeText);
  }

  public void shouldShowEnterprisePreviewMode() {
    page.container().waitFor();
    assertThat(page.enterpriseBanner()).isVisible();
    assertThat(page.previewAddAutoWaiverButton()).isVisible();
    assertThat(page.previewRow()).isVisible();
  }

  public void shouldShowScopeDropdownDisabled() {
    assertThat(page.scopeDropdown()).hasClass(Pattern.compile(".*\\bdisabled\\b.*"));
  }

  public void shouldShowMaxConfigTooltip(String expectedTooltip) {
    page.newAutoWaiverButton().hover();
    assertThat(page.activeTooltip()).hasText(expectedTooltip);
  }

  public void shouldShowDeleteTooltipOnInheritedRow(Locator inheritedRow, String expectedTooltip) {
    page.deleteButtonInRow(inheritedRow).hover();
    assertThat(page.activeTooltip()).hasText(expectedTooltip);
  }

  public void shouldShowDetailsEditAndDeleteButtons() {
    assertThat(page.detailsEditButton()).isVisible();
    assertThat(page.detailsEditButton()).isEnabled();
    assertThat(page.detailsDeleteButton()).isVisible();
    assertThat(page.detailsDeleteButton()).isEnabled();
  }

  public void shouldShowDetailsEditAndDeleteButtonsDisabledForInherited(
      String editTooltip,
      String deleteTooltip)
  {
    assertThat(page.detailsEditButton()).isVisible();
    assertThat(page.detailsEditButton()).isDisabled();
    assertThat(page.detailsEditButton()).hasAttribute("title", editTooltip);
    assertThat(page.detailsDeleteButton()).isVisible();
    assertThat(page.detailsDeleteButton()).isDisabled();
    assertThat(page.detailsDeleteButton()).hasAttribute("title", deleteTooltip);
  }

  public void clickViewEditAndWaitForDetails(Locator row) {
    Locator link = page.viewEditLinkInRow(row);
    assertThat(link).isVisible();
    link.click();
    page.waitForAutoWaiverDetailsPage();
  }

  public void deleteFirstWaiverAndConfirm() {
    Locator firstRow = page.tableRows().first();
    assertThat(firstRow).isVisible();
    page.deleteButtonInRow(firstRow).click();
    shouldShowDeleteConfirmationModal();
    page.deleteConfirmButton().click();
  }

  public void deleteFirstWaiverAndCancel() {
    Locator firstRow = page.tableRows().first();
    assertThat(firstRow).isVisible();
    page.deleteButtonInRow(firstRow).click();
    shouldShowDeleteConfirmationModal();
    page.deleteCancelButton().click();
    shouldHideDeleteConfirmationModal();
    assertThat(page.tableRows().first()).isVisible();
  }

  public void clickEditAndWaitForEditModal() {
    page.detailsEditButton().click();
    assertThat(page.modal()).isVisible();
    assertThat(page.editModalHeading()).isVisible();
  }

  public void submitEditModalAndWaitForDetails() {
    page.updateButton().click();
    assertThat(page.modal()).not().isVisible();
    page.autoWaiverDetailsRoot().waitFor();
  }
}
