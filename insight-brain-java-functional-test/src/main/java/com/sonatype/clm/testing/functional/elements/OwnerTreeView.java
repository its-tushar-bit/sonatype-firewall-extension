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

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class OwnerTreeView
{
  public static SelenideElement filter() {
    return $(".tree-view-filter input");
  }

  public static ElementsCollection organizationElements() {
    return $$(".tree-view-organization-group");
  }

  public static OrganizationNode organization(int num) {
    return new OrganizationNode("#organizations .tree-view-organization-group", nthChild(num + 1));
  }

  public static SelenideElement repositories() {
    return $("#tree-view-repositories-group .tree-view-item");
  }

  public static class RootOrganizationNode
  {
    public static SelenideElement treeViewElement() {
      return $("#tree-view-root-organization-group .tree-view-item");
    }

    public static SelenideElement newOrganizationButton() {
      return $(".tree-view-new-organization button");
    }
  }

  public static class OrganizationNode
      extends BasicElement<OrganizationNode>
  {
    public static final Condition CHILD_SELECTED = cssClass("child-selected");

    public static final String DISABLED_TOOLTIP_CONTENT = "You do not have permission to view this organization.";

    public static final Condition DISABLED_TOOLTIP_ATTRIBUTE = attribute("data-tooltip", DISABLED_TOOLTIP_CONTENT);

    public OrganizationNode(String... selectors) {
      super(selectors);
    }

    public SelenideElement treeViewElement() {
      return child(".tree-view-item:first-child");
    }

    public SelenideElement organizationName() {
      return child(".tree-view-item:first-child > span");
    }

    public SelenideElement twisty() {
      return child(".twisty");
    }

    public SelenideElement newApplicationButton() {
      return child(".tree-view-new-application button");
    }

    public ElementsCollection applicationElements() {
      return children(".tree-view-item:not(:first-child)");
    }

    public SelenideElement popup() {
      return $(".tooltip-inner");
    }

    public static ApplicationNode application(int num) {
      return new ApplicationNode(".tree-view-item", nthChild(num + 1 + 1 /* The org is the first entry */));
    }
    
    public static class ApplicationNode
        extends BasicElement<ApplicationNode>
    {

      public ApplicationNode(String... selectors) {
        super(selectors);
      }

      public SelenideElement applicationName() {
        return child("> span");
      }
    }

  }
}
