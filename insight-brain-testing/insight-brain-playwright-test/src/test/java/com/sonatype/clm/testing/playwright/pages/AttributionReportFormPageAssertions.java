/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AttributionReportFormPageAssertions
{
  private final AttributionReportFormPage page;

  public AttributionReportFormPageAssertions(AttributionReportFormPage page) {
    this.page = page;
  }

  public void shouldShowContainer() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowForm() {
    assertThat(page.form()).isVisible();
  }

  public void shouldShowGenerateReportButton() {
    assertThat(page.generateReportButton()).isVisible();
  }

  public void shouldHaveGenerateReportButtonEnabled() {
    assertThat(page.generateReportButton()).isEnabled();
  }

  public void shouldShowTableOfContentsCheckbox() {
    assertThat(page.tableOfContentsCheckbox()).isVisible();
  }

  public void shouldShowIncludeLicenseCheckbox() {
    assertThat(page.includeLicenseCheckbox()).isVisible();
  }

  public void shouldShowAppendixCheckbox() {
    assertThat(page.appendixCheckbox()).isVisible();
  }

  public void shouldShowManageTemplatesButton() {
    assertThat(page.manageTemplatesButton()).isVisible();
  }

  public void shouldShowReportTitleInput() {
    assertThat(page.reportTitleInput()).isVisible();
  }

  public void shouldShowAdditionalNoticeFilesSection() {
    assertThat(page.additionalNoticeFilesFieldset()).isVisible();
  }

  public void shouldShowAttachFilesButton() {
    assertThat(page.attachFilesButton()).isVisible();
  }

}
