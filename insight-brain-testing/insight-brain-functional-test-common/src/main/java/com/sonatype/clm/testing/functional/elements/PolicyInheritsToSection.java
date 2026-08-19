/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Selenide.$;

public class PolicyInheritsToSection
    extends BasicElement<PolicyInheritsToSection>
{
  public static final WebElementCondition ALL_TEXT_ROOT_ORG = Condition.text("All Applications and Repositories");

  private static final String ROOT = "#policy-edit-inheritance";

  public PolicyInheritsToSection() {
    super(ROOT);
  }

  public SelenideElement header() {
    return $(ROOT + " .nx-h2");
  }

  public static WebElementCondition allRadioText(String ownerName) {
    if ("Root Organization".equals(ownerName)) {
      return Condition.text("All Applications and Repositories");
    }

    return Condition.text("All Applications in " + ownerName);
  }

  public static WebElementCondition allRadioTextSbomManager(String ownerName) {
    if ("Root Organization".equals(ownerName)) {
      return Condition.text("All Applications");
    }

    return Condition.text("All Applications in " + ownerName);
  }

  public static WebElementCondition specifiedRadioText(String ownerName) {
    return Condition.text("Applications of the specified Application Categories in " + ownerName);
  }

  public ElementsCollection allInheritRadios() {
    return children("#editor-policy-inherit .nx-radio");
  }

  public NxRadio allChildrenInheritRadio() {
    return new NxRadio(allInheritRadios().get(0));
  }

  public NxRadio specifiedChildrenInheritRadio() {
    return new NxRadio(allInheritRadios().get(1));
  }

  public IqAssociationEditor associationEditor() {
    return new IqAssociationEditor(ROOT);
  }

  public NxCheckbox policyActionsOverrideCheckbox() {
    return new NxCheckbox($("#editor-policy-actions-override"));
  }

  public NxCheckbox policyNotificationsOverrideCheckbox() {
    return new NxCheckbox($("#editor-policy-notifications-override"));
  }

  public static class OverridesConfirmationModal
      extends BasicElement<OverridesConfirmationModal>
  {
    public OverridesConfirmationModal() {
      super("#policy-overrides-confirmation-modal");
    }

    public SelenideElement header() {
      return child(".nx-modal-header");
    }

    public SelenideElement continueButton() {
      return child(".nx-footer", ".nx-btn--primary");
    }

    public SelenideElement cancelButton() {
      return child(".nx-form__cancel-btn");
    }
  }
}
