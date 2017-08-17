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
    return $(".owner-tree-view__filter input");
  }

  public static ElementsCollection organizationElements() {
    return $$("#owner-tree-view-owner-rows > .iq-tree-view");
  }

  public static OrganizationNode organization(int num) {
    int cssNum = num + 1;

    return new OrganizationNode("#owner-tree-view-owner-rows > .iq-tree-view:nth-of-type(" + cssNum + ")");
  }

  public static SelenideElement repositories() {
    return $("#owner-tree-view-repositories-row");
  }

  public static class RootOrganizationNode
  {
    public static SelenideElement treeViewElement() {
      return $("#owner-tree-view-root-org-row");
    }

    public static SelenideElement newOrganizationButton() {
      return $("#owner-tree-view-root-org-row button");
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
      return child(".iq-tree-view__trigger");
    }

    public SelenideElement organizationName() {
      return child(".iq-tree-view__trigger .iq-tree-view__text");
    }

    public SelenideElement twisty() {
      return child(".iq-tree-view__trigger .iq-tree-view__twisty-icon");
    }

    public SelenideElement newApplicationButton() {
      return child(".owner-tree-view__new-application button");
    }

    public ElementsCollection applicationElements() {
      return children(".iq-tree-view__child");
    }

    public SelenideElement popup() {
      return $(".tooltip-inner");
    }

    public static ApplicationNode application(int num) {
      int cssNum = num + 1;
      return new ApplicationNode("#owner-tree-view-owner-rows > .iq-tree-view .iq-tree-view__child", nthChild(cssNum));
    }

    public static class ApplicationNode
        extends BasicElement<ApplicationNode>
    {

      public ApplicationNode(String... selectors) {
        super(selectors);
      }

      public SelenideElement applicationName() {
        return child("> .iq-tree-view__text");
      }
    }

  }
}
