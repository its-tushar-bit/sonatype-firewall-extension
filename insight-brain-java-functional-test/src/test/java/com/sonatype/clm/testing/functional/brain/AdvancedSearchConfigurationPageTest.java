/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchConfigurationPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.index.IndexService;

import com.codeborne.selenide.Configuration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;
import static java.lang.Boolean.parseBoolean;
import static org.assertj.core.api.Assertions.assertThat;

public class AdvancedSearchConfigurationPageTest
    extends AbstractFunctionalTest
{
  private final AdvancedSearchConfigurationPage page = new AdvancedSearchConfigurationPage();

  private SystemConfigurationPropertyDAO dao;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(AdvancedSearchConfigurationPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    dao = lookup(SystemConfigurationPropertyDAO.class);
  }

  @Test
  public void testOptIn() {
    dao.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "false"));
    refreshOrOpen(AdvancedSearchConfigurationPage.url());

    // Verify page is in expected state
    page.isEnabledCheckbox().shouldNotBe(selected).shouldBe(enabled);
    page.reIndexButton().shouldBe(disabled);
    page.cancelButton().shouldBe(disabled);
    page.saveButton().shouldBe(visible);

    // Opt-In
    page.isEnabledCheckbox().click();
    saveForm();

    // Verify page is in expected state after opting in
    SidebarNavigation.advancedSearchNavigationButton().shouldBe(visible);
    page.isEnabledCheckbox().shouldBe(selected).shouldBe(enabled);
    page.reIndexButton().shouldBe(enabled);
    page.cancelButton().shouldBe(disabled);

    // Verify state in backend
    assertThat(isAdvancedSearchEnabled()).isTrue();
  }

  @Test
  public void testOptOut() {
    dao.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "true"));
    refreshOrOpen(AdvancedSearchConfigurationPage.url());

    // Verify page is in expected state
    page.isEnabledCheckbox().shouldBe(selected).shouldBe(enabled);
    page.reIndexButton().shouldBe(enabled);
    page.cancelButton().shouldBe(disabled);

    // Opt-Out
    page.isEnabledCheckbox().click();
    saveForm();

    // Verify page is in expected state after opting out
    SidebarNavigation.advancedSearchNavigationButton().shouldBe(hidden);
    page.isEnabledCheckbox().shouldNotBe(selected).shouldBe(enabled);
    page.reIndexButton().shouldBe(disabled);
    page.cancelButton().shouldBe(disabled);

    // Verify state in backend
    assertThat(isAdvancedSearchEnabled()).isFalse();
  }

  @Test
  public void testReindex() throws Exception {
    TaskScheduler taskScheduler = testCLMServer.getCLMServer().getInstance(TaskScheduler.class);
    taskScheduler.disableForTesting = false;
    taskScheduler.start();
    IndexService indexService = testCLMServer.getCLMServer().getInstance(IndexService.class);
    indexService.disableForTesting = false;
    indexService.register();

    dao.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "true"));
    refreshOrOpen(AdvancedSearchConfigurationPage.url());

    page.reIndexButton().shouldBe(enabled).click();
    long currentTimeout = Configuration.timeout;
    try {
      // An Advanced Search reindex can take longer than the normal timeout to complete
      Configuration.timeout = 10000;
      page.lastIndexTime().should(matchText(".+"));
    }
    finally {
      Configuration.timeout = currentTimeout;
    }
  }

  @Test
  public void testResetForm() {
    dao.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "false"));
    refreshOrOpen(AdvancedSearchConfigurationPage.url());
    page.cancelButton().shouldBe(disabled);

    page.isEnabledCheckbox().shouldNotBe(selected).click();
    page.cancelButton().shouldBe(enabled).click();

    page.isEnabledCheckbox().shouldNotBe(selected);
  }

  @Test
  public void testCanOnlyReIndexWhenOptInIsChecked_OptedOutState() {
    dao.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "false"));
    refreshOrOpen(AdvancedSearchConfigurationPage.url());

    // By default I should not be able to trigger indexing.
    page.isEnabledCheckbox().shouldNotBe(selected);
    page.reIndexButton().shouldBe(disabled);

    // The moment I click the checkbox..
    page.isEnabledCheckbox().click();
    page.isEnabledCheckbox().shouldBe(selected);

    // ..I should be able to trigger indexing.
    page.reIndexButton().shouldBe(enabled);
  }

  @Test
  public void testCanOnlyReIndexWhenOptInIsChecked_OptedInState() {
    dao.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "true"));
    refreshOrOpen(AdvancedSearchConfigurationPage.url());

    // By default I should be able to trigger indexing.
    page.isEnabledCheckbox().shouldBe(selected);
    page.reIndexButton().shouldBe(enabled);

    // The moment I click the checkbox (i.e. uncheck it..)
    page.isEnabledCheckbox().click();
    page.isEnabledCheckbox().shouldNotBe(selected);

    // ..I should not be able to trigger indexing anymore
    page.reIndexButton().shouldBe(disabled);
  }

  @Test
  public void testUnauthorizedUserCannotSee() {
    try {
      User user = tempEntity.newUser("username", "john", "doe", "john@doe");
      refreshOrOpen(DashboardPage.url());
      logout();
      login(user.getUsername(), user.getPassword());
      refreshOrOpen(AdvancedSearchConfigurationPage.url());
      page.reIndexButton().shouldNotBe(visible);
      page.reIndexButton().shouldNotBe(visible);
      page.saveButton().shouldNotBe(visible);
    }
    finally {
      logout();
      refreshOrOpen(AdvancedSearchConfigurationPage.url());
      loginAsAdmin();
    }
  }

  @Test
  public void testUnsavedChangesModal() {
    refreshOrOpen(AdvancedSearchConfigurationPage.url());

    page.isEnabledCheckbox().click();

    testUnsavedChangesModal_Cancel();
    testUnsavedChangesModal_Continue();
  }

  private boolean isAdvancedSearchEnabled() {
    return parseBoolean(dao.getByName(ADVANCED_SEARCH_ENABLED).getValue());
  }

  private void saveForm() {
    page.saveButton().click();
    FormMask.seeAndWaitForDismissal();
  }

  private void testUnsavedChangesModal_Cancel() {
    refreshOrOpen(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldNotBe(visible);

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.cancelButton().click();

    DashboardPage.dashboardContainer().shouldNotBe(visible);

    page.title().shouldBe(visible).shouldHave(text("Advanced Search Configuration"));
  }

  private void testUnsavedChangesModal_Continue() {
    refreshOrOpen(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldNotBe(visible);

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.continueButton().click();

    DashboardPage.dashboardContainer().shouldBe(visible);
  }
}
