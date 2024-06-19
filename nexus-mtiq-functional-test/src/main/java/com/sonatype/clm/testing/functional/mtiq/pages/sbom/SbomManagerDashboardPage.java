/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.pages.sbom;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class SbomManagerDashboardPage extends BasicElement<SbomManagerDashboardPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/sbomManager/dashboard");
  }

  public SelenideElement container() {
    return child("#sbom-manager-dashboard");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public SelenideElement errorAlert() {
    return child(".nx-alert--error");
  }
}
