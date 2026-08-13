/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WebhookListPageAssertions
{
  private final WebhookListPage page;

  public WebhookListPageAssertions(WebhookListPage page) {
    this.page = page;
  }

  public void shouldRenderPageLayout() {
    assertThat(page.container()).isVisible();
    assertThat(page.heading()).hasText("Webhooks");
    assertThat(page.tileHeading()).hasText("Configure Webhooks");
    assertThat(page.addWebhookButton()).isVisible();
  }

  public void shouldShowEmptyState() {
    assertThat(page.emptyMessage()).hasText("No webhooks are defined");
  }

  public void shouldShowWebhookInList(String url) {
    assertThat(page.webhookItemByUrl(url)).isVisible();
  }

  public void shouldNotShowWebhookInList(String url) {
    assertThat(page.webhookItemByUrl(url)).not().isVisible();
  }
}
