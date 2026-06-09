/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/** Shared role-option constants. Callers must not mutate these instances. */
public final class CommonButtonOptions
{
  private CommonButtonOptions() {
  }

  public static final Locator.GetByRoleOptions CANCEL_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Cancel");

  public static final Locator.GetByRoleOptions CLOSE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Close");

  public static final Locator.GetByRoleOptions SAVE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Save");

  public static final Locator.GetByRoleOptions BACK_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Back");

  public static final Locator.GetByRoleOptions NEXT_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Next");

  public static final Locator.GetByRoleOptions DELETE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Delete");

  public static final Locator.GetByRoleOptions FILTER_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Filter");

  public static final Locator.GetByRoleOptions RETRY_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Retry");
}
