/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class TransitiveViolationsPage
    extends BasicElement<TransitiveViolationsPage>
{
  private static final String ROOT = "#transitive-violations-page";

  public static String url(String publicAppId, String scanId, String componentHash) {
    return BaseUrl.resolvePageUrl(
        String.format("/application/%s/%s/component/%s/transitiveViolations", publicAppId, scanId, componentHash));
  }

  public TransitiveViolationsPage() {
    super(ROOT);
  }

  public NxBackButton backButton() {
    return new NxBackButton("#menu-bar__back-button-container");
  }

  public ComponentDetailsHeader title() {
    return new ComponentDetailsHeader(childSelector(".component-details-header"));
  }

  public static class ComponentDetailsHeader
      extends BasicElement<ComponentDetailsPage.ComponentDetailsHeader>
  {
    private ComponentDetailsHeader(String selector) {
      super(selector);
    }

    public SelenideElement title() {
      return child("#transitive-violations-page-title");
    }

    public ElementsCollection reportInformationElements() {
      SelenideElement child = child(".component-details-header__reportinfo");
      return child.findAll(".component-details-header__reportinfo-item");
    }

    public ElementsCollection tags() {
      SelenideElement child = child(".component-details-header__tags");
      return child.findAll("label");
    }
  }

  public SelenideElement requestWaiveTransitiveViolations() {
    return child("#transitive-violations-page-request-waive");
  }

  public SelenideElement waiveTransitiveViolations() {
    return child("#transitive-violations-page-waive");
  }

  public SelenideElement viewTransitiveViolationWaivers() {
    return child("#transitive-violations-page-view-waivers");
  }

  public TransitiveViolationsTable transitiveViolationsTable() {
    return new TransitiveViolationsTable();
  }

  public static class TransitiveViolationsTable
      extends BasicElement<TransitiveViolationsTable>
  {
    static final String ROW_SELECTOR = "tbody .nx-table-row";

    TransitiveViolationsTable() {
      super(ROOT);
    }

    public SelenideElement threatHeader() {
      return child("#iq-transitive-violations-page-threat-level");
    }

    public SelenideElement policyAndActionHeader() {
      return child("#iq-transitive-violations-page-policy-name");
    }

    public SelenideElement componentHeader() {
      return child("#iq-transitive-violations-page-display-name");
    }

    public SelenideElement policyNameFilter() {
      return child("#iq-transitive-violations-page-policy-name-filter");
    }

    public SelenideElement componentNameFilter() {
      return child("#iq-transitive-violations-page-display-name-filter");
    }

    public ElementsCollection rows() {
      return children(ROW_SELECTOR);
    }

    public TransitiveViolationsRow row(int i) {
      return new TransitiveViolationsRow(childSelector(ROW_SELECTOR, nthChild(i)));
    }
  }

  public static class TransitiveViolationsRow
      extends BasicElement<TransitiveViolationsRow>
  {
    public TransitiveViolationsRow(String selector) {
      super(selector);
    }

    public SelenideElement threat() {
      return child(".nx-cell", nthChild(1));
    }

    public SelenideElement policyAndAction() {
      return child(".nx-cell", nthChild(2));
    }

    public SelenideElement component() {
      return child(".nx-cell", nthChild(3));
    }
  }
}
