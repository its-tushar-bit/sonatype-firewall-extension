/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page object for the Artifactory Repository Base Configurations screen
 * ({@code ArtifactoryRepositoryBaseConfigurations.jsx},
 * URL fragment {@code /artifactoryRepositoryBaseConfigurations}).
 */
public class ArtifactoryRepoConfigPage
    extends BasePage
{
  public static final String URL_FRAGMENT = "/artifactoryRepositoryBaseConfigurations";

  /** Full hash URL for a given org/app owner ID. */
  public static String url(String ownerId) {
    return "/assets/index.html#/management/edit/organization/" + ownerId + URL_FRAGMENT;
  }

  /**
   * Base URL path prefix for the artifactory connection status API.
   * Used in {@link #waitForResponse} predicates.
   */
  private static final Pattern ARTIFACTORY_STATUS_PUT =
      Pattern.compile(".*/api/v2/config/artifactoryConnection/[^/]+/[^/]+$");

  private static final Pattern ARTIFACTORY_DELETE =
      Pattern.compile(".*/api/v2/config/artifactoryConnection/[^/]+/[^/]+/.+");

  public ArtifactoryRepoConfigPage() {
    super();
  }

  /** Top-level {@code <main>} landmark — NxPageMain renders {@code <main>}. */
  public Locator container() {
    return page.getByRole(AriaRole.MAIN);
  }

  /** "Inherit" radio — only present on child org / application nodes. */
  public NxRadio inheritRadio() {
    return NxRadio.of(container(), "Inherit");
  }

  /** "Disable" radio — present at all org/app nodes. */
  public NxRadio disableRadio() {
    return NxRadio.of(container(), "Disable");
  }

  /**
   * "Enable" radio — present at the root org node only.
   * Child orgs show "Enable and Override Repository Connections" instead.
   */
  public NxRadio enableRadio() {
    return NxRadio.of(container(), "Enable");
  }

  /** "Enable and Override Repository Connections" radio — present on child org nodes. */
  public NxRadio enableAndOverrideRadio() {
    return NxRadio.of(container(), "Enable and Override Repository Connections");
  }

  /** Visible label for the "Allow Override" checkbox — use for {@code click()}/{@code isVisible()}. */
  public Locator allowOverrideCheckboxLabel() {
    return container().getByText("Allow Override", new Locator.GetByTextOptions().setExact(true));
  }

  /**
   * Lock info-alert shown when a parent org has disabled override.
   * Text: "The inherited configuration cannot be overridden."
   */
  public Locator lockAlert() {
    return container().getByText("The inherited configuration cannot be overridden.");
  }

  /** "Add a Repository" button. */
  public Locator addButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Add a Repository"));
  }

  /** "LOCAL" heading rendered above the connection list when Enable mode is active. */
  public Locator localHeader() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("LOCAL"));
  }

  /** Empty-state message rendered inside the connections NxList when no connections exist. */
  public Locator emptyListMessage() {
    return container().getByText("No Artifactory repository connection is configured");
  }

  /**
   * Edit (pen) button for an existing connection row.
   * CSS class selector used: the button's {@code title} attribute is dynamically composed
   * by {@code getAddOrEditTooltip()} and changes between "Edit Repository Configuration" and
   * the must-update message depending on the server-saved {@code enabled} state, making a
   * title-based selector fragile. The CSS class is set unconditionally on the element.
   */
  public Locator editButton() {
    return container().locator(".artifactory-repository-base-configurations-edit-button");
  }

  /**
   * Delete (trash) button for an existing connection row.
   * See {@link #editButton()} for selector rationale.
   */
  public Locator deleteButton() {
    return container().locator(".artifactory-repository-base-configurations-delete-button");
  }

  /**
   * Raw {@code <input type="radio">} inside the Inherit NxRadio label.
   * Used for {@code isDisabled()} assertions in locked-parent scenarios where
   * {@code getByRole(AriaRole.RADIO)} fails to resolve the CSS-hidden input.
   * The NxRadio {@code id} prop is placed on the {@code <label>} element (via RSC
   * {@code otherProps}); the CSS child combinator descends to the actual input.
   */
  public Locator inheritRadioRawInput() {
    return container().locator(
        "#artifactory-repository-base-configurations-inherit-radio input[type='radio']");
  }

  /**
   * Raw {@code <input type="radio">} inside the Disable NxRadio label.
   * See {@link #inheritRadioRawInput()} for selector rationale.
   */
  public Locator disableRadioRawInput() {
    return container().locator(
        "#artifactory-repository-base-configurations-disable-radio input[type='radio']");
  }

  /** "Update" submit button for the main status form. */
  public Locator updateButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Update"));
  }

  // ── Add / Edit modal ──────────────────────────────────────────────────────

  /**
   * Add/Edit connection modal container.
   * NxModal sets {@code aria-labelledby} pointing to its heading element; the heading text is
   * dynamic ({@code "Add Artifactory Repository Configuration"} or
   * {@code "Edit Artifactory Repository Configuration"}), so a regex pattern matches both forms.
   */
  public Locator addModal() {
    return page.getByRole(AriaRole.DIALOG,
        new Page.GetByRoleOptions().setName(Pattern.compile(".*Artifactory Repository Configuration")));
  }

  /** Heading inside the Add/Edit modal — text is "Add …" or "Edit …". */
  public Locator addModalHeading() {
    return addModal().getByRole(AriaRole.HEADING);
  }

  /** Repository Base URL input inside the Add/Edit modal. */
  public Locator baseUrlInput() {
    return addModal().getByRole(AriaRole.TEXTBOX,
        new Locator.GetByRoleOptions().setName("Repository Base URL"));
  }

  /** "Allow Anonymous Access" radio inside the Add/Edit modal auth fieldset. */
  public NxRadio anonymousAuthRadio() {
    return NxRadio.of(addModal(), "Allow Anonymous Access");
  }

  /** "Enter Username and Password" radio inside the Add/Edit modal auth fieldset. */
  public NxRadio credentialsAuthRadio() {
    return NxRadio.of(addModal(), "Enter Username and Password");
  }

  /** Username input — only visible when Credentials auth is selected. */
  public Locator usernameInput() {
    return addModal().getByRole(AriaRole.TEXTBOX,
        new Locator.GetByRoleOptions().setName("Username"));
  }

  /**
   * Password input — resolved via the {@code <NxFormGroup label="Password">} label within the modal.
   * Exact match required: {@code getByLabel("Password")} without exact would also match the
   * "Enter Username and Password" radio label (substring).
   */
  public Locator passwordInput() {
    return addModal().getByLabel("Password", new Locator.GetByLabelOptions().setExact(true));
  }

  /**
   * "Test Configuration" button inside the Add/Edit modal.
   * {@code aria-disabled} when Base URL is empty; do not use {@code isEnabled()} — use
   * {@code hasAttribute("aria-disabled")} to check the disabled state.
   */
  public Locator testConfigButton() {
    return addModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Test Configuration"));
  }

  // ── Delete confirmation modal ─────────────────────────────────────────────

  /**
   * Delete confirmation modal container.
   * NxModal sets {@code aria-labelledby} pointing to its static heading
   * {@code "Delete Repository Configuration?"}.
   */
  public Locator deleteModal() {
    return page.getByRole(AriaRole.DIALOG,
        new Page.GetByRoleOptions().setName("Delete Repository Configuration?"));
  }

  /** "OK" confirm button inside the delete modal. */
  public Locator deleteConfirmButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("OK"));
  }

  /** "Cancel" button inside the delete modal. */
  public Locator deleteCancelButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"));
  }

  // ── Action methods ────────────────────────────────────────────────────────

  /**
   * Clicks "Update" and waits for the {@code PUT artifactoryConnection} response to complete.
   * Wraps the wait in the page object so tests stay free of response-URL details.
   */
  public void clickUpdateAndWait() {
    page.waitForResponse(
        r -> ARTIFACTORY_STATUS_PUT.matcher(r.url()).matches()
            && "PUT".equalsIgnoreCase(r.request().method()),
        () -> updateButton().click());
  }

  /**
   * Clicks the delete "OK" confirm button and waits for the {@code DELETE} response.
   */
  public void clickDeleteConfirmAndWait() {
    page.waitForResponse(
        r -> ARTIFACTORY_DELETE.matcher(r.url()).matches()
            && "DELETE".equalsIgnoreCase(r.request().method()),
        () -> deleteConfirmButton().click());
  }
}
