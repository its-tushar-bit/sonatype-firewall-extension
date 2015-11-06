/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class AssociationEditor
{
  private SelenideElement root;
  
  private static final String ROOT_SELECTOR = ".association-editor-wrapper";

  public AssociationEditor(SelenideElement root) {
    this.root = root.$(ROOT_SELECTOR);
  }

  public SelenideElement root() {
    return root;
  }

  public ElementsCollection rows() {
    return root.$$(".association-editor-row");
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

    public SelenideElement checkBox() {
      return root.$("input[type='checkbox']");
    }

    public SelenideElement icon() {
      return root.$(".fa, .hexagon");
    }

    public SelenideElement description() {
      return root.$(".description");
    }
  }
}
