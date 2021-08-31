/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxRadio;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AddWaiverPopover
    extends BasicElement<AddWaiverPopover>
{
  public static final String ROOT = "#add-waiver-popover";

  public AddWaiverPopover() {
    super(ROOT);
  }

  public SelenideElement artifactName() {
    return child(".iq-add-waiver-form__component .nx-h2 span");
  }

  public SelenideElement componentName() {
    return child(".iq-add-waiver-form__component .nx-tile-header__subtitle");
  }

  public SelenideElement policyName() {
    return child(".iq-add-waiver-form__policy .iq-threat-level");
  }

  public SelenideElement constraintName() {
    return child(".iq-add-waiver-form__constraint .iq-read-only-data");
  }

  public ElementsCollection conditions() {
    return children(".iq-add-waiver-form__conditions .iq-read-only-data span");
  }

  public SelenideElement condition(int index) {
    return child(".iq-add-waiver-form__conditions .iq-read-only-data span", nthChild(index));
  }

  public ElementsCollection availableScopes() {
    return children(".iq-add-waiver-form__scope .nx-radio");
  }

  public NxRadio scope(int index) {
    return new NxRadio(this.availableScopes().get(index));
  }

  public ElementsCollection availableComponents() {
    return children(".iq-add-waiver-form__components .nx-radio");
  }

  public NxRadio component(int index) {
    return new NxRadio(this.availableComponents().get(index));
  }

  public SelenideElement comments() {
    return child(".iq-add-waiver-form__comments .nx-text-input textarea");
  }

  public Button saveButton() {
    return new Button("#add-waiver-submit");
  }

  public Button cancelButton() {
    return new Button("#add-waiver-cancel");
  }

  public SelenideElement submitError() {
    return child(".nx-footer .nx-alert");
  }

  public SelenideElement getCloseButton() {
    return child("#add=waiver=popover-close-button");
  }
}
