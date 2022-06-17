/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class PolicyInheritsToSection
    extends BasicElement<PolicyInheritsToSection>
{
  public static final Condition ALL_TEXT_ROOT_ORG = Condition.text("All Applications and Repositories");

  private static final String ROOT = "#policy-edit-inheritance";

  public PolicyInheritsToSection() {
    super(ROOT);
  }

  public static Condition allRadioText(String ownerName) {
    if ("Root Organization".equals(ownerName)) {
      return Condition.text("All Applications and Repositories");
    }

    return Condition.text("All Applications in " + ownerName);
  }

  public static Condition specifiedRadioText(String ownerName) {
    return Condition.text("Applications of the specified Application Categories in " + ownerName);
  }

  public IqRadio allChildrenInheritRadio() {
    return new IqRadio(child("#editor-policy-inherit iq-radio", nthChild(1)));
  }

  public IqRadio specifiedChildrenInheritRadio() {
    return new IqRadio(child("#editor-policy-inherit iq-radio", nthChild(2)));
  }

  public AssociationEditor associationEditor() {
    return new AssociationEditor(ROOT);
  }

  public IqCheckbox policyActionsOverrideCheckbox() {
    return new IqCheckbox(child("#editor-policy-actions-override"));
  }
}
