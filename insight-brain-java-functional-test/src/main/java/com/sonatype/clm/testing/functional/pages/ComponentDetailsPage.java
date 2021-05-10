/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.SelenideElement;

public class ComponentDetailsPage
    extends BasicElement<ComponentDetailsPage>
{
  public static final String ROOT = "#component-details-page";

  public static String url(Application app, String scanId, String hash) {
    return BaseUrl.resolvePageUrl("/applicationReport/{applicationPublicId}/{scanId}/componentDetails/{hash}",
        app.getPublicId(), scanId, hash);
  }

  public ComponentDetailsPage() {
    super(ROOT);
  }

  public SelenideElement title() {
    return child(".title");
  }
}
