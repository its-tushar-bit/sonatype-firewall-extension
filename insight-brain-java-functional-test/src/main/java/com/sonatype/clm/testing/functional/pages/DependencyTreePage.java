/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxTree;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.SelenideElement;

public class DependencyTreePage
    extends BasicElement<DependencyTreePage>
{
  public static final String ROOT = ".nx-page-main.iq-dependency-tree-page";

  private static final String BASE_URL = "/applicationReport/{applicationPublicId}/{scanId}/dependencyTree";

  public DependencyTreePage() {
    super(ROOT);
  }

  public static String url(Application app, String scanId) {
    return BaseUrl.resolvePageUrl(BASE_URL, app.getPublicId(), scanId);
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public NxTree tree() {
    return new NxTree(".iq-dependency-tree");
  }
}
