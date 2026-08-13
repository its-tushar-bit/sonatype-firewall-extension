/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Thin navigation helper for the SBOM Manager BOM page.
 * Used as a prerequisite navigation step before loading component details.
 */
public class SbomManagerBomPage
    extends BasePage
{
  public SbomManagerBomPage() {
    super();
  }

  public static String url(String appId, String versionId) {
    return "/assets/index.html#/sbomManager/application/" + appId + "/bom/" + versionId + "/overview";
  }

  public Locator container() {
    return locator("#sbom-manager-bom");
  }
}
