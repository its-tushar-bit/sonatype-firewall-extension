/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright component object for the global "Orgs and Policies" summary sidebar
 * ({@code OwnerSideNav.jsx}, root {@code .nx-page-sidebar.iq-orgs-and-policies-summary-sidebar}).
 * <p>
 * Hosts the currently-selected owner header ({@code .iq-selected-org}), the collapsible
 * {@code Organizations} group ({@code #organizations-collapsible}, role {@code menu}) whose
 * {@code role="menuitem"} children link to child orgs, the collapsible {@code Applications}
 * group ({@code #applications-collapsible}), and the per-owner Add Application split-button
 * ({@code <NxStatefulIconDropdown title="Add Application">} → {@code New Application} option).
 * <p>
 * Distinct from {@link SidebarComponent} (the cross-app left-rail navigation) and from
 * {@link OwnerDetailSidebarComponent} (the right-hand owner-detail tile sidebar).
 */
public class OrgsAndPoliciesSidebarComponent
    extends BasePage
{
  private static final String ROOT = ".nx-page-sidebar.iq-orgs-and-policies-summary-sidebar";

  private static final String ORGANIZATIONS_GROUP = "#organizations-collapsible";

  private static final String APPLICATIONS_GROUP = "#applications-collapsible";

  /** Owner editor modal root id ({@code OwnerModal.jsx:157} → {@code <NxModal id="owner-editor">}). */
  private static final String OWNER_EDITOR_MODAL = "#owner-editor";

  public OrgsAndPoliciesSidebarComponent() {
    super();
  }

  // --------------- Roots ---------------

  /** Outer sidebar container. */
  public Locator container() {
    return locator(ROOT);
  }

  /**
   * The {@code .iq-selected-org} block at the top of the sidebar, rendered for the currently
   * displayed owner ({@code OwnerSideNav.jsx} → {@code SelectedOwner}). Always present whenever
   * an owner is in scope.
   */
  public Locator selectedOwner() {
    return locator(ROOT + " .iq-selected-org");
  }

  // --------------- Organizations group ---------------

  /** The Organizations {@code NxCollapsibleItems} container. */
  public Locator organizationsGroup() {
    return locator(ROOT + " " + ORGANIZATIONS_GROUP);
  }

  /**
   * All child-organization links inside {@code #organizations-collapsible}.
   * <p>
   * RSC's {@code NxCollapsibleItems.Child} clones its child element and sets the
   * {@code role="menuitem"} attribute directly on that child via
   * {@code React.cloneElement} (not on a wrapper). Since {@code Organization.jsx} renders an
   * {@code <a>} as the child, the resulting DOM is
   * {@code <a role="menuitem" class="nx-collapsible-items__child" href="...">…</a>} — the
   * anchor IS the menuitem, not a child of it.
   */
  public Locator organizationLinks() {
    return locator(ROOT + " " + ORGANIZATIONS_GROUP + " a[role=\"menuitem\"]");
  }

  // --------------- Applications group + Add Application dropdown ---------------

  /** The Applications {@code NxCollapsibleItems} container. */
  public Locator applicationsGroup() {
    return locator(ROOT + " " + APPLICATIONS_GROUP);
  }

  /**
   * The "Add Application" split-button trigger ({@code <NxStatefulIconDropdown title="Add
   * Application">} on {@code OwnerSideNav.jsx:340}).
   * <p>
   * RSC's icon-only {@code NxButton} wraps the {@code title} prop in an {@code NxTooltip} with
   * {@code isName=true}, which projects the title onto the trigger as {@code aria-label}
   * (NOT as the HTML {@code title} attribute). Use {@link Locator#getByRole} so we match the
   * button by its accessible name regardless of how RSC renders the underlying tooltip.
   */
  public Locator addApplicationDropdownTrigger() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Add Application"));
  }

  /** The "New Application" dropdown option ({@code OwnerSideNav.jsx:341-343}). */
  public Locator newApplicationOption() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("New Application"));
  }

  // --------------- Owner editor modal (opens on New Application click) ---------------

  /** The owner-editor {@code NxModal} ({@code OwnerModal.jsx:157}). */
  public Locator ownerEditorModal() {
    return locator(OWNER_EDITOR_MODAL);
  }

  // --------------- Business actions ---------------

  /**
   * Open the Add Application dropdown for the currently-displayed owner and select
   * {@code New Application}, returning the now-visible owner-editor modal.
   */
  public Locator openNewApplicationModal() {
    addApplicationDropdownTrigger().click();
    Locator option = newApplicationOption();
    assertThat(option).isVisible();
    option.click();
    Locator modal = ownerEditorModal();
    assertThat(modal).isVisible();
    return modal;
  }

}
