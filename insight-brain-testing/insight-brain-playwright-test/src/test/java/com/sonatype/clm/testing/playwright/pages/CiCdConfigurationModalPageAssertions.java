/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import org.assertj.core.api.Assertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CiCdConfigurationModalPageAssertions
{
  private final CiCdConfigurationModalPage page;

  public CiCdConfigurationModalPageAssertions(CiCdConfigurationModalPage page) {
    this.page = page;
  }

  public void shouldShowModal() {
    assertThat(page.modal()).isVisible();
    assertThat(page.modalTitle()).isVisible();
  }

  public void shouldShowWizardWithStepCards() {
    assertThat(page.wizardContainer()).isVisible();
    assertThat(page.stepCard("Install / Configure")).isVisible();
    assertThat(page.stepCard("Connect")).isVisible();
    assertThat(page.stepCard("Review")).isVisible();
  }

  public void shouldHaveThreeViewDocumentationLinksOpeningInNewTabs() {
    Locator links = page.viewDocumentationLinks();
    assertThat(links).hasCount(3);
    // Click each link and verify a new tab actually opens; this is the user-observable behavior
    // (target="_blank" alone is an implementation detail and would not catch a JS handler that
    // calls preventDefault()).
    for (int i = 0; i < 3; i++) {
      clickAndAssertOpensNewTab(links.nth(i));
    }
  }

  public void shouldShowMoreInfoLinkOpeningInNewTab() {
    assertThat(page.moreInfoLink()).isVisible();
    clickAndAssertOpensNewTab(page.moreInfoLink());
  }

  /**
   * Click a link and assert that doing so opens a new browser tab. We do not wait for the popup
   * to finish loading because the destination is an external docs URL that the test environment
   * cannot reach; we only need to verify the new page was created with a non-blank URL.
   */
  private void clickAndAssertOpensNewTab(Locator link) {
    Page popup = page.playwrightPage().context().waitForPage(() -> link.click());
    try {
      Assertions.assertThat(popup).isNotNull();
      Assertions.assertThat(popup.url())
          .isNotBlank()
          .isNotEqualTo("about:blank");
    }
    finally {
      popup.close();
    }
  }

  public void shouldShowPipelineSnippet() {
    assertThat(page.pipelineSnippet()).isVisible();
    assertThat(page.copyToClipboardButton()).isVisible();
  }

  public void shouldShowAllParameterTerms() {
    // Filter <dt>s by text so "Stage" doesn't match descriptions containing "build, stage, release".
    assertThat(page.parameterDescriptionList()).isVisible();
    assertThat(termWithText("Application ID")).isVisible();
    assertThat(termWithText("Instance")).isVisible();
    assertThat(termWithText("Scan patterns")).isVisible();
    assertThat(termWithText("Stage")).isVisible();
    assertThat(termWithText("Organization")).isVisible();
  }

  private Locator termWithText(String text) {
    return page.parameterDescriptionList()
        .locator("dt.nx-list__term")
        .filter(new Locator.FilterOptions().setHasText(text));
  }
}
