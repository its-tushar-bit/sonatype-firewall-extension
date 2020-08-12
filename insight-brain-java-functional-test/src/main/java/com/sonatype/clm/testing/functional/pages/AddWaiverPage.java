/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.ReactTextInput;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AddWaiverPage
    extends BasicElement<AddWaiverPage>
{
  public static final String ROOT = "#add-waiver-page";

  public static String url(String violationId) {
    return BaseUrl.resolvePageUrl("/addWaiver/{id}", violationId);
  }

  public AddWaiverPage() {
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
    return children(".iq-add-waiver-form__conditions span");
  }

  public SelenideElement condition(int index) {
    return child(".iq-add-waiver-form__conditions span", nthChild(index));
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

  public ReactTextInput comments() {
    return new ReactTextInput(child(".iq-add-waiver-form__comments .nx-text-input"));
  }

  public Button saveButton() {
    return new Button("#add-waiver-submit");
  }

  public SelenideElement submitError() {
    return child(".nx-btn-bar .nx-alert");
  }
}
