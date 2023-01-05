/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage.WebhookListElement;
import com.sonatype.clm.testing.functional.utils.InputUtils;
import com.sonatype.clm.testing.functional.pages.WebhookEditPage;
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
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;

public class WebhookConfigurationTest
    extends AbstractFunctionalTest
{
  private final WebhookDAO webhookDAO = new WebhookDAO();

  private final WebhookEditPage webhookEditPage = new WebhookEditPage();

  private final WebhookConfigurationPage webhookConfigurationPage = new WebhookConfigurationPage();
  
  private final List<Webhook> webhookList = new ArrayList<>();

  @BeforeClass
  public static void startup() {
    refreshOrOpen(WebhookConfigurationPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    insertWebhooks();
    refreshOrOpen(WebhookConfigurationPage.url());
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
    ElementsCollection webhooks = webhookConfigurationPage.webhooks();

    webhooks.shouldHaveSize(3);
    webhooks.shouldHave(texts("http://localhost0", "http://localhost1", "http://localhost2"));

    // click on page title before eyesCheck to avoid random mouse-over style
    webhookConfigurationPage.pageTitle().click();
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testCanAddNewWebhook() {
    SelenideElement newWebhook = webhookConfigurationPage.newWebhook();
    newWebhook.shouldBe(visible);

    newWebhook.click();
    webhookEditPage.should(appear);
    webhookEditPage.title().shouldHave(text("Create Webhook"));

    webhookEditPage.backButton().shouldHave(text("Back to Webhooks")).click();
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

    webhookConfigurationPage.webhooks()
        .shouldHave(texts("http://localhost0", "http://localhost1", "http://localhost2", "http://foo.bar"));
    webhookConfigurationPage.webhook(3).shouldHave(text("Application Evaluation"));
  }

  @Test
  public void testCanNavigateToAndEditWebhook() {
    WebhookListElement firstWebhook = webhookConfigurationPage.webhook(0);
    firstWebhook.link().click();

    webhookEditPage.should(appear);

    webhookEditPage.backButton().shouldHave(text("Back to Webhooks")).click();
    webhookConfigurationPage.should(appear);

    firstWebhook.link().click();
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
    firstWebhook = webhookConfigurationPage.webhook(2);

    firstWebhook.text().shouldHave(text("http://foo.bar"));
    firstWebhook.subtext().shouldHave(text("License Override"));
  }

  @Test
  public void testCanRemoveWebhook() {

    for (int i = 0; i < 3; i++) {
      webhookConfigurationPage.webhook(0).link().click();
      webhookEditPage.should(appear);
      
      String url = webhookEditPage.url().getValue();
      webhookEditPage.remove().shouldBe(enabled).click();

      NxDeleteModal deleteModal = new NxDeleteModal("#delete-modal");

      deleteModal.header().shouldHave(text("Delete Webhook"));
      deleteModal.alertContent().shouldHave(text("You are about to permanently remove webhook for " +
          url + ". This action cannot be undone."));
      deleteModal.submitButton().click();
      deleteModal.should(disappear);

      webhookConfigurationPage.should(appear);
      webhookConfigurationPage.webhooks().shouldHave(size(2 - i));
    }

    webhookConfigurationPage.emptyListMessage().shouldBe(visible).shouldHave(text("No webhooks are defined"));
  }

  @Test
  public void testInvalidWebhookIDErrorMessage() {
    //navigate to a non-existing webhook
    String badWebhookIdUrl = WebhookEditPage.url("BAD_ID");
    refreshOrOpen(badWebhookIdUrl);

    webhookEditPage.should(appear);
    webhookEditPage.form().shouldNot(appear);
    
    webhookEditPage.errorAlert().should(appear);
    // Re-trying a bad webhook ID won't change the page.
    webhookEditPage.errorAlert().retryButton().should(appear)
        .shouldHave(text("Retry")).click();

    webhookEditPage.errorAlert()
        .shouldHave(text("An error occurred loading data. Unable to locate webhook"));

    webhookEditPage.backButton().shouldHave(text("Back to Webhooks")).click();
    webhookConfigurationPage.should(appear);
  }

  @Test
  public void testUnsavedChangesModal() {
    UnsavedModal unsavedChangesModal = new UnsavedModal();

    //Unsaved changes when creating webhook

    SelenideElement newWebhook = webhookConfigurationPage.newWebhook();
    newWebhook.shouldBe(visible);

    newWebhook.click();
    webhookEditPage.url().setValue("foo");
    
    refreshOrOpen(WebhookConfigurationPage.url());
    unsavedChangesModal.should(appear);
    unsavedChangesModal.cancelButton().click();

    webhookEditPage.applicationEvaluation().click();
    webhookEditPage.applicationEvaluation().shouldBe(selected);
    InputUtils.clearInput(webhookEditPage.url());

    refreshOrOpen(WebhookConfigurationPage.url());
    unsavedChangesModal.should(appear);
    unsavedChangesModal.cancelButton().click();

    webhookEditPage.applicationEvaluation().click();
    webhookEditPage.applicationEvaluation().shouldNotBe(selected);
    refreshOrOpen(WebhookConfigurationPage.url());

    unsavedChangesModal.shouldNot(appear);

    //Unsaved changes when editing webhook
    webhookConfigurationPage.webhook(0).link().click();

    String previousUrl = webhookEditPage.url().val();
    
    webhookEditPage.url().val("isDirty");
    refreshOrOpen(WebhookConfigurationPage.url());
    unsavedChangesModal.should(appear);
    unsavedChangesModal.cancelButton().click();

    webhookEditPage.url().val(previousUrl);
    webhookEditPage.applicationEvaluation().click();
    refreshOrOpen(WebhookConfigurationPage.url());
    unsavedChangesModal.should(appear);
    unsavedChangesModal.cancelButton().click();

    webhookEditPage.applicationEvaluation().click();
    refreshOrOpen(WebhookConfigurationPage.url());
    unsavedChangesModal.shouldNot(appear);

  }

  @Test
  public void testWebhooks_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    
    // make sure we display an error when navigating directly to webhooks pages
    String notLicensedText = "Webhooks feature is not supported by your license.";

    refreshOrOpen(WebhookConfigurationPage.url());
    webhookConfigurationPage.shouldHave(text(notLicensedText));
    
    refreshOrOpen(WebhookEditPage.url(webhookList.get(0).getId()));
    webhookEditPage.shouldHave(text(notLicensedText));
  }

  @Test
  public void testWebhooks_Foundation_Firewall() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);

    refreshOrOpen(WebhookConfigurationPage.url());
    webhookConfigurationPage.should(appear);

    refreshOrOpen(WebhookEditPage.url(webhookList.get(0).getId()));

    webhookEditPage.should(appear);
    WebhookEditPage.disabledApplicationEvaluationMessage()
        .shouldBe(text("Webhooks with Application Evaluation event types are not supported by your license."));

    webhookEditPage.applicationEvaluation().shouldBe(visible, disabled).shouldNotBe(selected).click();
    webhookEditPage.applicationEvaluation().shouldNotBe(selected);

    webhookEditPage.save().shouldHave(text("Update"), DISABLED);
  }

  private void insertWebhooks() {
    for (int i = 0; i < 3; i++) {
      Webhook newWebhook = tempEntity.newWebhookWithSecret("http://localhost" + i, 
          Sets.newSet(WebhookEventType.POLICY_MANAGEMENT), "");
      webhookList.add(newWebhook);
    }
  }
}
