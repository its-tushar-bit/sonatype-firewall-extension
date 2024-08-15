/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class OwnerTreeView
{
  public static SelenideElement filter() {
    return $(".iq-owner-tree-view__filter input");
  }

  public static SelenideElement filterMinCharsMessage() {
    return $(".iq-owner-tree-view__min-filter-message");
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
      return $(".iq-owner-tree-view__new-organization button");
    }
  }

  public static class OrganizationNode
      extends BasicElement<OrganizationNode>
  {
    public static final WebElementCondition CHILD_SELECTED = cssClass("child-selected");

    public static final String DISABLED_TOOLTIP_CONTENT = "You do not have permission to view this organization.";

    public static final WebElementCondition DISABLED_TOOLTIP_ATTRIBUTE =
        attribute("tooltip-text", DISABLED_TOOLTIP_CONTENT);

    public OrganizationNode(String... selectors) {
      super(selectors);
    }

    public SelenideElement treeViewElement() {
      return child(".iq-tree-view__trigger");
    }

    public SelenideElement organizationName() {
      return child(".iq-tree-view__trigger");
    }

    public SelenideElement twisty() {
      return child(".iq-tree-view__trigger .iq-tree-view__twisty-icon");
    }

    public SelenideElement newApplicationButton() {
      return child(".iq-owner-tree-view__new-application button");
    }

    public SelenideElement importApplicationsButton() {
      return child(".iq-owner-tree-view__import-applications a");
    }

    public ElementsCollection applicationElements() {
      return children(".iq-tree-view__child");
    }

    public SelenideElement application(int num) {
      int cssNum = num + 1;
      return child(".iq-tree-view__child", nthChild(cssNum));
    }
  }
}
