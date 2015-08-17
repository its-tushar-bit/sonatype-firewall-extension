/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import java.util.List;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.hasAttribute;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class OwnerTreeView
{
  public static final Condition NODE_SELECTED_CLASS = cssClass("selected");

  public static SelenideElement filter() {
    return $(".tree-view-filter input");
  }

  public static ElementsCollection organizationElements() {
    return $$(".tree-view-organization-group");
  }

  public static List<OrganizationNode> organizations() {
    return Lists.transform(organizationElements(),
      new Function<SelenideElement, OrganizationNode>()
      {
        @Override
        public OrganizationNode apply(final SelenideElement element) {
          return new OrganizationNode(element);
        }
      }
    );
  }

  public static class RootOrganizationNode
  {
    public static SelenideElement treeViewElement() {
      return $(".tree-view-root-organization-group .tree-view-item");
    }

    public static SelenideElement newOrganizationButton() {
      return $(".tree-view-new-organization button");
    }
  }

  public static class OrganizationNode
  {
    private final SelenideElement element;

    public static final Condition CHILD_SELECTED_CLASS = cssClass("childSelected");
    public static final Condition COLLAPSE_CLASS = cssClass("collapse");
    public static final Condition EXPAND_CLASS = cssClass("expand");
    public static final Condition DISABLED_CLASS = cssClass("disabled");
    public static final String DISABLED_TOOLTIP_CONTENT = "You do not have permission to view this organization.";
    public static final Condition DISABLED_TOOLTIP_ATTRIBUTE = hasAttribute("data-tooltip", DISABLED_TOOLTIP_CONTENT);

    public OrganizationNode(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement treeViewElement() {
      return element.$(".tree-view-item:first-child");
    }

    public SelenideElement organizationName() {
      return element.$(".tree-view-item:first-child > span");
    }

    public SelenideElement twisty() {
      return element.$(".twisty");
    }

    public SelenideElement newApplicationButton() {
      return element.$(".tree-view-new-application button");
    }

    public ElementsCollection applicationElements() {
      return element.findAll(".tree-view-item:not(:first-child)");
    }

    public SelenideElement popup() {
      return $(".tooltip-inner");
    }

    public List<ApplicationNode> applications() {
      return Lists.transform(applicationElements(), new Function<SelenideElement, ApplicationNode>()
      {
        @Override
        public ApplicationNode apply(final SelenideElement applicationElement) {
          return new ApplicationNode(applicationElement);
        }
      });
    }
  }

  public static class ApplicationNode
  {
    private final SelenideElement element;

    public ApplicationNode(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement treeViewElement() {
      return element;
    }
  }
}
