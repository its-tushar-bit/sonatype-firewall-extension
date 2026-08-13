/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.AccessTileList.AccessTileListElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AccessTile
    extends OwnerTile
{
  public AccessTile(String root) {
    super(root);
  }

  public static WebElementCondition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static WebElementCondition subHeaderText(String ownerName) {
    return Condition.text(ownerName + " users by role");
  }

  @Override
  public SelenideElement addRoleButton() {
    return $("#add-role-button");
  }

  private String accessListSelector() {
    return createSelector(selector, ".nx-tile-content section");
  }

  public ElementsCollection accessLists() {
    return $$(accessListSelector());
  }

  public AccessTileList accessList(int num) {
    return new AccessTileList(accessListSelector(), nthChild(num + 1));
  }

  public SelenideElement localAccessRole(String roleName) {
    return children("#iq-access-tile-local-access-list .nx-list__item").findBy(text(roleName));
  }

  public ElementsCollection inheritedAccessLists() {
    return children(".nx-collapsible-items__children dl");
  }

  public InheritedAccessList inheritedAccessList(String ownerId) {
    return new InheritedAccessList("#access-for-" + ownerId);
  }

  public static class InheritedAccessList
      extends BasicElement<InheritedAccessList>
  {
    public InheritedAccessList(String... selectors) {
      super(selectors);
    }

    public ElementsCollection elements() {
      return children(".access-element");
    }

    public InheritedAccess element(int num) {
      return new InheritedAccess(selector, ".access-element", nthChild(num + 1));
    }
  }

  public static class InheritedAccess
      extends BasicElement<InheritedAccess>
  {
    public InheritedAccess(String... selectors) {
      super(selectors);
    }

    public SelenideElement label() {
      return child(".access-label");
    }

    public AccessTileListElement description() {
      return new AccessTileListElement(selector, ".access-description");
    }
  }

  public SelenideElement accessListSubheader(int num) {
    return this.accessListsSubheaders().get(num + 1);
  }

  public ElementsCollection accessListsSubheaders() {
    return children(".nx-h3");
  }
}
