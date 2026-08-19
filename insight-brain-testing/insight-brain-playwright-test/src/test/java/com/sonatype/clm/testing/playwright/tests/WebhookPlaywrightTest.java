/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.WebhookEditorPage;
import com.sonatype.clm.testing.playwright.pages.WebhookEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.WebhookListPage;
import com.sonatype.clm.testing.playwright.pages.WebhookListPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WebhookPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String WEBHOOK_URL_PREFIX = "https://hooks.example.com/pw-";

  private static final String WEBHOOK_HTTP_URL = "http://hooks.example.com/pw-http-test";

  private static final String WEBHOOK_SECRET_KEY = "pw-secret-key";

  private WebhookListPage listPage;

  private WebhookListPageAssertions listAssertions;

  private WebhookEditorPage editorPage;

  private WebhookEditorPageAssertions editorAssertions;

  @BeforeEach
  public void setUp() {
    listPage = new WebhookListPage();
    listAssertions = new WebhookListPageAssertions(listPage);
    editorPage = new WebhookEditorPage();
    editorAssertions = new WebhookEditorPageAssertions(editorPage);
    playwrightRefreshOrOpen(WebhookListPage.url());
    playwrightLogin();
  }

  @AfterEach
  public void cleanup() {
    deleteAllTestWebhooks();
  }

  @Test
  @Tag("regression")
  public void testWebhookListRendersEmptyStateAndAddNavigates() {
    assertThat(listPage.container()).isVisible();

    listAssertions.shouldRenderPageLayout();
    listAssertions.shouldShowEmptyState();

    listPage.addWebhookButton().click();
    editorAssertions.shouldShowCreateMode();
  }

  @Test
  @Tag("regression")
  public void testCreateWebhookUrlValidationAndHttpWarning() {
    listPage.addWebhookButton().click();
    assertThat(editorPage.container()).isVisible();

    editorAssertions.shouldShowValidationError("Webhook URL is a required field");

    editorPage.urlInput().fill(WEBHOOK_HTTP_URL);
    editorPage.eventTypeCheckbox("Application Evaluation").click();
    editorAssertions.shouldShowHttpInfoAlert();
    editorPage.submitButton().click();

    editorAssertions.shouldShowHttpWarningModal();
    editorPage.httpWarningModalContinueButton().click();
    waitForSubmitMaskSuccess();

    listAssertions.shouldShowWebhookInList(WEBHOOK_HTTP_URL);
  }

  @Test
  @Tag("regression")
  public void testCreateWebhookEventTypesAndSuccessfulSave() {
    String webhookUrl = WEBHOOK_URL_PREFIX + TemporaryEntity.uuid();

    listPage.addWebhookButton().click();
    assertThat(editorPage.container()).isVisible();

    assertThat(editorPage.eventTypesFieldset()).isVisible();
    assertThat(editorPage.eventTypeCheckbox("Application Evaluation")).isVisible();
    assertThat(editorPage.eventTypeCheckbox("Violation Alert")).isVisible();
    assertThat(editorPage.eventTypeCheckbox("Policy Management")).isVisible();
    editorAssertions.shouldShowEventTypesAlphabetically();

    editorPage.urlInput().fill(webhookUrl);
    editorPage.eventTypeCheckbox("Violation Alert").click();
    editorAssertions.shouldShowEventTypeChecked("Violation Alert");
    editorPage.submitButton().click();
    waitForSubmitMaskSuccess();

    listAssertions.shouldShowWebhookInList(webhookUrl);
  }

  @Test
  @Tag("regression")
  public void testCreateWebhook_missingApplicationLicense_pageFailsToLoad() {
    setMissingFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS, LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);
    playwrightRefreshOrOpen(WebhookEditorPage.createUrl());

    editorAssertions.shouldShowLoadErrorContaining(
        "Webhooks feature is not supported by your license");
  }

  @Test
  @Tag("regression")
  public void testEditWebhookPrePopulatedFieldsUpdateAndCancel() {
    String webhookUrl = WEBHOOK_URL_PREFIX + TemporaryEntity.uuid();

    listPage.addWebhookButton().click();
    assertThat(editorPage.container()).isVisible();
    editorPage.urlInput().fill(webhookUrl);
    editorPage.secretKeyInput().fill(WEBHOOK_SECRET_KEY);
    editorPage.eventTypeCheckbox("Policy Management").click();
    editorPage.submitButton().click();
    waitForSubmitMaskSuccess();

    assertThat(listPage.container()).isVisible();
    listPage.webhookItemByUrl(webhookUrl).click();

    assertThat(editorPage.container()).isVisible();
    editorAssertions.shouldShowEditMode();
    editorAssertions.shouldShowFieldsPrePopulatedWithMaskedSecret(webhookUrl, "Policy Management");
    editorAssertions.shouldShowValidationError("There are no changes to update");

    editorPage.eventTypeCheckbox("Violation Alert").click();
    editorAssertions.shouldShowSubmitEnabled();
    editorPage.submitButton().click();
    waitForSubmitMaskSuccess();

    assertThat(listPage.container()).isVisible();

    listPage.webhookItemByUrl(webhookUrl).click();
    assertThat(editorPage.container()).isVisible();
    editorPage.cancelButton().click();

    assertThat(listPage.container()).isVisible();
  }

  @Test
  @Tag("regression")
  public void testDeleteWebhookConfirmationAndRemoval() {
    String webhookUrl = WEBHOOK_URL_PREFIX + TemporaryEntity.uuid();

    listPage.addWebhookButton().click();
    assertThat(editorPage.container()).isVisible();
    editorPage.urlInput().fill(webhookUrl);
    editorPage.eventTypeCheckbox("Policy Management").click();
    editorPage.submitButton().click();
    waitForSubmitMaskSuccess();

    assertThat(listPage.container()).isVisible();
    listPage.webhookItemByUrl(webhookUrl).click();

    assertThat(editorPage.container()).isVisible();
    editorPage.deleteButton().click();

    editorAssertions.shouldShowDeleteModal();
    editorAssertions.shouldShowDeleteModalWarning(webhookUrl);
    editorPage.deleteModalContinueButton().click();
    waitForSubmitMaskSuccess();

    listAssertions.shouldNotShowWebhookInList(webhookUrl);
  }

  private void deleteAllTestWebhooks() {
    WebhookDAO webhookDAO = lookup(WebhookDAO.class);
    try (TransactionContext tx = webhookDAO.createTransactionContext()) {
      tx.begin();
      webhookDAO.getAll(tx)
          .stream()
          .filter(w -> w.getUrl() != null
              && (w.getUrl().startsWith(WEBHOOK_URL_PREFIX) || WEBHOOK_HTTP_URL.equals(w.getUrl())))
          .forEach(w -> webhookDAO.delete(tx, w));
      tx.commit();
    }
  }
}
