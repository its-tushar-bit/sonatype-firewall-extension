/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class MtiqHeaderDivergencesPage
    extends BasePage
{
  public MtiqHeaderDivergencesPage() {
    super();
  }

  public static String mailConfigUrl() {
    return "/assets/index.html#/mailConfig";
  }

  public Locator footer() {
    return byRole(AriaRole.CONTENTINFO);
  }

  /** Absent in MTIQ ({@code selectIsShowVersionEnabled=false}). */
  public Locator footerVersionText() {
    return footer().getByText(Pattern.compile("Release \\d.*"));
  }

  /** Absent in MTIQ ({@code selectIsShowNotificationMenuEnabled=false}). */
  public Locator notificationsMenuButton() {
    return byRole(AriaRole.BUTTON, "Notifications");
  }

  /** Scoped to {@code MAIN} region to avoid ambiguity with other buttons on the page. */
  public Locator deleteConfigButton() {
    return page.getByRole(AriaRole.MAIN)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete Configuration"));
  }

  /** Structural container with no accessible name — ID is the stable scope anchor. */
  public Locator deleteModal() {
    return locator("#mail-config-delete-modal");
  }

  /** {@code NxWarningAlert} has no ARIA role — CSS class is the only reliable selector. */
  public Locator deleteModalWarning() {
    return deleteModal().locator(".nx-alert--warning");
  }

  public Locator cancelDeleteButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"));
  }
}
