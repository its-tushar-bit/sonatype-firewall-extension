/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.componentdetails.DependencyTreeTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.OverviewTabContent;
import com.sonatype.clm.testing.functional.elements.componentdetails.SecurityTabContent;
import com.sonatype.clm.testing.functional.elements.componentdetails.LegalTabContent;
import com.sonatype.clm.testing.functional.elements.componentdetails.ClaimTabContent;
import com.sonatype.clm.testing.functional.elements.componentdetails.ViolationsTabContent;
import com.sonatype.clm.testing.functional.elements.componentdetails.ManageLabelsContentTab;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ContainerComponentDetailsPage
    extends BasicElement<ContainerComponentDetailsPage>
{
  public static final String ROOT = ".nx-page-main.iq-component-details-page";

  private static final String BASE_URL = "/firewall/containerReport/{publicId}/{scanId}/componentDetails/{hash}";

  public static String url(Application app, String scanId, String hash) {
    return urlToOverview(app, scanId, hash);
  }

  public static String urlToOverview(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/overview", app.getPublicId(), scanId, hash);
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

  public static String urlToLabels(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/labels", app.getPublicId(), scanId, hash);
  }

  public static String urlToAudit(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/audit", app.getPublicId(), scanId, hash);
  }

  public static String urlToClaim(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl(BASE_URL + "/claim", app.getPublicId(), scanId, hash);
  }

  public ContainerComponentDetailsPage() {
    super(ROOT);
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }

  public static SelenideElement title() {
    return $("#component-details-title");
  }

  public SelenideElement unknownComponentAlert() {
    return child(".iq-component-details-unknown-component-alert");
  }

  public SelenideElement proprietaryComponentAlert() {
    return child("#proprietary-component-matched-alert");
  }

  public SelenideElement addProprietarypComponentMatchersBtn() {
    return child("#iq-component-details-add-proprietary-component-matchers-btn");
  }

  public SelenideElement unknownComponentClaim() {
    return child("#iq-component-details-unknown-component-claim");
  }

  public ComponentDetailsHeader header() {
    return new ComponentDetailsHeader(childSelector(".component-details-header"));
  }

  public ComponentDetailsFooter footer() {
    return new ComponentDetailsFooter(childSelector(".iq-page-footer"));
  }

  public ElementsCollection tabs() {
    return children(".nx-tab");
  }

  public SelenideElement overviewTab() {
    return this.tabs().get(0);
  }

  public SelenideElement violationsTab() {
    return this.tabs().get(1);
  }

  public SelenideElement securityTab() {
    return this.tabs().get(2);
  }

  public SelenideElement legalTab() {
    return this.tabs().get(3);
  }

  public SelenideElement labelsTab() {
    return this.tabs().get(4);
  }

  public SelenideElement auditTab() {
    return this.tabs().get(5);
  }

  public SelenideElement claimTabForClaimedComponent() {
    return this.tabs().get(5);
  }

  public DependencyTreeTile dependencyTreeTile() {
    return new DependencyTreeTile();
  }

  public ViolationsTabContent violationsTabContent() {
    return new ViolationsTabContent();
  }

  public SecurityTabContent securityTabContent() {
    return new SecurityTabContent();
  }

  public LegalTabContent legalTabContent() {
    return new LegalTabContent();
  }

  public ClaimTabContent claimTabContent() {
    return new ClaimTabContent();
  }

  public OverviewTabContent overviewTabContent() {
    return new OverviewTabContent();
  }

  public ManageLabelsContentTab labelsContent() {
    return new ManageLabelsContentTab("#component-details-labels-tab-content");
  }

  public AuditLogContent auditLogContent() {
    return new AuditLogContent("#component-details-audit-tab-content");
  }

  public static class ComponentDetailsHeader
      extends BasicElement<ContainerComponentDetailsPage.ComponentDetailsHeader>
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
      return child.findAll("label");
    }
  }

  public static class ComponentDetailsFooter
      extends BasicElement<ContainerComponentDetailsPage.ComponentDetailsFooter>
  {
    private static final String FOOTER_CLICKABLE_SELECTOR = ".nx-text-link";

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

    public SelenideElement backButton() {
      return child(FOOTER_CLICKABLE_SELECTOR);
    }
  }
}
