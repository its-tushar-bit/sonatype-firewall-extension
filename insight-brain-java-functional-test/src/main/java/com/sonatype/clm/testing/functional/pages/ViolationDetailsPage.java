/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.ListSimilarWaiversTable;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

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
    return new NxBackButton();
  }

  public SidebarNav sidebarNav() {
    return new SidebarNav("#sidebar-nav-list");
  }

  public static class SidebarNav
      extends BasicElement<SidebarNav>
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

  public static class SidebarNavListItem
      extends BasicElement<SidebarNavListItem>
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

  public PolicyViolationConstraintInfo policyViolationConstraintInfo() {
    return new PolicyViolationConstraintInfo(childSelector("#policy-violation-constraint-info"));
  }

  public PolicyViolationSecurityDetailsInfoTile securityVulnerabilityDetailsTile() {
    return new PolicyViolationSecurityDetailsInfoTile(childSelector("#security-vulnerability-details-tile"));
  }

  public PolicyViolationApplicableWaiversInfoTile applicableWaiversInfoTile() {
    return new PolicyViolationApplicableWaiversInfoTile(childSelector("#applicable-waivers-tile"));
  }

  public PolicyViolationSimilarWaiversInfoTile similarWaiversInfoTile() {
    return new PolicyViolationSimilarWaiversInfoTile(childSelector("#similar-waivers-tile"));
  }

  public SelenideElement securityVulnerabilityDetailsTab() {
    return child("#violation-security-vulnerability-details-tab");
  }

  public PolicyViolationApplicableWaiversTab applicableWaiversTab() {
    return new PolicyViolationApplicableWaiversTab(childSelector("#violation-applicable-waivers-tab"));
  }

  public SelenideElement similarWaiversTab() {
    return child("#violation-similar-waivers-tab");
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

    public SelenideElement getAddWaiversSegmentedDropdownButton() {
      return child("#violation-page-add-waiver .nx-segmented-btn__dropdown-btn");
    }

    public SelenideElement addWaiverButton() {
      return child("#violation-page-add-waiver");
    }

    public SelenideElement addWaiverSegmentedButton() {
      return child("#violation-page-add-waiver .nx-segmented-btn__main-btn");
    }

    public SelenideElement requestWaiverButton() {
      return child("#violation-page-request-waiver");
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

    public static WebElementCondition unused() {
      return Condition.cssClass("iq-violation-details__stage--unused");
    }
  }

  public class PolicyViolationConstraintInfo
      extends BasicElement<PolicyViolationConstraintInfo>
  {
    private PolicyViolationConstraintInfo(String selector) {
      super(selector);
    }

    public SelenideElement headerTitle() {
      return child(".nx-tile-header__title");
    }

    public SelenideElement subheaderTitle() {
      return child(".nx-tile-content h3.nx-h3");
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

  public static class PolicyViolationApplicableWaiversInfoTile
      extends BasicElement<PolicyViolationApplicableWaiversInfoTile>
  {
    private final String parentSelector;

    public PolicyViolationApplicableWaiversInfoTile(String selector) {
      super(selector);
      parentSelector = selector;
    }

    public SelenideElement waiverListHeader() {
      return child(".nx-h3");
    }

    public ListWaiversTable getApplicableWaiversTable() {
      return new ListWaiversTable(parentSelector);
    }
  }

  public static class PolicyViolationSimilarWaiversInfoTile
      extends BasicElement<PolicyViolationSimilarWaiversInfoTile>
  {
    private final String parentSelector;

    public PolicyViolationSimilarWaiversInfoTile(String selector) {
      super(selector);
      parentSelector = selector;
    }

    public SelenideElement waiverListHeader() {
      return child(".similar-waivers-header__title");
    }

    public SelenideElement waiverListSubtitle() {
      return child(".similar-waivers-header__subtitle");
    }

    public ListSimilarWaiversTable getSimilarWaiversTable() {
      return new ListSimilarWaiversTable(parentSelector);
    }

    public SelenideElement filterDropdown() {
      return child(".similar-waivers-header__filter");
    }

    public SelenideElement activeFilter() {
      return child(".similar-waivers-header__filter .nx-radio-checkbox:nth-child(1)");
    }

    public SelenideElement activeFilterCheckbox() {
      return child(".similar-waivers-header__filter .nx-radio-checkbox:nth-child(1) " +
          ".nx-radio-checkbox__control .fa-check");
    }

    public SelenideElement exactFilter() {
      return child(".similar-waivers-header__filter .nx-radio-checkbox:nth-child(2)");
    }

    public SelenideElement commentFilter() {
      return child(".similar-waivers-header__filter .nx-radio-checkbox:nth-child(3)");
    }
  }

  public static class PolicyViolationApplicableWaiversTab
      extends BasicElement<PolicyViolationApplicableWaiversTab>
  {
    public PolicyViolationApplicableWaiversTab(String selector) {
      super(selector);
    }

    public SelenideElement waiversIndicator() {
      return child(".iq-waiver-indicator-tab");
    }
  }

  public static class PolicyViolationSimilarWaiversTab
      extends BasicElement<PolicyViolationSimilarWaiversTab>
  {
    public PolicyViolationSimilarWaiversTab(String selector) {
      super(selector);
    }
  }
}
