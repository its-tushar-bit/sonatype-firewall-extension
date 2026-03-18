/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.NxAlert;
import com.sonatype.clm.testing.functional.elements.NxTree;
import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class OwnerSummaryPageWithLimitedVisibility
{
  private OwnerSummaryPageWithLimitedVisibility() {
    throw new IllegalStateException("This should not be instanced");
  }

  public static String baseUrl() {
    return BaseUrl.resolvePageUrl("/management/view");
  }

  public static String urlToRootOrg() {
    return url(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  public static String url(Owner owner) {
    return url(owner.getType(), owner.getPublicId());
  }

  public static String url(OwnerType ownerType, String id) {
    if (OwnerType.REPOSITORY_CONTAINER.equals(ownerType)) {
      return BaseUrl.resolvePageUrl("/management/view/repositories");
    }
    return BaseUrl.resolvePageUrl("/management/view/{ownerType}/{ownerId}", ownerType, id);
  }

  public static OrgsAndPoliciesSidebar sidebar() {
    return new OrgsAndPoliciesSidebar();
  }

  public static SelenideElement title() {
    return $(".nx-h1");
  }

  public static NxTree tree() {
    return new NxTree(".iq-owner-tree");
  }

  public static SelenideElement titleDescription() {
    return $(".nx-page-title__description");
  }

  public static NxAlert notification() {
    return NxAlert.getInfoAlert();
  }
}
