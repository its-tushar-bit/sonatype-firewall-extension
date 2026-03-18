/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.BaseUrlConfigurationDeleteModal;
import com.sonatype.clm.testing.functional.pages.BaseUrlConfigurationPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.value;

public class BaseUrlConfigurationPageTest
    extends AbstractFunctionalTest
{
  protected String baseUrl = "";

  protected SystemConfigurationPropertyDAO dao;

  protected BaseUrlConfigurationPage baseUrlConfigurationPage;

  @Before
  public void before() {
    dao = lookup(SystemConfigurationPropertyDAO.class);

    baseUrlConfigurationPage = new BaseUrlConfigurationPage();
    refreshOrOpen(baseUrlConfigurationPage.getUrl());
    loginAsAdmin();
    baseUrl = dao.get(SystemConfigurationProperty.BASE_URL);
  }

  @After
  public void after() {
    logout();
    clearAlerts();
  }

  @Test
  public void testDefaultState() {
    eyesWatcher.eyesCheck("base url configuration editor on top");
    assertDefaultState();
  }

  /**
   * The saving, updating and deletion of data was left to be tested manually because the Base URL cannot be altered at
   * test time because it affects the rest of the tests that run in parallel in the Jenkins Job.
   */
  @Test
  public void testCRUD() {
    fillFormData();
    baseUrlConfigurationPage.saveButton().click();

    logout();
    refreshOrOpen(baseUrlConfigurationPage.getUrl());
    loginAsAdmin();

    // Once saved data is re-loaded
    baseUrlConfigurationPage.baseUrlAttribute().shouldHave(value(baseUrl));
    baseUrlConfigurationPage.deleteButton().shouldBe(enabled);
    baseUrlConfigurationPage.saveButton().shouldBe(enabled);
    baseUrlConfigurationPage.cancelButton().shouldBe(disabled);

    // If the Base URL config is loaded, then it can be deleted
    baseUrlConfigurationPage.deleteButton().shouldBe(enabled);
    baseUrlConfigurationPage.deleteButton().click();
    BaseUrlConfigurationDeleteModal deleteModal = new BaseUrlConfigurationDeleteModal();
    deleteModal.should(appear);
    deleteModal.okButton().shouldBe(enabled);
    deleteModal.cancelButton().shouldBe(enabled).click();
    deleteModal.should(disappear);
  }

  @Test
  public void testCancel() {
    baseUrlConfigurationPage.baseUrlAttribute().clear();
    baseUrlConfigurationPage.baseUrlAttribute().sendKeys("test");
    // If the form is dirty, the cancel button should be enabled
    baseUrlConfigurationPage.cancelButton().shouldBe(enabled).click();

    // The form should be reset
    assertDefaultState();
    baseUrlConfigurationPage.deleteButton().shouldBe(enabled);
  }

  public void assertDefaultState() {
    baseUrlConfigurationPage.saveButton().shouldBe(enabled);
    baseUrlConfigurationPage.cancelButton().shouldBe(disabled);
    baseUrlConfigurationPage.baseUrlAttribute().shouldHave(value(baseUrl));
    // For functional tests, there is always a base URL, so the delete button should be enabled
    baseUrlConfigurationPage.deleteButton().shouldBe(enabled);
  }

  private void fillFormData() {
    baseUrlConfigurationPage.baseUrlAttribute().clear();
    baseUrlConfigurationPage.baseUrlAttribute().sendKeys(baseUrl);
  }
}
