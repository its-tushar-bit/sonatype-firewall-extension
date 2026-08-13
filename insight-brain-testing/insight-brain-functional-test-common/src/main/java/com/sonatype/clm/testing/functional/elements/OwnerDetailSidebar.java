/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class OwnerDetailSidebar
{
  private static final String OWNER_DETAIL_SIDEBAR_SELECTOR = "#owner-detail-sidebar";

  public static SelenideElement root() {
    return $(OWNER_DETAIL_SIDEBAR_SELECTOR);
  }

  public static SelenideElement header() {
    return root().$("h3");
  }

  public static NxBackButton backLink() {
    return new NxBackButton();
  }

  public static OwnerDetailSidebarGroup applicationCategoryGroup() {
    return new OwnerDetailSidebarGroup("#application-category-group");
  }

  public static OwnerDetailSidebarGroup policyGroup() {
    return new OwnerDetailSidebarGroup("#policy-group");
  }

  public static OwnerDetailSidebarGroup componentLabelGroup() {
    return new OwnerDetailSidebarGroup("#label-group");
  }

  public static OwnerDetailSidebarGroup ltgGroup() {
    return new OwnerDetailSidebarGroup("#license-threat-group-group");
  }

  public static OwnerDetailSidebarGroup accessGroup() {
    return new OwnerDetailSidebarGroup("#access-group");
  }

  public static SelenideElement legacyViolations() {
    return $("#legacy-violations-link");
  }

  public static SelenideElement continuousMonitoring() {
    return $("#continous-monitoring-link");
  }

  public static SelenideElement publicDatasources() {
    return $("#public-data-sources-link");
  }

  public static SelenideElement proprietaryComponents() {
    return $("#proprietary-components-link");
  }

  public static class OwnerDetailSidebarGroup
      extends BasicElement<OwnerDetailSidebarGroup>
  {
    public OwnerDetailSidebarGroup(String... selectors) {
      super(selectors);
    }

    public SelenideElement title() {
      return child(".nx-collapsible-items__header");
    }

    public ElementsCollection items() {
      return children(".nx-collapsible-items__child");
    }

    public ElementsCollection entryItems() {
      return children(".nx-collapsible-items__child:nth-child(n+2)"); // skip 'Add ...' entries
    }

    public OwnerDetailSidebarItem item(int num) {
      return new OwnerDetailSidebarItem(childSelector(".nx-collapsible-items__child", nthChild(num + 1)));
    }

    public static class OwnerDetailSidebarItem
        extends BasicElement<OwnerDetailSidebarItem>
    {
      public OwnerDetailSidebarItem(String... selectors) {
        super(selectors);
      }
    }
  }
}
