/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.CategoryTile;
import com.sonatype.clm.testing.functional.elements.CategoryTile.CategoryTileAppContext;
import com.sonatype.clm.testing.functional.elements.CategoryTile.CategoryTileOrgContext;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupTile;
import com.sonatype.clm.testing.functional.elements.PillButton;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class OwnerSummaryPage
{
  public static String url(Owner owner) {
    return url(owner.getType(), owner.getPublicId());
  }

  public static String url(OwnerType ownerType, String id) {
    if (OwnerType.REPOSITORY_CONTAINER.equals(ownerType)) {
      return BaseUrl.uriBuilder().fragment("/management/view/repositories").build().toString();
    }

    return BaseUrl.uriBuilder().fragment("/management/view/{ownerType}/{ownerId}").build(ownerType, id).toString();
  }

  public static CategoryTile categoryTile(Owner owner) {
    return categoryTile(owner.getType());
  }

  public static CategoryTile categoryTile(OwnerType ownerType) {
    return OwnerType.ORGANIZATION.equals(ownerType) ? new CategoryTileOrgContext() : new CategoryTileAppContext();
  }

  public static PolicyTile policyTile() {
    return new PolicyTile();
  }

  public static LabelTile labelTile() {
    return new LabelTile();
  }

  public static LicenseThreatGroupTile licenseThreatGroupTile() {
    return new LicenseThreatGroupTile();
  }

  public static AccessTile accessTile() {
    return new AccessTile("#owner-pill-access");
  }

  static SelenideElement scrollContainer() {
    return $(".tile-scroll-container");
  }

  public static class SummaryTile
  {
    private static final String ROOT_ID = "#owner-summary";

    private static SelenideElement root() {
      return $(ROOT_ID);
    }

    public static SelenideElement name() {
      return $(SelectorUtils.createSelector(ROOT_ID, ".iq-tile-header"));
    }

    public static SelenideElement publicId() {
      return $(SelectorUtils.createSelector(ROOT_ID, ".iq-tile-header__description"));
    }

    public static SelenideElement headerIcon() {
      return $(SelectorUtils.createSelector(ROOT_ID, ".iq-tile-header__icon", "img"));
    }

    public static SelenideElement contact() {
      return root().find(".iq-tile-header__subtitle");
    }

    public static SelenideElement icon() {
      return $("img");
    }

    public static ErrorBox error() {
      return new ErrorBox(ROOT_ID, ".iq-alert.iq-alert--error");
    }

    public static PillButton appCategoriesButton() {
      return new PillButton(scrollContainer(), "#owner-app-categories-button");
    }

    public static PillButton policyButton() {
      return new PillButton(scrollContainer(), "#owner-policy-button");
    }

    public static PillButton labelsButton() {
      return new PillButton(scrollContainer(), "#owner-comp-labels-button");
    }

    public static PillButton ltgsButton() {
      return new PillButton(scrollContainer(), "#owner-ltgs-button");
    }

    public static PillButton accessButton() {
      return new PillButton(scrollContainer(), "#owner-access-button");
    }
  }
}
