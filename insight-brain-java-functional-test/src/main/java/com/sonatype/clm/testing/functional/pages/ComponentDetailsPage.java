/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class ComponentDetailsPage
    extends BasicElement<ComponentDetailsPage>
{
  public static final String ROOT = "#component-details-page";

  private static final String BASE_URL = "/applicationReport/{applicationPublicId}/{scanId}/componentDetails/{hash}";

  public static String url(Application app, String scanId, String hash) {
    return urlToRemediation(app, scanId, hash);
  }

  public static String urlToRemediation(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/remediation", app.getPublicId(), scanId, hash);
  }

  public static String urlToComponentInfo(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/info", app.getPublicId(), scanId, hash);
  }

  public static String urlToViolations(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/violations", app.getPublicId(), scanId, hash);
  }

  public static String urlToSecurity(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/security", app.getPublicId(), scanId, hash);
  }

  public static String urlToLegal(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/legal", app.getPublicId(), scanId, hash);
  }

  public static String urlToAudit(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/audit", app.getPublicId(), scanId, hash);
  }

  public ComponentDetailsPage() {
    super(ROOT);
  }

  public SelenideElement backButton() {
    return child(".nx-text-link");
  }

  public ComponentDetailsHeader header() {
    return new ComponentDetailsHeader(childSelector(".component-details-header"));
  }

  public ComponentDetailsFooter footer() {
    return new ComponentDetailsFooter(childSelector(".component-details-footer"));
  }

  public ElementsCollection tabs() {
    return children(".nx-tab");
  }

  public SelenideElement remediationTab() {
    return this.tabs().get(0);
  }

  public SelenideElement componentInfoTab() {
    return this.tabs().get(1);
  }

  public SelenideElement violationsTab() {
    return this.tabs().get(2);
  }

  public SelenideElement securityTab() {
    return this.tabs().get(3);
  }

  public SelenideElement legalTab() {
    return this.tabs().get(4);
  }

  public SelenideElement auditTab() {
    return this.tabs().get(5);
  }

  public AuditLogContent auditLogContent() {
    return new AuditLogContent("#audit-log-tab-content");
  }

  public static class ComponentDetailsHeader
      extends BasicElement<ComponentDetailsPage.ComponentDetailsHeader>
  {
    private ComponentDetailsHeader(String selector) {
      super(selector);
    }

    public SelenideElement title() {
      return child("#component-details-title");
    }

    public ElementsCollection reportInformationElements() {
      SelenideElement child = child(".component-details-header__reportinfo");
      return child.findAll(".component-details-header__reportinfo-item");
    }

    public ElementsCollection tags() {
      SelenideElement child = child(".component-details-header__tags");
      return child.findAll("dd");
    }
  }

  public static class ComponentDetailsFooter
      extends BasicElement<ComponentDetailsPage.ComponentDetailsFooter>
  {
    private ComponentDetailsFooter(String selector) {
      super(selector);
    }

    public SelenideElement prevLink() {
      return child(".iq-pagination-link__prev");
    }

    public SelenideElement nextLink() {
      return child(".iq-pagination-link__next");
    }

    public SelenideElement paginationCounter() {
      return child(".iq-page-counter");
    }
  }
}
