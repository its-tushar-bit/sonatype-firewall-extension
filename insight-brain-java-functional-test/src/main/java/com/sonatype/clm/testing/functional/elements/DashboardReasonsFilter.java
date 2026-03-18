/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardReasonsFilter
    extends BasicElement<DashboardReasonsFilter>
{
  public DashboardReasonsFilter(final String selector) {
    super(selector);
  }

  public ElementsCollection getLabels() {
    return this.element.findAll(By.tagName("label"));
  }

  public SelenideElement twisty() {
    return child(".nx-collapsible-items__trigger");
  }

  public SelenideElement acknowledgedViolationRiskCheck() {
    return child(".reason-9b704ef5bc064fc29d7fe08a251ee9a6");
  }

  public NxCheckbox checkboxItem(int index) {
    return new NxCheckbox(child(".nx-collapsible-items__children .nx-collapsible-items__child.nx-checkbox",
        nthChild(index)));
  }
}
