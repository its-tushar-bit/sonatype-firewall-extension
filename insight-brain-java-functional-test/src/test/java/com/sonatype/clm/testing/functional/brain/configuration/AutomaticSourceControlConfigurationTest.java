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

import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class AutomaticSourceControlConfigurationTest
    extends AbstractFunctionalTest
{
  @Test
  public void automaticSourceControlConfigurationTest() {
    AutomaticSourceControlConfigurationPage configurationPage =
        new AutomaticSourceControlConfigurationPage();

    refreshOrOpen(AutomaticSourceControlConfigurationPage.URL);
    loginAsAdmin();

    // check description is present
    configurationPage.explanation().shouldBe(visible).shouldNotBe(empty);

    // check initial state
    configurationPage.toggle().shouldBe(visible, enabled).shouldNotBe(checked);

    // check that configuration settings can be saved
    configurationPage.toggle().click();
    eyesWatcher.eyesCheck();
    configurationPage.update().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    configurationPage.update().shouldBe(CLM.DISABLED);
    verifyConfiguration(true);

    // check that the updated configuration is displayed on refresh
    refresh();
    configurationPage.toggle().shouldNotBe(CLM.DISABLED);

    // check that local changes to configuration settings can be cancelled
    configurationPage.toggle().click();
    configurationPage.update().shouldNotBe(CLM.DISABLED);
    configurationPage.cancel().shouldNotBe(disabled).click();
    configurationPage.toggle().shouldNotBe(CLM.DISABLED);
    configurationPage.update().shouldBe(CLM.DISABLED);
    configurationPage.cancel().shouldBe(disabled);

    // check that subsequent changes to the configuration are also persisted
    configurationPage.toggle().click();
    configurationPage.update().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    configurationPage.update().shouldBe(CLM.DISABLED);
    verifyConfiguration(false);
  }

  private void verifyConfiguration(boolean enabled) {
    AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO =
        new AutomaticSourceControlConfigurationDAO();

    assertThat(automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled()).isEqualTo(enabled);
  }
}
