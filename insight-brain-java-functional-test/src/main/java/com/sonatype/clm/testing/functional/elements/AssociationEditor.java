/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class AssociationEditor
    extends BasicElement<AssociationEditor>
{
  private static final String ROOT_SELECTOR = ".association-editor-wrapper";

  public AssociationEditor(String selector) {
    super(selector, ROOT_SELECTOR);
  }

  public ElementsCollection rows() {
    return children(".association-editor-row");
  }

  public int columnCount() {
    if (rows().size() > 0) {
      return rows().get(0).$$("td").size();
    }
    return 0;
  }

  public AssociationEditorElement item(int num, int column) {
    return new AssociationEditorElement(rows().get(num), column);
  }

  public static class AssociationEditorElement
  {
    public SelenideElement root;

    public AssociationEditorElement(SelenideElement root, int column) {
      this.root = column == 0 ? root.$("td:first-child") : root.$("td:last-child");
    }

    public IqCheckbox checkBox() {
      return new IqCheckbox(root.$("iq-checkbox"));
    }

    public SelenideElement icon() {
      return root.$(".fa, .hexagon");
    }

    public SelenideElement description() {
      return root.$(".description");
    }
  }
}
