/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ApplicationReportPage
    extends BasicElement<ApplicationReportPage>
{
  public static final String ROOT = "#application-report";

  public static String url(Application app, String scanId) {
    return BaseUrl.resolvePageUrl("/applicationReport/{applicationPublicId}/{scanId}", app.getPublicId(), scanId);
  }

  public ApplicationReportPage() {
    super(ROOT);
  }

  public SelenideElement reportTitle() {
    return $(".iq-tile-header__title");
  }

  public SelenideElement reportDate() {
    return $(".iq-tile-header__subtitle");
  }

  public IQDropdown optionsDropdown() {
    return new IQDropdown("#options-dropdown");
  }
}
