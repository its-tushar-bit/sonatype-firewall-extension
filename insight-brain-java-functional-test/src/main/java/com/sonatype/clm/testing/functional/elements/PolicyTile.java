/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class PolicyTile
    extends OwnerTile
{
  private static final String POLICY_HIERARCHY = ".simple-list:not(#continuous-monitoring)";

  private static final String ROOT_ID = "#owner-pill-policy";

  public PolicyTile() {
    super($(ROOT_ID));
  }

  public static Condition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static Condition subHeaderText(String ownerName) {
    return Condition.text("applying to " + ownerName);
  }

  public ElementsCollection policyLists() {
    return root.$$(POLICY_HIERARCHY);
  }

  public PolicyTileList policyList(int num) {
    return new PolicyTileList(ROOT_ID, POLICY_HIERARCHY, nthChild(num + 1));
  }

  public static Condition name() {
    return Condition.text("NAME");
  }

  public static Condition noAction() {
    return Condition.text("no action");
  }
}
