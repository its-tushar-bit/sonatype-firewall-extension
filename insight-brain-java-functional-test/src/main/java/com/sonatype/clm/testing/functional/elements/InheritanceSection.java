/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Selenide.$;

public class InheritanceSection
    extends PolicyEditorSection
{
  public static final Condition ALL_TEXT_ROOT_ORG = Condition.text("All Applications and Repositories");
  public InheritanceSection() {
    super($("#policy-edit-inheritance"));
  }

  public static Condition allRadioText(String ownerName) {
    return Condition.text("All Applications in " + ownerName);
  }

  public static Condition specifiedRadioText(String ownerName) {
    return Condition.text("Applications of the specified Application Categories in " + ownerName);
  }

  public Radio allChildrenInheritRadio() {
    return new Radio(root.$$("#editor-policy-inherit label").get(0));
  }

  public Radio specifiedChildrenInheritRadio() {
    return new Radio(root.$$("#editor-policy-inherit label").get(1));
  }

  public AssociationEditor associationEditor() {
    return new AssociationEditor(root);
  }
}
