/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.componentdetails.OtherVersionsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class QuarantineComponentReportPage
    extends BasicElement<QuarantineComponentReportPage>
{
  public static final String QUARANTINE_COMPONENT_REPORT_SELECTOR = "#quarantined-component-report";

  public static String url(String quarantineComponentReportId) {
    return BaseUrl.rootUriBuilder()
        .fragment("/firewall/repositories/quarantinedComponent/" +
            quarantineComponentReportId)
        .toString();
  }

  public RiskRemediationTile getRiskRemediationTile() {
    return RiskRemediationTile.getOverviewTileForParent(QUARANTINE_COMPONENT_REPORT_SELECTOR);
  }

  public PolicyViolationsTable getViolationsTable() {
    return PolicyViolationsTable.getPolicyViolationsTableForParent(QUARANTINE_COMPONENT_REPORT_SELECTOR);
  }

  public OtherVersionsTable getOtherVersionsTable() {
    return OtherVersionsTable.getOtherVersionsTableForParent(QUARANTINE_COMPONENT_REPORT_SELECTOR);
  }

  public ElementsCollection getAllLoadingSpinners() {
    return children(".nx-loading-spinner");
  }

  public SelenideElement getQuarantineReportComponentOverviewTile() {
    return child(".iq-quarantine-report-component-overview-tile");
  }

  public SelenideElement getQuarantineReportComponentOverviewTileTitle() {
    return child(".iq-quarantine-report-component-overview-tile .nx-h2");
  }

  public SelenideElement getQuarantineReportComponentOverviewTileReadOnlyItemData(int index) {
    return children(".nx-read-only__item .nx-read-only__data").get(index);
  }

  public SelenideElement getQuarantineReportComponentOverviewTileRepositoryLink() {
    return child(".iq-quarantine-report-component-overview-tile .nx-text-link");
  }

  public SelenideElement getQuarantineReportComponentOverviewTileViewComponentDetails() {
    return child(".iq-quarantine-report-component-overview-tile .nx-btn");
  }

  public SelenideElement getTokenWarningAlert() {
    return child(".nx-alert.nx-alert--warning");
  }

  public SelenideElement getExpirationReportAlert() {
    return child(".nx-alert.nx-alert--info");
  }
}
