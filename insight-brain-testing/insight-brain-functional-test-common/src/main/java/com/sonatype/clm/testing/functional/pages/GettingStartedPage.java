/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class GettingStartedPage
    extends BasicElement<GettingStartedPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/gettingStarted");
  }

  private static final String ROOT = "#getting-started";

  public GettingStartedPage() {
    super(ROOT);
  }

  public SelenideElement hdsConnectivityWarning() {
    return child("#hds-unreachable-warning");
  }

  public ProductLicenseSummaryTile productLicenseSummary() {
    return new ProductLicenseSummaryTile();
  }

  public SelenideElement systemSetup() {
    return child("#system-setup");
  }

  public SelenideElement learningTopics() {
    return child("#learning-topics");
  }

  public SelenideElement docLink(int index) {
    return child(nthChild(index + 1), ".nx-text-link");
  }

  public SelenideElement docLinkIcon(int index) {
    return child(nthChild(index + 1), ".nx-text-link svg");
  }

  public static class ProductLicenseSummaryTile
      extends BasicElement<ProductLicenseSummaryTile>
  {
    private static final String ROOT = "#product-license-summary";

    ProductLicenseSummaryTile() {
      super(ROOT);
    }

    public SelenideElement expiryDate() {
      return child("#license-expiry-date");
    }

    public SelenideElement daysToExpiration() {
      return child("#license-days-to-expiration");
    }

    public SelenideElement fingerprint() {
      return child("#license-fingerprint");
    }

    public ElementsCollection products() {
      return children("#license-products .nx-read-only__data");
    }

    public SelenideElement licensedSboms() {
      return $("#license-sbom-limit");
    }

    public ElementsCollection licensedDevelopersRows() {
      return children("#license-licensed-developers > div");
    }
  }
}
