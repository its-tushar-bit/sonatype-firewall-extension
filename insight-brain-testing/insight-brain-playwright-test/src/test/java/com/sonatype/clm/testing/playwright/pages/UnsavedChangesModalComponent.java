/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Page object for the global "Unsaved Changes" confirmation modal
 * ({@code UnsavedChangesModal.jsx}).
 *
 * <p>
 * The modal is shared across many flows (administrators edit, owner edit, policy editor, etc.) and
 * is rendered into the SPA's top-level {@code ModalContainer}. All members are anchored under the
 * modal's stable {@code #unsaved-modal} id so the locator is strict-mode safe even when the
 * underlying screen contains other modals (see Playwright authoring guide §4a).
 */
public class UnsavedChangesModalComponent
    extends BasePage
{
  private static final String ROOT = "#unsaved-modal";

  public UnsavedChangesModalComponent() {
    super();
  }

  public Locator container() {
    // NxModal does not set aria-labelledby in this RSC version — use the stable id.
    return locator(ROOT);
  }

  /** Primary "Continue" (discard changes) button. */
  public Locator continueButton() {
    return locator(ROOT + " #unsaved-changes-modal-continue-button");
  }

  /** Secondary "Cancel" (keep editing) button. */
  public Locator cancelButton() {
    return locator(ROOT + " #unsaved-changes-modal-cancel-button");
  }

  /**
   * Best-effort dismissal used in {@code @After} cleanup. Returns immediately if the modal isn't
   * open, so callers can invoke this unconditionally without guarding for state.
   */
  public void continueIfOpen() {
    if (container().count() > 0 && container().first().isVisible()) {
      continueButton().click();
    }
  }

  /**
   * Wait up to {@code timeoutMs} for the modal to appear, then click Continue if it does. Used by
   * flows (e.g. logout) where the modal appears asynchronously after a user action and we don't
   * want to fail the test if it never appears (no unsaved changes ⇒ no modal).
   */
  public void dismissIfAppearsWithin(long timeoutMs) {
    try {
      container().waitFor(new Locator.WaitForOptions()
          .setState(WaitForSelectorState.VISIBLE)
          .setTimeout(timeoutMs));
      continueButton().click();
    }
    catch (TimeoutError ignored) {
      // Modal did not appear within the timeout; nothing to dismiss. Anything other than a
      // TimeoutError (browser crash, page closed, etc.) should propagate.
    }
  }
}
