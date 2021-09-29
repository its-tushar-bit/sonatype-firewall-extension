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
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;

public class AutomaticSourceControlConfigurationTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void startup() {
    refreshOrOpen(AutomaticSourceControlConfigurationPage.url());
    loginAsAdmin();
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

    // check that configuration settings can be saved
    configurationPage.toggle().click();
    eyesWatcher.eyesCheck();
    configurationPage.update().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    configurationPage.update().shouldBe(CLM.DISABLED);
    verifyConfiguration(true);

    // check that the updated configuration is displayed on refresh
    refresh();
    configurationPage.toggle().input().shouldNotBe(CLM.DISABLED);

    // check that local changes to configuration settings can be cancelled
    configurationPage.toggle().click();
    configurationPage.update().shouldNotBe(CLM.DISABLED);
    configurationPage.cancel().shouldNotBe(disabled).click();
    configurationPage.toggle().input().shouldNotBe(CLM.DISABLED);
    configurationPage.update().shouldBe(CLM.DISABLED);
    configurationPage.cancel().shouldBe(disabled);

    // check that subsequent changes to the configuration are also persisted
    configurationPage.toggle().click();
    configurationPage.update().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    configurationPage.update().shouldBe(CLM.DISABLED);
    verifyConfiguration(false);
  }

  @Test
  public void automaticSourceControlConfigurationTest_explanationAutomaticApplications() {
    // given automatic applications are enabled
    Organization organization = tempEntity.newOrganizationAutomaticApplicationsConfiguration();

    // and source control is configured
    tempEntity.newSourceControl(organization.getId(), null, "token", GITHUB);

    // when opening the automatic source control page
    AutomaticSourceControlConfigurationPage configurationPage =
        new AutomaticSourceControlConfigurationPage();
    refreshOrOpen(AutomaticSourceControlConfigurationPage.url());

    // then a description about usage of automatic applications is visible
    eyesWatcher.eyesCheck();
    configurationPage.explanationAutomaticApplications().shouldBe(visible);
  }

  private void verifyConfiguration(boolean enabled) {
    AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO =
        new AutomaticSourceControlConfigurationDAO();

    assertThat(automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled()).isEqualTo(enabled);
  }
}
