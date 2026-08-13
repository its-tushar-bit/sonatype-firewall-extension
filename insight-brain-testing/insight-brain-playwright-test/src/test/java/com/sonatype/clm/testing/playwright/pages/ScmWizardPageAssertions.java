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

public class ScmWizardPageAssertions
{
  private final ScmWizardPage page;

  public ScmWizardPageAssertions(ScmWizardPage page) {
    this.page = page;
  }

  public void shouldShowModal() {
    assertThat(page.modal()).isVisible();
    assertThat(page.modalTitle()).isVisible();
  }

  public void shouldShowWizardCardWithCoreSections() {
    assertThat(page.wizardCard()).isVisible();
    assertThat(page.sectionHeading("Enable automatic source control")).isVisible();
    assertThat(page.sectionHeading("Create Access Token")).isVisible();
    assertThat(page.sectionHeading("Application Source Control Configuration")).isVisible();
  }

  public void shouldShowAutomaticSourceControlLinkInNewTab() {
    assertThat(page.automaticSourceControlLink()).isVisible();
    clickAndAssertOpensNewTab(page.automaticSourceControlLink());
  }

  public void shouldShowApplicationSourceControlLinkInNewTab() {
    assertThat(page.applicationSourceControlLink().first()).isVisible();
    clickAndAssertOpensNewTab(page.applicationSourceControlLink().first());
  }

  /**
   * Click a link and assert that doing so opens a new browser tab. We do not wait for the popup
   * to finish loading because the destination is an external docs URL that the test environment
   * cannot reach; we only need to verify the new page was created with a non-blank URL. This
   * verifies the user-observable behavior (a new tab opens on click) rather than the underlying
   * target="_blank" attribute.
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

  public void shouldShowTokenUrl(String tokenUrl) {
    assertThat(page.tokenUrlCode(tokenUrl)).isVisible();
  }

  public void shouldShowConfigureBaseUrlSection() {
    assertThat(page.configureBaseUrlSection()).isVisible();
  }

  public void shouldNotShowConfigureBaseUrlSection() {
    assertThat(page.configureBaseUrlSection()).hasCount(0);
  }
}
