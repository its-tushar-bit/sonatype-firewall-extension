/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.NxTree;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class OwnerTreeViewPage
{
  private OwnerTreeViewPage() {
    throw new IllegalStateException("Utility class, it should not be instanced");
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/management/tree");
  }

  public static SelenideElement title() {
    return $(".nx-page-title");
  }

  public static NxTree tree() {
    return new NxTree(".iq-owner-tree");
  }

  public static SelenideElement treeViewButton() {
    return $(".iq-tree-view-button");
  }

  public static NxBackButton backButton() {
    return new NxBackButton("#menu-bar__back-button-container");
  }
}
