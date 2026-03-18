/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages.sbom;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class LearnMoreSbomManagerPage
    extends BasicElement<LearnMoreSbomManagerPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/sbomManager/learnMore");
  }

  public SelenideElement infoAlert() {
    return child(".nx-alert--info");
  }

  public SelenideElement infoLink() {
    return child(".nx-alert--info a");
  }
}
