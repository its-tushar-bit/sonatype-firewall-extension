/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

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

  public SelenideElement policyName() {
    return child("#editor-policy-name");
  }

  public IqCheckbox policyViolationGrandfatheringCheckbox() {
    return new IqCheckbox(child("#editor-policy-violation-grandfathering"));
  }
}
