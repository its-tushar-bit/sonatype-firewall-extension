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

public class PolicyTile
    extends OwnerTile
{
  private static final String OWNER_POLICY_LIST = "owner-policy-list";

  public PolicyTile() {
    super("#owner-pill-policy");
  }

  public static Condition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static Condition subHeaderText(String ownerName) {
    return Condition.text("applying to " + ownerName);
  }

  public static Condition noActionText() {
    return Condition.text("no action");
  }

  public SelenideElement addPolicyButton() {
    return $("#add-policy-button");
  }

  public ElementsCollection policyLists() {
    return children(OWNER_POLICY_LIST);
  }

  public PolicyTileList policyList(int num) {
    return new PolicyTileList(selector, OWNER_POLICY_LIST, nthChild(num + 1));
  }

  public SelenideElement localPolicy(String policyName) {
    return children("table tr > .policy-list__name-column").findBy(text(policyName));
  }

  public SelenideElement violationGrandfathering() {
    return $("#violation-grandfathering div");
  }

  public SelenideElement monitoredStage() {
    return $("#continuous-monitoring div");
  }

  public SelenideElement proprietaryComponentMatchers() {
    return $("#proprietary-component-matchers div.title");
  }
}
