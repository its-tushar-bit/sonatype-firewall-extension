/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class ApplicationAttributionReportPage
    extends BasicElement<ApplicationAttributionReportPage>
{
  public static final String ROOT = "body";

  public static String url(Application app, String stage) {
    return BaseUrl.resolveApiV2Url("/licenseLegalMetadata/application/{applicationPublicId}/stage/{stage}/report",
        app.getPublicId(),
        stage);
  }

  public ApplicationAttributionReportPage() {
    super(ROOT);
  }

  public SelenideElement reportTitle() {
    return child("h1");
  }

  public SelenideElement tableOfContents() {
    return child("#table-of-contents");
  }

  public SelenideElement appendix() {
    return child("#appendix");
  }

  public SelenideElement appendixStandardLicenseText(String license) {
    return child("#appendix h4#standard-" + license).parent();
  }

  public SelenideElement header() {
    return child("#header");
  }

  public SelenideElement footer() {
    return child("#footer");
  }

  public SelenideElement additionalNotices() {
    return child("#additional-notices");
  }

  public ElementsCollection componentElements() {
    return children(".componentBox");
  }

  public SelenideElement findComponentFor(String purl) {
    return child("#" + purl).parent();
  }
}
