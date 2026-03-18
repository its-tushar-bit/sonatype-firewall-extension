/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.NxTreeViewMultiSelect;
import com.sonatype.clm.testing.functional.pages.ComponentCopyrightDetailsPage.CopyrightOverview;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class LegalApplicationDetailsPage
{
  private static final String BASE_URL_CDP_ORIGIN = "/applicationReport/%s/%s";

  private LegalApplicationDetailsPage() {
  }

  public static String urlToApplicationScope(String publicAppId, String stage) {
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/stage/%s",
        publicAppId, stage));
  }

  public static String urlToApplicationScopeComingFromCDP(
      String publicAppId,
      String stage,
      String scanId,
      String componentHash)
  {
    return BaseUrl.resolvePageUrl(String.format(BASE_URL_CDP_ORIGIN, publicAppId, scanId) +
        String.format("/legal/application/%s/stage/%s", publicAppId, stage) +
        String.format("/component/%s?scanId=%s&tabId=legal", componentHash, scanId));
  }

  public static String urlToComponentAtRootScopeByHash(String componentHash) {
    return BaseUrl.resolvePageUrl(String.format("/legal/component/%s", componentHash));
  }

  public static String urlToComponentAtRootScopeByComponentIdentifier(String componentIdentifier) {
    return BaseUrl.resolvePageUrl(String.format("/legal/component/componentIdentifier/%s", componentIdentifier));
  }

  public static String urlToComponentAtApplicationScopeByComponentIdentifier(
      String componentIdentifier,
      String publicAppId,
      String hash,
      String scanId,
      String tabId)
  {
    // note can't use resolvePageUrl since it unescapes characters in a way that's not consistent wit the frontend,
    // making it unsuitable for test comparisons.
    return BaseUrl.pageUriBuilder()
        .fragment(
            String.format("/legal/component/componentIdentifier/%s/application/%s/component/%s/scan/%s/%s",
                componentIdentifier, publicAppId, hash, scanId, tabId))
        .toString();
  }

  public static String sbomManagerUrlToApplicationScope(String publicAppId) {
    return BaseUrl.resolvePageUrl(
        String.format("/sbomManager/legal/application/%s/stage/%s", publicAppId, StageTypes.COMPLIANCE.getId()));
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static SelenideElement filterButton() {
    return $("#filter-toggle");
  }

  public static SelenideElement filterContainer() {
    return $(".legal-application-details-filter");
  }

  public static ComponentTable componentTable() {
    return new ComponentTable();
  }

  public static LegalApplicationDetailsReviewStatusFilter reviewStatusFilter() {
    return new LegalApplicationDetailsReviewStatusFilter("#legal-progress-options-filter");
  }

  public static class LegalApplicationDetailsReviewStatusFilter
      extends NxTreeViewMultiSelect
  {
    public LegalApplicationDetailsReviewStatusFilter(final String selector) {
      super(selector);
    }

    public NxCheckbox noCategory() {
      return getFilterCheckboxAt(1);
    }

    public NxCheckbox getFilterCheckboxAt(int i) {
      return new NxCheckbox(child(".nx-collapsible-items__children .nx-collapsible-items__child", nthChild(i + 1)));
    }
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
