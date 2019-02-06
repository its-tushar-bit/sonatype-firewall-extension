/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionList;
import com.sonatype.clm.testing.functional.elements.ActionList.ActionListElement;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;
import com.sonatype.clm.testing.functional.pages.WebhookEditPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.ElementsCollection;
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
  
  private List<Webhook> webhookList = new ArrayList<>();

  @BeforeClass
  public static void startup() {
    refreshOrOpen(WebhookConfigurationPage.URL);
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
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testCanAddNewWebhook() {
    SelenideElement newWebhook = webhookConfigurationPage.newWebhook();
    newWebhook.shouldBe(visible);

    newWebhook.click();
    webhookEditPage.should(appear);
    webhookEditPage.title().shouldHave(text("Create Webhook"));

    webhookEditPage.backButton().shouldHave(text("Back to Webhook Configuration")).click();
    webhookConfigurationPage.should(appear);

    newWebhook.shouldBe(visible).click();
    webhookEditPage.should(appear);

    webhookEditPage.url().val("http://foo.bar");

    webhookEditPage.secretKey().val("sooper sekrit");
    webhookEditPage.applicationEvaluation().click();

    webhookEditPage.save().shouldHave(text("Create"));
    eyesWatcher.eyesCheck();
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

    webhookEditPage.backButton().shouldHave(text("Back to Webhook Configuration")).click();
    webhookConfigurationPage.should(appear);

    firstWebhook.shouldBe(visible).click();
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

  @Test
  public void testInvalidWebhookIDErrorMessage() {
    //navigate to a non-existing webhook
    String badWebhookIdUrl = BaseUrl.resolvePageUrl("/webhooks/BAD_ID");
    refreshOrOpen(badWebhookIdUrl);

    webhookEditPage.should(appear);
    webhookEditPage.form().shouldNot(appear);
    
    webhookEditPage.errorAlert().should(appear);
    // Re-trying a bad webhook ID won't change the page.
    webhookEditPage.errorAlert().retryButton().should(appear)
        .shouldHave(text("Retry")).click();

    webhookEditPage.errorAlert()
        .shouldHave(text("An error occurred loading data. Could not find an webhook with ID BAD_ID."));

    webhookEditPage.backButton().shouldHave(text("Back to Webhook Configuration")).click();
    webhookConfigurationPage.should(appear);
  }

  @Test
  public void testWebhooks_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    
    // make sure we display an error when navigating directly to webhooks pages
    String notLicensedText = "Webhooks feature is not supported by your license.";

    refreshOrOpen(WebhookConfigurationPage.URL);
    webhookConfigurationPage.shouldHave(text(notLicensedText));
    
    refreshOrOpen(WebhookEditPage.url(webhookList.get(0).getId()));
    webhookEditPage.shouldHave(text(notLicensedText));
  }

  private void insertWebhooks() {
    for (int i = 0; i < 3; i++) {
      webhookList.add(tempEntity.newWebhook("http://localhost" + i, Sets.newSet(WebhookEventType.POLICY_MANAGEMENT)));
    }
  }
}
