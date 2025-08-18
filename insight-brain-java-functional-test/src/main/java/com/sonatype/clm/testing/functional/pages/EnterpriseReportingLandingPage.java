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

public class EnterpriseReportingLandingPage
    extends BasicElement<EnterpriseReportingLandingPage>
{
  public static final String ROOT = "#enterprise-reporting-landing-page";

  public EnterpriseReportingLandingPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/enterpriseReportingLandingPage");
  }

  public SelenideElement enterpriseReports() {
    return child("#enterprise-reporting-dashboards--enterprise");
  }

  public SelenideElement insightsReports() {
    return child("#enterprise-reporting-dashboards--insights");
  }

  public SelenideElement statusIndicator() {
    return child(".nx-status-indicator");
  }

  public SelenideElement enterpriseReportingNotEnabledError() {
    return child(".nx-alert--error");
  }

  public SelenideElement heading() {
    return child("#enterprise-reporting-landing-page-title");
  }

  public SelenideElement description() {
    return child("#enterprise-reporting-landing-page-description");
  }

  public SelenideElement infoAlert() {
    return child(".nx-alert--info");
  }

  public SelenideElement copyToClipboard() {
    return child(".iq-enterprise-reporting-support-info__btn");
  }

  public SelenideElement copySuccessMessage() {
    return child(".iq-enterprise-reporting-support-info__message");
  }

  public SelenideElement helpLink() {
    return child(".iq-enterprise-reporting-support-info__link");
  }

  public SelenideElement contactUs() {
    return child(".iq-enterprise-reporting__contactus");
  }

  public SelenideElement contactUsHeading() {
    return child(".iq-enterprise-reporting__header--contact");
  }

  public DashboardCard dashboardAt(String id) {
    return new DashboardCard(childSelector("#enterprise-reporting-dashboard-" + id));
  }

  public ContactCard contactCard(int index) {
    return new ContactCard(childSelector(".iq-enterprise-reporting-card--contact", nthChild(index)));
  }

  public static class DashboardCard
      extends BasicElement<DashboardCard>
  {
    public DashboardCard(String selector) {
      super(selector);
    }

    public SelenideElement icon() {
      return child(".nx-icon");
    }

    public SelenideElement spotlight() {
      return child(".iq-enterprise-reporting-card__spotlight");
    }

    public SelenideElement dashboardTitle() {
      return child(".iq-enterprise-reporting-card__header .nx-h3");
    }

    public SelenideElement dashboardDescription() {
      return child(".nx-card__text");
    }

    public ElementsCollection featureText() {
      return children(".nx-list__item .nx-list__text");
    }

    public SelenideElement dashboardButton() {
      return child(".iq-enterprise-reporting-card__button");
    }

    public SelenideElement dashboardGroupButton() {
      return child(".nx-segmented-btn .nx-segmented-btn__main-btn");
    }

    public SelenideElement dashboardOpenDropdownButton() {
      return child(".nx-segmented-btn .nx-segmented-btn__dropdown-btn");
    }

    public SelenideElement dashboardDropdownItemButton() {
      return child(".nx-dropdown-menu .nx-dropdown-button", nthChild(1));
    }
  }

  public static class ContactCard
      extends BasicElement<ContactCard>
  {
    public ContactCard(String selector) {
      super(selector);
    }

    public SelenideElement contactTitle() {
      return child(".nx-h3");
    }

    public SelenideElement contactDescription() {
      return child(".nx-card__text");
    }

    public SelenideElement contactButton() {
      return child(".iq-enterprise-reporting-card__button");
    }
  }
}
