/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.AtlassianCrowdConfigurationDeleteModal;
import com.sonatype.clm.testing.functional.pages.AtlassianCrowdConfigurationPage;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.security.CrowdMockServerRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public class AtlassianCrowdConfigurationPageTest
    extends AbstractFunctionalTest
{
  @Rule
  public CrowdMockServerRule crowdMockServer = new CrowdMockServerRule();

  @Before
  public void before() {
    refreshOrOpen(AtlassianCrowdConfigurationPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    logout();
    clearAlerts();
    lookup(CrowdConfigurationDAO.class).delete();
  }

  @Test
  public void testDefaultState() {
    AtlassianCrowdConfigurationPage atlassianCrowdConfigurationPage = new AtlassianCrowdConfigurationPage();
    eyesWatcher.eyesCheck("crowd configuration editor on top");
    assertDefaultState(atlassianCrowdConfigurationPage);
  }

  @Test
  public void testCRUD() {
    AtlassianCrowdConfigurationPage atlassianCrowdConfigurationPage = new AtlassianCrowdConfigurationPage();
    atlassianCrowdConfigurationPage = fillFormData();
    atlassianCrowdConfigurationPage.saveButton().click();

    logout();
    refreshOrOpen(AtlassianCrowdConfigurationPage.url());
    loginAsAdmin();

    // Once saved data is re-loaded
    atlassianCrowdConfigurationPage.serverUrlAttribute().shouldHave(value("http://localhost:8095/crowd"));
    atlassianCrowdConfigurationPage.applicationNameAttribute().shouldHave(value("Sonatype"));
    atlassianCrowdConfigurationPage.applicationPasswordAttribute().shouldHave(value("\u0000\u0000\u0000\u0000\u0000"));
    atlassianCrowdConfigurationPage.deleteButton().shouldBe(enabled);
    atlassianCrowdConfigurationPage.saveButton().shouldBe(enabled);
    atlassianCrowdConfigurationPage.cancelButton().shouldBe(enabled);
    atlassianCrowdConfigurationPage.testButton().shouldBe(enabled);

    // If a crowd config is loaded, then it can be deleted
    atlassianCrowdConfigurationPage.deleteButton().shouldBe(enabled);
    atlassianCrowdConfigurationPage.deleteButton().click();
    AtlassianCrowdConfigurationDeleteModal deleteModal = new AtlassianCrowdConfigurationDeleteModal();
    deleteModal.should(appear);
    deleteModal.cancelButton().shouldBe(enabled);
    deleteModal.okButton().shouldBe(enabled).click();
    deleteModal.should(disappear);
    FormMask.seeAndWaitForDismissal();

    // Deleting a crowd conf sets the controls to default values
    assertDefaultState(atlassianCrowdConfigurationPage);
  }

  @Test
  public void testCancel() {
    AtlassianCrowdConfigurationPage atlassianCrowdConfigurationPage = new AtlassianCrowdConfigurationPage();
    atlassianCrowdConfigurationPage = fillFormData();

    // If the form is dirty, the cancel button should be enabled
    atlassianCrowdConfigurationPage.cancelButton().shouldBe(enabled);
    atlassianCrowdConfigurationPage.cancelButton().click();

    // The form should be reset
    assertDefaultState(atlassianCrowdConfigurationPage);
  }

  @Test
  public void testTestConnection() throws Exception {
    crowdMockServer.mockTestConnection();
    AtlassianCrowdConfigurationPage atlassianCrowdConfigurationPage = fillFormData();
    atlassianCrowdConfigurationPage.serverUrlAttribute().clear();
    atlassianCrowdConfigurationPage.serverUrlAttribute().sendKeys(crowdMockServer.getBaseUrl() + "/crowd");
    atlassianCrowdConfigurationPage.testButton().shouldBe(enabled).click();
    atlassianCrowdConfigurationPage.successAlertBox().shouldBe(visible).shouldHave(text("Success!"));
  }

  private void assertDefaultState(AtlassianCrowdConfigurationPage atlassianCrowdConfigurationPage) {
    atlassianCrowdConfigurationPage.serverUrlAttribute().shouldBe(empty);
    atlassianCrowdConfigurationPage.applicationNameAttribute().shouldBe(empty);
    atlassianCrowdConfigurationPage.applicationPasswordAttribute().shouldBe(empty);
    atlassianCrowdConfigurationPage.saveButton().shouldBe(enabled);
    atlassianCrowdConfigurationPage.cancelButton().shouldBe(enabled);
    atlassianCrowdConfigurationPage.deleteButton().shouldBe(disabled);
    atlassianCrowdConfigurationPage.testButton().shouldBe(disabled);
  }

  private AtlassianCrowdConfigurationPage fillFormData() {
    AtlassianCrowdConfigurationPage atlassianCrowdConfigurationPage = new AtlassianCrowdConfigurationPage();
    atlassianCrowdConfigurationPage.serverUrlAttribute().clear();
    atlassianCrowdConfigurationPage.serverUrlAttribute().sendKeys("http://localhost:8095/crowd");
    atlassianCrowdConfigurationPage.applicationNameAttribute().clear();
    atlassianCrowdConfigurationPage.applicationNameAttribute().sendKeys("Sonatype");
    atlassianCrowdConfigurationPage.applicationPasswordAttribute().clear();
    atlassianCrowdConfigurationPage.applicationPasswordAttribute().sendKeys("admin123");
    return atlassianCrowdConfigurationPage;
  }
}
