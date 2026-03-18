/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.AutomaticSourceControlConfigurationPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;

public class AutomaticSourceControlConfigurationTest
    extends AbstractFunctionalTest
{
  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(AutomaticSourceControlConfigurationPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    automaticSourceControlConfigurationDAO = lookup(AutomaticSourceControlConfigurationDAO.class);
  }

  @Test
  public void automaticSourceControlConfigurationTest() {
    AutomaticSourceControlConfigurationPage configurationPage =
        new AutomaticSourceControlConfigurationPage();
    refreshOrOpen(AutomaticSourceControlConfigurationPage.url());

    // check descriptions visibility
    configurationPage.explanation().shouldBe(visible).shouldNotBe(empty);
    configurationPage.explanationAutomaticApplications().shouldNotBe(visible);

    // check initial state
    configurationPage.toggle().input().shouldNotBe(checked);

    // check that updating with no changes shows an error
    configurationPage.update().click();
    configurationPage.update().shouldNotBe(visible);
    FormUtils.getAlertElement(configurationPage)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to update."));
    refresh();

    // check that configuration settings can be saved
    configurationPage.toggle().click();
    eyesWatcher.eyesCheck();
    configurationPage.update().click();
    FormMask.seeAndWaitForDismissal();
    verifyConfiguration(true);

    // check that the updated configuration is displayed on refresh
    refresh();
    configurationPage.toggle().input().shouldNotBe(CLM.DISABLED);

    // check that local changes to configuration settings can be cancelled
    configurationPage.toggle().click();
    configurationPage.update().shouldBe(visible);
    FormUtils.getAlertElement(configurationPage).shouldNotBe(visible);
    configurationPage.cancel().shouldNotBe(disabled).click();
    configurationPage.toggle().input().shouldNotBe(CLM.DISABLED);
    configurationPage.update().shouldBe(visible);
    FormUtils.getAlertElement(configurationPage).shouldNotBe(visible);
    configurationPage.cancel().shouldBe(disabled);

    // check that subsequent changes to the configuration are also persisted
    configurationPage.toggle().click();
    configurationPage.update().shouldBe(visible);
    FormUtils.getAlertElement(configurationPage).shouldNotBe(visible);
    configurationPage.update().click();
    FormMask.seeAndWaitForDismissal();
    verifyConfiguration(false);

  }

  @Test
  public void automaticSourceControlConfigurationTest_explanationAutomaticApplications() {
    // given automatic applications are enabled
    Organization organization = tempEntity.newOrganization("automatic source control test");
    tempEntity.newOrganizationAutomaticApplicationsConfiguration(organization);

    // and source control is configured
    tempEntity.newSourceControl(organization.getId(), null, "token", GITHUB);

    // when opening the automatic source control page
    AutomaticSourceControlConfigurationPage configurationPage =
        new AutomaticSourceControlConfigurationPage();
    refreshOrOpen(AutomaticSourceControlConfigurationPage.url());

    // then a description about usage of automatic applications is visible
    eyesWatcher.eyesCheck();
    configurationPage.explanationAutomaticApplications()
        .shouldBe(visible)
        .shouldHave(text("imported into automatic source control test Organization which uses GitHub."));
  }

  private void verifyConfiguration(boolean enabled) {
    assertThat(automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled()).isEqualTo(enabled);
  }
}
