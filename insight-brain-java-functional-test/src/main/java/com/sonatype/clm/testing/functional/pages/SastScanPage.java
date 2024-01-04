/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class SastScanPage
{
  public static String urlToEdit(String applicationId, String sastScanId) {
    return urlToCreate(applicationId, sastScanId);
  }

  public static String urlToCreate(String applicationId, String sastScanId) {
    return BaseUrl.resolvePageUrl("/application/{applicationId}/sastScan/{sastScanId}", applicationId, sastScanId);
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static SelenideElement triggeredOnDate() {
    return $(".nx-page-title__description");
  }

  public static SelenideElement filterBySeverityDropdown() {
    return $(".iq_sast_scan_findings__container .nx-dropdown");
  }

  public static SelenideElement findingsTable() {
    return $(".iq_sast_scan_findings__container .nx-table");
  }

  public static ElementsCollection sastFindingTableDataRows() {
    return findingsTable().findAll(" tbody .nx-table-row");
  }
}
