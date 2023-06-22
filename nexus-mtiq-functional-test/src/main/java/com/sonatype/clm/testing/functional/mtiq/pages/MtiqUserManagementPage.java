/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.pages;

import com.sonatype.clm.testing.functional.pages.UserManagementPage;

import com.codeborne.selenide.ElementsCollection;

public class MtiqUserManagementPage
    extends UserManagementPage
{
  @Override
  public ElementsCollection userItems() {
    return children(".nx-list__item");
  }
}
