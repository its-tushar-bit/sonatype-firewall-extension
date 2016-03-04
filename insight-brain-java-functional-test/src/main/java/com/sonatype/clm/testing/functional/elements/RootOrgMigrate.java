/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class RootOrgMigrate
{
  public static SelenideElement migrateBanner() {
    return $("#root-org-migrate-banner");
  }

  public static SelenideElement migrateConfiguredBanner() {
    return $("#root-org-migrate-configured-banner");
  }

  public static SelenideElement startButton() {
    return $("#root-org-migrate-banner button");
  }
}
