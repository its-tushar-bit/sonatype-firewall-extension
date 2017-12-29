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
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class LicenseThreatGroupTile
    extends OwnerTile
{
  private static final String LTG_OWNER_ELEMENT_ID = "#owner-pill-ltgs";

  public LicenseThreatGroupTile() {
    super(LTG_OWNER_ELEMENT_ID);
  }

  public static Condition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public SelenideElement addLTGButton() {
    return $("#add-ltg-button");
  }

  public ElementsCollection ltgLists() {
    return children(".simple-list");
  }

  public ThreatGroupTileSimpleList ltgList(int num) {
    return new ThreatGroupTileSimpleList("#ltg-summary-hierarchy", ".simple-list", nthChild(num + 1));
  }

  public SelenideElement localLTG(String ltgName) {
    return localLTGs().findBy(text(ltgName));
  }

  public ElementsCollection localLTGs() {
    return children(".simple-list:first-child ul div.threat-group-title");
  }
}
