/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.Dropdown.Option;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.AutomaticApplicationsConfigurationPage;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class AutomaticApplicationsConfigurationTest
    extends AbstractFunctionalTest
{
  @Test
  public void automaticApplicationsConfigurationTest() {
    AutomaticApplicationsConfigurationPage automaticApplicationsConfigurationPage =
        new AutomaticApplicationsConfigurationPage();

    refreshOrOpen(AutomaticApplicationsConfigurationPage.URL);
    loginAsAdmin();

    // check description is present
    automaticApplicationsConfigurationPage.explanation().shouldBe(visible).shouldNotBe(empty);

    // check initial state
    automaticApplicationsConfigurationPage.organization().listItems().shouldHaveSize(0);
    automaticApplicationsConfigurationPage.toggle().shouldBe(visible, enabled).shouldNotBe(checked);

    // check user cannot update when organizations are not present
    automaticApplicationsConfigurationPage.update().shouldBe(CLM.DISABLED);
    automaticApplicationsConfigurationPage.cancel().shouldBe(disabled);
    automaticApplicationsConfigurationPage.toggle().click();
    automaticApplicationsConfigurationPage.update().shouldBe(CLM.DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Unable to update: fields with invalid or missing data."));
    automaticApplicationsConfigurationPage.cancel().shouldNotBe(disabled).hover();
    Tooltip.get().shouldBe(hidden);

    // check that configuration settings can be saved
    Organization org1 = tempEntity.newOrganization("Test Organization 1");
    Organization org2 = tempEntity.newOrganization("Test Organization 2");
    refresh();
    automaticApplicationsConfigurationPage.organization().shouldNotBe(empty);
    automaticApplicationsConfigurationPage.toggle().click();
    eyesWatcher.eyesCheck();
    automaticApplicationsConfigurationPage.organization().chooseOption(new Option(0, org1.getName()));
    automaticApplicationsConfigurationPage.update().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    automaticApplicationsConfigurationPage.update().shouldBe(CLM.DISABLED);
    verifyConfiguration(true, org1);

    // check that the updated configuration is displayed on refresh
    refresh();
    automaticApplicationsConfigurationPage.organization().shouldNotBe(empty);
    automaticApplicationsConfigurationPage.organization().selectedItem().shouldHave(text(org1.getName()));
    automaticApplicationsConfigurationPage.toggle().shouldNotBe(CLM.DISABLED);

    // check that local changes to configuration settings can be cancelled
    automaticApplicationsConfigurationPage.toggle().click();
    automaticApplicationsConfigurationPage.organization().chooseOption(new Option(1, org2.getName()));
    automaticApplicationsConfigurationPage.update().shouldNotBe(CLM.DISABLED);
    automaticApplicationsConfigurationPage.cancel().shouldNotBe(disabled).click();
    automaticApplicationsConfigurationPage.toggle().shouldNotBe(CLM.DISABLED);
    automaticApplicationsConfigurationPage.organization().selectedItem().shouldHave(text(org1.getName()));
    automaticApplicationsConfigurationPage.update().shouldBe(CLM.DISABLED);
    automaticApplicationsConfigurationPage.cancel().shouldBe(disabled);

    // check that subsequent changes to the configuration are also persisted
    automaticApplicationsConfigurationPage.toggle().click();
    automaticApplicationsConfigurationPage.organization().chooseOption(new Option(1, org2.getName()));
    automaticApplicationsConfigurationPage.update().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    automaticApplicationsConfigurationPage.update().shouldBe(CLM.DISABLED);
    verifyConfiguration(false, org2);
  }

  private void verifyConfiguration(boolean enabled, Organization organization) {
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();

    assertThat(automaticApplicationsConfigurationDAO.isEnabled()).isEqualTo(enabled);
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo(organization.getId());
  }
}
