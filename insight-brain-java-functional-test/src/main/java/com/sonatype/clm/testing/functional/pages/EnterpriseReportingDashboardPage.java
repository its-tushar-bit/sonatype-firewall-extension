/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class EnterpriseReportingDashboardPage
    extends BasicElement<EnterpriseReportingDashboardPage>
{
  public static final String ROOT = "#enterprise-reporting-dashboard-page";

  public EnterpriseReportingDashboardPage() {
    super(ROOT);
  }

  public static String url(String id) {
    return BaseUrl.resolvePageUrl("/enterpriseReportingDashboard/{id}", id);
  }

  public static String groupUrl(String groupId, String id) {
    return BaseUrl.resolvePageUrl("/enterpriseReportingDashboard/{groupId}/{id}", groupId, id);
  }

  public SelenideElement enterpriseReportingNotEnabledAlert() {
    return child(".nx-alert--error");
  }

  public SelenideElement copyToClipboard() {
    return child(".iq-enterprise-reporting-support-info__btn");
  }

  public SelenideElement copySuccessMessage() {
    return child(".iq-enterprise-reporting-support-info__message");
  }

  public SelenideElement groupTitle() {
    return child(".nx-h1");
  }

  public ElementsCollection groupTabs() {
    return children(".nx-tab");
  }

  public SelenideElement navigationBar() {
    return child(".enterprise-reporting-dashboard__navigation-bar");
  }

  public NavigationListItem navigationListItem(String id) {
    return new NavigationListItem(childSelector(".enterprise-reporting-dashboard__link-item.item--" + id));
  }

  public EnterpriseRow enterpriseRow() {
    return new EnterpriseRow(childSelector(".enterprise-reporting-dashboard__navigation-links", nthChild(1)));
  }

  public DataInsightRow dataInsightRow() {
    return new DataInsightRow(childSelector(".enterprise-reporting-dashboard__navigation-links", nthChild(2)));
  }

  public static class EnterpriseRow
      extends BasicElement<EnterpriseRow>
  {
    public EnterpriseRow(String selector) {
      super(selector);
    }

    public SelenideElement enterpriseTitle() {
      return child(".nx-h3");
    }

    public ElementsCollection enterpriseLinks() {
      return children(".enterprise-reporting-dashboard__link-item");
    }

    public SelenideElement firstEnterpriseLink() {
      return child(".enterprise-reporting-dashboard__link-item", nthChild(1));
    }
  }

  public static class DataInsightRow
      extends BasicElement<DataInsightRow>
  {
    public DataInsightRow(String selector) {
      super(selector);
    }

    public SelenideElement dataInsightTitle() {
      return child(".nx-h3");
    }

    public ElementsCollection dataInsightLinks() {
      return children(".enterprise-reporting-dashboard__link-item");
    }
  }

  public static class NavigationListItem
      extends BasicElement<NavigationListItem>
  {
    public NavigationListItem(String selector) {
      super(selector);
    }

    public SelenideElement activeLink() {
      return child(".nx-text-link");
    }

    public SelenideElement currentPageLink() {
      return child("span");
    }
  }
}
