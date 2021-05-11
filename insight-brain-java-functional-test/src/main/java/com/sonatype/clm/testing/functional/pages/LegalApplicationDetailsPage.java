/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.pages.ComponentCopyrightDetailsPage.CopyrightOverview;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LegalApplicationDetailsPage
{
  private LegalApplicationDetailsPage() {}

  public static String urlToApplicationScope(String publicAppId, String stage) {
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/stage/%s",
        publicAppId, stage));
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static ComponentTable componentTable() {
    return new ComponentTable();
  }

  public static class ComponentTable
      extends BasicElement<CopyrightOverview>
  {
    private static final String ROWS_SELECTOR = "#legal-application-details-table > tbody";

    public ComponentTable() {
      super(ROWS_SELECTOR);
    }

    public ElementsCollection rows() {
      return getElement().findAll("tr.nx-clickable");
    }

    public SelenideElement componentNameFilter() {
      return $("#legal-application-component-filter");
    }

    public SelenideElement licenseFilter() {
      return $("#legal-application-license-filter");
    }

    public SelenideElement sortByComponent() {
      return $(".legal-application-details-table-component span.nx-cell__sort-icons");
    }

    public SelenideElement sortByLicenses() {
      return $(".legal-application-details-table-licenses span.nx-cell__sort-icons");
    }

    public SelenideElement sortByPercentage() {
      return $(".legal-application-details-table-review-progress span.nx-cell__sort-icons");
    }

    public SelenideElement sortByReviewStatus() {
      return $(".legal-application-details-table-review-status span.nx-cell__sort-icons");
    }

    public ElementsCollection componentNames() {
      return getElement().findAll("tr.nx-clickable td.legal-application-details-component-name");
    }

    public ElementsCollection licenses() {
      return getElement().findAll("tr.nx-clickable td.legal-application-details-licenses");
    }
  }
}
