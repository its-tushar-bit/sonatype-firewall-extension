/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Per-tab helpers on top of {@link ApplicationReportPage}. The Policy tab is the default route
 * at {@code /applicationReport/<appPublicId>/<scanId>/policy}.
 */
public class ApplicationReportPolicyTabPage
    extends ApplicationReportPage
{
  public ApplicationReportPolicyTabPage() {
    super();
  }

  public Locator waivedFilterOption() {
    return filterPopover().getByText("Waived", new Locator.GetByTextOptions().setExact(true));
  }

  public Locator legacyFilterOption() {
    return filterPopover().getByText("Legacy", new Locator.GetByTextOptions().setExact(true));
  }

  public Locator violationStateSectionTrigger() {
    return violationStateFilter().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Violation State"));
  }

  /** Section is collapsed by default; pair with the matching assertion to verify expansion. */
  public void expandViolationStateSection() {
    violationStateSectionTrigger().click();
  }
}
