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

public class IqAssociationEditor
    extends BasicElement<IqAssociationEditor>
{
  private static final String ROOT_SELECTOR = ".iq-association-editor";

  // The CSS class present when the editor is using CSS multi-column display
  public static final WebElementCondition MULTI_COLUMN = Condition.cssClass("iq-association-editor--multi-column");

  public IqAssociationEditor(String selector) {
    super(selector, ROOT_SELECTOR);
  }

  public ElementsCollection rows() {
    return children("> label");
  }

  public IqAssociationEditor.AssociationEditorElement item(int num) {
    return new IqAssociationEditor.AssociationEditorElement(rows().get(num));
  }

  public static class AssociationEditorElement
  {
    public final SelenideElement root;

    public AssociationEditorElement(SelenideElement root) {
      this.root = root;
    }

    public NxCheckbox checkBox() {
      return new NxCheckbox(root);
    }

    public SelenideElement icon() {
      return root.$(".fa, .hexagon");
    }

    public SelenideElement description() {
      return root.$(".iq-association-editor__description");
    }
  }
}
