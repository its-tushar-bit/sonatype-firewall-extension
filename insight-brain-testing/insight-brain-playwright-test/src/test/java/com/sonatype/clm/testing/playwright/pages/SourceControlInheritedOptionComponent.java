/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

public class SourceControlInheritedOptionComponent
    extends BasePage
{
  public static final String PULL_REQUEST_COMMENTING = "source-control-pull-request-commenting";

  public static final String SOURCE_CONTROL_EVALUATIONS = "source-control-evaluations";

  public static final String AUTOMATED_COMMIT_FEEDBACK = "automated-commit-feedback";

  public static final int INHERIT_INDEX = 0;

  public static final int ENABLED_INDEX = 1;

  public static final int DISABLED_INDEX = 2;

  public SourceControlInheritedOptionComponent() {
    super();
  }

  public Locator fieldset(String optionId) {
    return locator("#" + optionId);
  }

  /** Radio {@code <input>}s in DOM order: Inherit(0) / Enabled(1) / Disabled(2). */
  public Locator radioInputs(String optionId) {
    return fieldset(optionId).locator(".nx-radio__input");
  }

  public Locator radioLabels(String optionId) {
    return fieldset(optionId).locator(".nx-radio-checkbox__content");
  }

  /** Click a radio option by its exact visible label text (NxRadio CSS-hides the input). */
  public void clickOption(String optionId, String labelText) {
    fieldset(optionId).locator(".nx-radio-checkbox__content")
        .filter(new Locator.FilterOptions().setHasText(labelText))
        .first()
        .click();
  }
}
