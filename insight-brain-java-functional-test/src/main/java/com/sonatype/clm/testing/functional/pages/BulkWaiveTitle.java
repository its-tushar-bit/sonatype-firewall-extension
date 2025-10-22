/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

/**
 * Shared title/subtitle component used across all bulk waiver pages
 * (BulkWaivePage, WaiverConfigurationPage, WaiverConfirmationPage)
 */
public class BulkWaiveTitle
    extends BasicElement<BulkWaiveTitle>
{
  private static final String ROOT = ".nx-page-title";

  public BulkWaiveTitle() {
    super(ROOT);
  }

  public SelenideElement title() {
    return child("h1");
  }

  public SelenideElement subtitle() {
    return child("h2");
  }
}
