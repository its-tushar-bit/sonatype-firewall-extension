/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class PolicyTile
    extends OwnerTile
{
  private static final String POLICIES_TABLE = ".nx-tile-subsection .nx-table";

  private static final String OWNER_POLICIES = ".nx-tile-subsection .nx-table tbody.iq-policy-table";

  private static final String TABLE_HEADER_COLUMNS = ".nx-tile-subsection .nx-table thead th";

  private static final String INHERITED_POLICIES_LIST = ".iq-policy-table-inherited-section";

  public PolicyTile() {
    super("#owner-pill-policy");
  }

  public static WebElementCondition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static WebElementCondition noActionText() {
    return Condition.text("—");
  }

  public SelenideElement addPolicyButton() {
    return $("#add-policy-button");
  }

  public ElementsCollection policyLists() {
    return children(OWNER_POLICIES);
  }

  public ElementsCollection headerColumns() {
    return children(TABLE_HEADER_COLUMNS);
  }

  public ElementsCollection inheritedPolicyLists() {
    return children(OWNER_POLICIES + INHERITED_POLICIES_LIST);
  }

  public PolicyTileList policyList(int num) {
    return new PolicyTileList(selector, OWNER_POLICIES + ":nth-of-type(" + (num + 1) + ")");
  }

  public PolicyTileList policyTileTable() {
    return new PolicyTileList(selector, POLICIES_TABLE);
  }

  public SelenideElement localPolicy(String policyName) {
    return children("table tr > .nx-cell:nth-of-type(2)").findBy(text(policyName));
  }

  public PolicyTileList localPolicyList() {
    return policyList(0);
  }

  public SelenideElement localEmptyDescriptor() {
    return child(".nx-list__item--empty");
  }

  public SelenideElement policyOverrideAsterisk() {
    return children("table tr > .nx-cell:nth-of-type(2) span").findBy(text("*"));
  }

  @Override
  public SelenideElement subHeader() {
    return child(".nx-tile-header__subtitle");
  }
}
