/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages.sbom;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class BillOfMaterialsPageSummaryTile
    extends BasicElement<BillOfMaterialsPageSummaryTile>
{
  public static String url(String applicationId, String versionId) {
    return BaseUrl.resolvePageUrl("/sbomManager/management/view/application/{applicationId}/bom/{versionId}",
        applicationId, versionId);
  }

  public SelenideElement componentSummaryChartAndProgress() {
    return child("#bill-of-materials-summary-tile-chart-and-progress-component-summary");
  }

  public SelenideElement vulnerabilitySummaryChartAndProgress() {
    return child("#bill-of-materials-summary-tile-chart-and-progress-vulnerability-summary");
  }

  public SelenideElement policyViolationSummaryChartAndProgress() {
    return child("#bill-of-materials-summary-tile-chart-and-progress-policy-violation-summary");
  }

  public SelenideElement releaseStatusSummaryDescription() {
    return child(".sbom-manager-summary-tile-release-status__description");
  }

  public SelenideElement summaryTileMetadataAccordion() {
    return child(".sbom-manager-bill-of-materials-summary-metadata-accordion");
  }
}
