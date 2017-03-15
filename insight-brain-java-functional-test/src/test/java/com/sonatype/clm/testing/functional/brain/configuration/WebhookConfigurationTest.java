/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionList;
import com.sonatype.clm.testing.functional.elements.ActionList.ActionListElement;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;
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

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.DeleteModal.bodyText;
import static com.sonatype.clm.testing.functional.elements.DeleteModal.headerText;

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
    ElementsCollection webhooks = webhookConfigurationPage.webhooksList().elements();

    webhooks.shouldHaveSize(3);
    webhooks.shouldHave(texts("http://localhost0", "http://localhost1", "http://localhost2"));
  }

  @Test
  public void testCanAddNewWebhook() {
    SelenideElement newWebhook = webhookConfigurationPage.newWebhook();
    newWebhook.shouldBe(visible);

    newWebhook.click();
    webhookEditPage.should(appear);
    webhookEditPage.title().shouldHave(text("Create Webhook"));

    webhookEditPage.url().val("http://foo.bar");

    webhookEditPage.secretKey().val("sooper sekrit");
    webhookEditPage.applicationEvaluation().click();

    webhookEditPage.save().shouldHave(text("Create"));
    webhookEditPage.save().shouldBe(enabled).click();

    webhookConfigurationPage.should(appear);
    ActionList webhooks = webhookConfigurationPage.webhooksList();

    webhooks.elements()
        .shouldHave(texts("http://localhost0", "http://localhost1", "http://localhost2", "http://foo.bar"));
    webhooks.element(3).subtext().shouldHave(text("Application Evaluation"));
  }

  @Test
  public void testCanNavigateToAndEditWebhook() {
    ActionListElement firstWebhook = webhookConfigurationPage.webhooksList().element(0);
    firstWebhook.click();

    webhookEditPage.should(appear);
    webhookEditPage.url().shouldHave(value("http://localhost0"));
    webhookEditPage.title().shouldHave(text("Edit Webhook"));
    webhookEditPage.secretKey().shouldHave(value("#~FAKE~SECRET~KEY~#"));
    webhookEditPage.management().shouldBe(selected);
    webhookEditPage.component().shouldNotBe(selected);
    webhookEditPage.applicationEvaluation().shouldNotBe(selected);

    webhookEditPage.url().val("");

    webhookEditPage.url().val("http://foo.bar");

    webhookEditPage.management().click();
    webhookEditPage.component().click();

    webhookEditPage.save().shouldHave(text("Update"));
    webhookEditPage.save().shouldBe(enabled).click();

    webhookConfigurationPage.should(appear);
    firstWebhook = webhookConfigurationPage.webhooksList().element(0);

    firstWebhook.text().shouldHave(text("http://foo.bar"));
    firstWebhook.subtext().shouldHave(text("License Override"));
  }

  @Test
  public void testCanRemoveWebhook() {
    ActionList webhooks = webhookConfigurationPage.webhooksList();

    for (int i = 0; i < 3; i++) {
      webhooks.element(0).click();
      webhookEditPage.should(appear);
      webhookEditPage.remove().shouldBe(enabled).click();
      DeleteModal.body().should(appear).shouldHave(bodyText("http://localhost" + i));
      DeleteModal.header().shouldHave(headerText("Webhook"));
      DeleteModal.continueButton().shouldBe(enabled).click();
      FormMask.seeAndWaitForDismissal();
      DeleteModal.body().should(disappear);
      webhookConfigurationPage.should(appear);
      webhooks.elements().shouldHave(size(2 - i));
    }

    webhooks.emptyDescriptor().shouldBe(visible).shouldHave(text("No webhooks are defined"));
  }

  private void insertWebhooks() {
    for (int i = 0; i < 3; i++) {
      tempEntity.newWebhook("http://localhost" + i, Sets.newSet(WebhookEventType.POLICY_MANAGEMENT));
    }
  }
}
