/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage.WebhookSummary;
import com.sonatype.clm.testing.functional.pages.WebhookEditPage;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public class WebhookConfigurationTest
    extends AbstractFunctionalTest
{
  private WebhookDAO webhookDAO = new WebhookDAO();
  private WebhookEditPage webhookEditPage = new WebhookEditPage();
  private WebhookConfigurationPage webhookConfigurationPage = new WebhookConfigurationPage();

  @BeforeClass
  public static void startup() {
    Selenide.open(WebhookConfigurationPage.URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    insertWebhooks();
    refreshOrOpen(WebhookConfigurationPage.URL);
    webhookConfigurationPage.should(appear);
  }

  @After
  public void end() {
    List<Webhook> allWebhooks = webhookDAO.getAll();
    for (Webhook webhook : allWebhooks) {
      webhookDAO.delete(webhook);
    }
  }

  @Test
  public void testShowsListOfExistingWebhooks() {
    ElementsCollection webhooks = webhookConfigurationPage.webhooksList();

    webhooks.shouldHaveSize(3);
    webhooks.shouldHave(texts("http://localhost0", "http://localhost1", "http://localhost2"));
  }

  @Test
  public void testCanAddNewWebhook() {
    SelenideElement newWebhook = webhookConfigurationPage.newWebhook();
    newWebhook.shouldBe(visible);

    newWebhook.click();
    webhookEditPage.should(appear);
    webhookEditPage.title().should(text("Create Webhook"));

    webhookEditPage.url().val("http://foo.bar");

    webhookEditPage.secretKey().val("sooper sekrit");
    webhookEditPage.applicationEvaluation().click();

    webhookEditPage.save().shouldBe(enabled).click();

    webhookConfigurationPage.should(appear);
    ElementsCollection webhooks = webhookConfigurationPage.webhooksList();

    webhooks.shouldHaveSize(4);
    webhooks.shouldHave(texts("http://localhost0", "http://localhost1", "http://localhost2", "http://foo.bar"));
    WebhookSummary webhookSummary = webhookConfigurationPage.webhook(3);
    webhookSummary.webhookEvents().shouldHave(texts("Application Evaluation"));
  }

  @Test
  public void testCanNavigateToAndEditWebhook() {
    WebhookSummary firstWebhook = webhookConfigurationPage.webhook(0);
    firstWebhook.click();

    webhookEditPage.should(appear);
    webhookEditPage.url().shouldHave(value("http://localhost0"));
    webhookEditPage.secretKey().shouldHave(value("#~FAKE~SECRET~KEY~#"));
    webhookEditPage.management().shouldBe(selected);
    webhookEditPage.component().shouldNotBe(selected);
    webhookEditPage.applicationEvaluation().shouldNotBe(selected);

    webhookEditPage.url().val("");

    webhookEditPage.url().val("http://foo.bar");

    webhookEditPage.management().click();
    webhookEditPage.component().click();

    webhookEditPage.save().shouldBe(enabled).click();

    webhookConfigurationPage.should(appear);
    firstWebhook = webhookConfigurationPage.webhook(0);

    firstWebhook.title().shouldHave(text("http://foo.bar"));
    firstWebhook.webhookEvents().shouldHave(texts("License Override"));
  }

  private void insertWebhooks() {
    for (int i = 0; i < 3; i++) {
      tempEntity.newWebhook("http://localhost" + i, Sets.newSet(WebhookEventType.POLICY_MANAGEMENT));
    }
  }
}
