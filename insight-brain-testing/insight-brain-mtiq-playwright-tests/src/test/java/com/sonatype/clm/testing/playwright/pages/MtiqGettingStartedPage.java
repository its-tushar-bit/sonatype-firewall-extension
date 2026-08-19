/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

public class MtiqGettingStartedPage
    extends BasePage
{
  private static final String ROOT = "#getting-started";

  private static final String PRODUCT_LICENSE_SUMMARY = "#product-license-summary";

  private static final String SYSTEM_SETUP = "#system-setup";

  private static final String LEARNING_TOPICS = "#learning-topics";

  public MtiqGettingStartedPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/gettingStarted";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator hdsConnectivityWarning() {
    return locator(ROOT + " #hds-unreachable-warning");
  }

  public Locator productLicenseSummary() {
    return locator(PRODUCT_LICENSE_SUMMARY);
  }

  public Locator licenseExpiryDate() {
    return locator(PRODUCT_LICENSE_SUMMARY + " #license-expiry-date");
  }

  public Locator licenseDaysToExpiration() {
    return locator(PRODUCT_LICENSE_SUMMARY + " #license-days-to-expiration");
  }

  public Locator licenseFingerprint() {
    return locator(PRODUCT_LICENSE_SUMMARY + " #license-fingerprint");
  }

  public Locator licenseProducts() {
    return locator(PRODUCT_LICENSE_SUMMARY + " #license-products .nx-read-only__data");
  }

  public Locator licensedDevelopersRows() {
    return locator(PRODUCT_LICENSE_SUMMARY + " #license-licensed-developers > div");
  }

  public Locator systemSetup() {
    return locator(SYSTEM_SETUP);
  }

  public Locator systemSetupSections() {
    return locator(SYSTEM_SETUP + " .nx-read-only__item > .nx-read-only__label");
  }

  public Locator addingUsersTopics() {
    return locator(SYSTEM_SETUP + " #system-setup-adding-users .nx-read-only__item > .nx-grid-header__title");
  }

  public Locator learningTopics() {
    return locator(LEARNING_TOPICS);
  }

  public Locator learningTopicsSectionTopics() {
    return locator(LEARNING_TOPICS + " .nx-read-only__item > .nx-grid-header__title");
  }
}
