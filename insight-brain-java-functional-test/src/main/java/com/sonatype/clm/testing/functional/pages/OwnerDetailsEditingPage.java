/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

public class OwnerDetailsEditingPage
{
  public static String url(Owner owner) {
    return url(owner.getType(), owner.getPublicId());
  }

  public static String url(OwnerType ownerType, String ownerId) {
    if (OwnerType.REPOSITORY_CONTAINER.equals(ownerType)) {
      return BaseUrl.uriBuilder().fragment("/management/edit/repositories").build().toString();
    }

    return BaseUrl.uriBuilder().fragment("/management/edit/{ownerType}/{ownerId}").build(ownerType, ownerId)
        .toString();
  }
}
