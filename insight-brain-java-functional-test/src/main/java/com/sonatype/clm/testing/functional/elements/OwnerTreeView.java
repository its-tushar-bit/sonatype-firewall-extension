/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import java.util.List;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class OwnerTreeView
{
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

  public static class OrganizationNode
  {
    private final SelenideElement element;

    public static final String COLLAPSE_CLASS = "collapse";
    public static final String EXPAND_CLASS = "expand";
    public static final String SELECTED_CLASS = "selected";

    public OrganizationNode(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement treeViewElement() {
      return element.$(".tree-view-item:first-child");
    }

    public SelenideElement twisty() {
      return element.$(".twisty");
    }

    public SelenideElement newApplicationButton() {
      return $(".tree-view-new-application");
    }

    public ElementsCollection applicationElements() {
      return element.findAll(".tree-view-item:not(:first-child)");
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

    public static final String SELECTED_CLASS = "selected";

    public ApplicationNode(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement treeViewElement() {
      return element;
    }
  }
}
