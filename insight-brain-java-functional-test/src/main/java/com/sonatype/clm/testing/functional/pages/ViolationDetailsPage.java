/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ViolationDetailsPage
    extends BasicElement<ViolationDetailsPage>
{
  public static final String ROOT = "#violation-page";

  public static String url(String violationId) {
    return BaseUrl.resolvePageUrl("/violation/{id}", violationId);
  }

  public static String urlWithQueryParams(String violationId, String type, String sidebarReference) {
    return urlWithQueryParams(violationId, type, sidebarReference, null);
  }

  public static String urlWithQueryParams(String violationId, String type, String sidebarReference, Integer page) {
    String path = "/violation/{id}?type={type}&sidebarReference={sidebarReference}";
    if (page != null) {
      return BaseUrl.resolvePageUrl(path + "&page={page}", violationId, type, sidebarReference, page);
    }
    return BaseUrl.resolvePageUrl(path, violationId, type, sidebarReference);
  }

  public ViolationDetailsPage() {
    super(ROOT);
  }

  public NxBackButton backButton() {
    return new NxBackButton("#menu-bar__back-button-container");
  }

  public SidebarNav sidebarNav() {
    return new SidebarNav("#sidebar-nav-list");
  }

  public static class SidebarNav extends BasicElement<SidebarNav>
  {
    SidebarNav(String selector) {
      super(selector);
    }

    public SelenideElement sidebarNavTitle() {
      return child(".nx-h4");
    }

    public ElementsCollection sidebarNavItems() {
      return children("li");
    }

    public SidebarNavListItem navItem(int index) {
      return new SidebarNavListItem(childSelector("li", nthChild(index + 1)));
    }
  }

  public static class SidebarNavListItem extends BasicElement<SidebarNavListItem>
  {
    SidebarNavListItem(String selector) {
      super(selector);
    }

    public SelenideElement threatIndicator() {
      return child(".nx-threat-indicator");
    }

    public SelenideElement policyName() {
      return child(".nx-list__text");
    }

    public SelenideElement artifactName() {
      return child(".nx-list__subtext");
    }
  }

  public ViolationDetailsTile detailsTile() {
    return new ViolationDetailsTile(childSelector("#violation-details-tile"));
  }

  public PolicyViolationConstraintInfoTile policyViolationConstraintInfoTile() {
    return new PolicyViolationConstraintInfoTile(childSelector("#policy-violation-constraint-info-tile"));
  }

  public PolicyViolationSecurityDetailsInfoTile securityVulnerabilityDetailsTile() {
    return new PolicyViolationSecurityDetailsInfoTile(childSelector("#security-vulnerability-details-tile"));
  }

  public static class ViolationDetailsTile
      extends BasicElement<ViolationDetailsTile>
  {
    private ViolationDetailsTile(String selector) {
      super(selector);
    }

    public SelenideElement headerTitle() {
      return child(".nx-tile-header__title");
    }

    public SelenideElement headerSubtitle() {
      return child(".nx-tile-header__subtitle");
    }

    public SelenideElement threatLevel() {
      return child(".iq-violation-details__threat-level dd");
    }

    public SelenideElement firstReported() {
      return child(".iq-violation-details__first-reported dd");
    }

    public SelenideElement lastReported() {
      return child(".iq-violation-details__last-reported dd");
    }

    public SelenideElement policyType() {
      return child(".iq-violation-details__policy-type dd");
    }

    public ElementsCollection stages() {
      return children(".iq-violation-details__stages dd");
    }

    public ViolationDetailsStage stage(int index) {
      return new ViolationDetailsStage(
          childSelector(".iq-violation-details__stages dd:nth-of-type(" + (index + 1) + ")"));
    }

    public SelenideElement policyOwner() {
      return child(".iq-violation-details__policy-owner dd");
    }

    public SelenideElement policyOwnerLink() {
      return child(".iq-violation-details__policy-owner dd a");
    }

    public Button manageWaiversButton() {
      return new Button("#violation-page-manage-waivers");
    }

    public SelenideElement manageWaiversDropdownToggle() {
      return child(".nx-segmented-btn__dropdown-btn");
    }

    public SelenideElement addWaiverDropdownButton() {
      return child("#violation-page-add-waiver");
    }

    public SelenideElement requestWaiverDropdownButton() {
      return child("#violation-page-request-waiver");
    }

    public SelenideElement waiversIndicator() {
      return child(".iq-waiver-indicator");
    }
  }

  public static class ViolationDetailsStage
      extends BasicElement<ViolationDetailsStage>
  {
    private ViolationDetailsStage(String selector) {
      super(selector + " .iq-violation-details__stage");
    }

    public SelenideElement icon() {
      return child(".nx-icon");
    }

    public SelenideElement link() {
      return child("a");
    }

    public static Condition unused() {
      return Condition.cssClass("iq-violation-details__stage--unused");
    }
  }

  public class PolicyViolationConstraintInfoTile
      extends BasicElement<PolicyViolationConstraintInfoTile>
  {
    private PolicyViolationConstraintInfoTile(String selector) {
      super(selector);
    }

    public SelenideElement headerTitle() {
      return child(".nx-tile-header__title");
    }

    public SelenideElement subheaderTitle() {
      return child("h3.nx-h3");
    }

    public ElementsCollection reasons() {
      return children("#policy-violation-reasons li");
    }

    public SelenideElement reason(int index) {
      return child("#policy-violation-reasons li:nth-of-type(" + (index + 1) + ")");
    }
  }

  public class PolicyViolationSecurityDetailsInfoTile
      extends BasicElement<PolicyViolationSecurityDetailsInfoTile>
  {
    private PolicyViolationSecurityDetailsInfoTile(String selector) {
      super(selector);
    }

    public SelenideElement vulnerabilityDetailsHeader() {
      return child(".iq-vulnerability-details .nx-h2");
    }
  }
}
