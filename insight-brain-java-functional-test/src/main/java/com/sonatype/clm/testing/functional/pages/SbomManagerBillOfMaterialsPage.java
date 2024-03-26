/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class SbomManagerBillOfMaterialsPage
    extends BasicElement<SbomManagerBillOfMaterialsPage>
{
  public static String url(String applicationId, String versionId) {
    return BaseUrl.resolvePageUrl("/sbomManager/management/view/application/{applicationId}/bom/{versionId}",
        applicationId, versionId);
  }

  public SelenideElement bomPageContainer() {
    return child("#sbom-manager-bom");
  }

  public SelenideElement pageTitle() {
    return child(".nx-h1");
  }

  public SelenideElement sbomManagerNotEnabledError() {
    return child(".nx-alert--error");
  }
}
