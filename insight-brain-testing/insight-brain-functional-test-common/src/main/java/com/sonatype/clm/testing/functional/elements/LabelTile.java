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

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class LabelTile
    extends OwnerTile
{
  public LabelTile() {
    super("#owner-pill-comp-labels");
  }

  public static WebElementCondition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static WebElementCondition subHeaderText(String ownerName) {
    return Condition.text("available to " + ownerName + " policies");
  }

  public SelenideElement addLabelButton() {
    return $("#add-label-button");
  }

  public ElementsCollection labelLists() {
    return children(".nx-list");
  }

  public NxList labelList(int num) {
    return new NxList(labelLists().get(num));
  }

  public ElementsCollection inheritedLabelsLists() {
    return children(".nx-collapsible-items__children dl");
  }

  public InheritedLabelsList inheritedLabelsList(String ownerId) {
    return new InheritedLabelsList("#component-labels-for-" + ownerId);
  }

  public static class InheritedLabelsList
      extends BasicElement<InheritedLabelsList>
  {
    public InheritedLabelsList(String... selectors) {
      super(selectors);
    }

    public ElementsCollection elements() {
      return children(".component-labels-element");
    }

    public InheritedLabel element(int num) {
      return new InheritedLabel(selector, ".component-labels-element", nthChild(num + 1));
    }
  }

  public static class InheritedLabel
      extends BasicElement<InheritedLabel>
  {
    public InheritedLabel(String... selectors) {
      super(selectors);
    }

    public SelenideElement icon() {
      return child(".nx-icon");
    }

    public SelenideElement label() {
      return child(".component-labels-label");
    }

    public SelenideElement description() {
      return child(".component-labels-description");
    }
  }

  public ElementsCollection labelListsSubheaders() {
    return children(".nx-h3");
  }

  public SelenideElement labelListSubheader(int num) {
    return this.labelListsSubheaders().get(num);
  }

  public SelenideElement localLabel(String labelName) {
    return children("ul .nx-list__text").findBy(text(labelName));
  }

  @Override
  public SelenideElement subHeader() {
    return child(".nx-tile-header__subtitle");
  }
}
