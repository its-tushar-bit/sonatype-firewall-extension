/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.pages.sbom;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.mtiq.elements.sbom.ImportSbomModal;
import com.sonatype.clm.testing.functional.mtiq.elements.sbom.SbomsTile;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class SbomManagerApplicationSummaryPage
    extends BasicElement<SbomManagerApplicationSummaryPage>
{
  public static String url(String applicationId) {
    return BaseUrl.resolvePageUrl("/sbomManager/management/view/application/{applicationId}", applicationId);
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public static SbomsTile sbomsTile() {
    return new SbomsTile();
  }

  public ImportSbomModal importSbomModal() {
    return new ImportSbomModal();
  }
}
