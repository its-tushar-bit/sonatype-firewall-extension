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

public class AssociationEditor
    extends BasicElement<AssociationEditor>
{
  private static final String ROOT_SELECTOR = ".association-editor";

  // The CSS class present when the editor is using CSS multi-column display
  public static final Condition MULTI_COLUMN = Condition.cssClass("association-editor--multi-column");

  public AssociationEditor(String selector) {
    super(selector, ROOT_SELECTOR);
  }

  public ElementsCollection rows() {
    return children("> *");
  }

  public AssociationEditorElement item(int num) {
    return new AssociationEditorElement(rows().get(num));
  }

  public static class AssociationEditorElement
  {
    public final SelenideElement root;

    public AssociationEditorElement(SelenideElement root) {
      this.root = root;
    }

    public IqCheckbox checkBox() {
      return new IqCheckbox(root);
    }

    public SelenideElement icon() {
      return root.$(".fa, .hexagon");
    }

    public SelenideElement description() {
      return root.$(".description");
    }
  }
}
