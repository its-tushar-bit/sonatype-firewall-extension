/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.IqVulnerabilityModal;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class WaiverDetailsPage
    extends BasicElement<WaiverDetailsPage>
{
  public static final String ROOT = "#waiver-details-page";

  public static String url(String ownerType, String ownerId, String waiverId) {
    return BaseUrl.resolvePageUrl(
        "/waiver/{ownerType}/{ownerId}/{waiverId}",
        ownerType,
        ownerId,
        waiverId);
  }

  public static String urlWithQueryParams(
      String ownerType,
      String ownerId,
      String waiverId,
      String type,
      String sidebarReference)
  {
    return urlWithQueryParams(ownerType, ownerId, waiverId, type, sidebarReference, null);
  }

  public static String urlWithQueryParams(
      String ownerType,
      String ownerId,
      String waiverId,
      String type,
      String sidebarReference,
      Integer page)
  {
    String path = "/waiver/{ownerType}/{ownerId}/{waiverId}?type={type}&sidebarReference={sidebarReference}";
    if (page != null) {
      return BaseUrl.resolvePageUrl(
          path + "&page={page}",
          ownerType,
          ownerId,
          waiverId,
          type,
          sidebarReference,
          page);
    }
    return BaseUrl.resolvePageUrl(
        path,
        ownerType,
        ownerId,
        waiverId,
        type,
        sidebarReference);
  }

  public WaiverDetailsPage() {
    super(ROOT);
  }

  public SelenideElement detailsTileHeader() {
    return child("#iq-waiver-details-header");
  }

  public SelenideElement detailsInfoAlert() {
    return child(".nx-alert--info");
  }

  public SelenideElement detailsPolicy() {
    return child(".iq-waiver-details__policy .nx-read-only__data");
  }

  public SelenideElement detailsConstraint() {
    return child(".iq-waiver-details__constraint .nx-read-only__data");
  }

  public SelenideElement detailsConditions() {
    return child(".iq-waiver-details__constraint .nx-read-only__data.iq-waiver-details__constrain-conditions");
  }

  public SelenideElement vulnerabilityDetailsButton() {
    return child(".iq-waiver-details__vulnerability-details-button");
  }

  public IqVulnerabilityModal detailsModal() {
    return new IqVulnerabilityModal("#vulnerability-details-modal");
  }

  public SelenideElement detailsScope() {
    return child(".iq-waiver-details__scope .nx-read-only__data");
  }

  public SelenideElement detailsComponent() {
    return child(".iq-waiver-details__components .nx-read-only__data");
  }

  public SelenideElement detailsExpiration() {
    return child(".iq-waiver-details__expiration .nx-read-only__data");
  }

  public SelenideElement detailsComment() {
    return child(".iq-waiver-details__comments .nx-read-only__data");
  }

  public SelenideElement detailsCreatedBy() {
    return child(".iq-waiver-details__created-by .nx-read-only__data");
  }

  public SelenideElement detailsDateCreated() {
    return child(".iq-waiver-details__date-created .nx-read-only__data");
  }

  public SelenideElement detailsReason() {
    return child(".iq-waiver-details__reason .nx-read-only__data");
  }

  public SelenideElement detailsVersion() {
    return child(".iq-waiver-details__version .nx-read-only__data");
  }

  public SelenideElement renewWaiverButton() {
    return child(".iq-waiver-details__delete-waiver .nx-btn--tertiary:first-child");
  }

  public SelenideElement deleteWaiverButton() {
    return child(".iq-waiver-details__delete-waiver .nx-btn:last-child");
  }

  public SelenideElement detailsLastRenewed() {
    return child(".iq-waiver-details__last-renewed-date .nx-read-only__data");
  }

  public SelenideElement detailsRenewedBy() {
    return child(".iq-waiver-details__last-renewed-by .nx-read-only__data");
  }

  public SelenideElement detailsRenewalReason() {
    return child(".iq-waiver-details__last-renewal-reason .nx-read-only__data");
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }

  public SelenideElement detailsComponentUpgradeAvailable() {
    return child(".iq-upgrade-available-indicator");
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

    public SelenideElement componentName() {
      return child(".nx-list__subtext .iq-component-display-text");
    }

    public SelenideElement componentNameWithoutDisplayText() {
      return child(".nx-list__subtext");
    }

    public SelenideElement organizationFullName() {
      return child(".nx-list__subtext", ".nx-truncate-ellipsis", nthChild(2));
    }
  }
}
