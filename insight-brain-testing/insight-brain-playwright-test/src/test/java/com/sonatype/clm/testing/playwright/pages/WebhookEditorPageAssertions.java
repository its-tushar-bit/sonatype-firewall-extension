/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.model.configuration.webhook.Webhook;

import org.assertj.core.api.Assertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WebhookEditorPageAssertions
{
  private final WebhookEditorPage page;

  public WebhookEditorPageAssertions(WebhookEditorPage page) {
    this.page = page;
  }

  public void shouldShowCreateMode() {
    assertThat(page.pageTitle()).hasText("Create Webhook");
  }

  public void shouldShowEditMode() {
    assertThat(page.pageTitle()).hasText("Edit Webhook");
  }

  public void shouldShowValidationError(String expectedText) {
    assertThat(page.validationError()).containsText(expectedText);
  }

  public void shouldShowHttpInfoAlert() {
    assertThat(page.httpInfoAlert()).isVisible();
  }

  public void shouldShowHttpWarningModal() {
    assertThat(page.httpWarningModal()).isVisible();
  }

  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }

  public void shouldShowDeleteModalWarning(String url) {
    assertThat(page.deleteModalWarningText()).containsText(
        "You are about to permanently remove webhook for " + url);
  }

  public void shouldShowSubmitDisabled() {
    assertThat(page.submitButton()).isDisabled();
  }

  public void shouldShowSubmitEnabled() {
    assertThat(page.submitButton()).isEnabled();
  }

  public void shouldShowFieldsPrePopulated(String expectedUrl) {
    assertThat(page.urlInput()).hasValue(expectedUrl);
  }

  public void shouldShowFieldsPrePopulatedWithMaskedSecret(String expectedUrl, String expectedSelectedEventType) {
    assertThat(page.urlInput()).hasValue(expectedUrl);
    assertThat(page.secretKeyInput()).hasValue(Webhook.FAKE_SECRET_KEY);
    assertThat(page.eventTypeCheckboxInput(expectedSelectedEventType)).isChecked();
  }

  public void shouldShowEventTypeChecked(String eventTypeName) {
    assertThat(page.eventTypeCheckboxInput(eventTypeName)).isChecked();
  }

  public void shouldShowLoadErrorContaining(String expectedText) {
    assertThat(page.loadError()).isVisible();
    assertThat(page.loadError()).containsText(expectedText);
  }

  public void shouldShowEventTypesAlphabetically() {
    assertThat(page.eventTypeLabels().first()).isVisible();
    List<String> labels = page.eventTypeLabels()
        .allInnerTexts()
        .stream()
        .map(String::trim)
        .toList();
    Assertions.assertThat(labels)
        .as("event type checkboxes should be listed alphabetically")
        .isSortedAccordingTo(Comparator.naturalOrder());
  }
}
