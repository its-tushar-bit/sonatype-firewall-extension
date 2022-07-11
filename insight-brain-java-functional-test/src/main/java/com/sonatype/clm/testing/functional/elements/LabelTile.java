/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class LabelTile
    extends OwnerTile
{
  public LabelTile() {
    super("#owner-pill-comp-labels");
  }

  public static Condition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static Condition subHeaderText(String ownerName) {
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
