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
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupTile;
import com.sonatype.clm.testing.functional.elements.OwnerSummaryTile;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

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

  public static OwnerSummaryTile summaryTile() {
    return new OwnerSummaryTile();
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
}
