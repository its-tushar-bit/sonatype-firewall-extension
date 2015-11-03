/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView;

public class OwnerDetailsEditingPage
{
  public static String url(String ownerType, String ownerId) {
    return "new/assets/index.html#/management/edit/" + ownerType + "/" + ownerId;
  }
}
