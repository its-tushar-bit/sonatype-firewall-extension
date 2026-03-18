/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$x;

public class SummarySection
    extends BasicElement<SummarySection>
{
  private static final String ROOT = "#policy-edit-summary";

  public SummarySection() {
    super(ROOT);
  }

  public SelenideElement title() {
    return child("h2");
  }

  public NxTextInput policyName() {
    return new NxTextInput(child("#editor-policy-name"));
  }

  public NxCheckbox legacyViolationCheckbox() {
    return new NxCheckbox(child("#editor-legacy-violation-checkbox"));
  }

  public SelenideElement threatLevel() {
    return child("#editor-policy-threat-level .nx-btn");
  }

  public SelenideElement legacyViolationTitle() {
    return $x("//legend[span[text()='Legacy Violations']]");
  }
}
