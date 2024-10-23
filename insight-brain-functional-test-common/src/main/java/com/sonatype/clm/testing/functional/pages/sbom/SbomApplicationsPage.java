/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages.sbom;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.sbom.SbomApplicationsTable;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class SbomApplicationsPage
    extends BasicElement<SbomApplicationsPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/sbomManager/applications");
  }

  public SelenideElement container() {
    return child("#sbom-manager-applications-page");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public static SbomApplicationsTable sbomApplicationsTable() {
    return new SbomApplicationsTable();
  }
}
