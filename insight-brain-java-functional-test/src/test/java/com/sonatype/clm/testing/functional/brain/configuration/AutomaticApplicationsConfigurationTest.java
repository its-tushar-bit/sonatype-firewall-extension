/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.pages.AutomaticApplicationsConfigurationPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;

public class AutomaticApplicationsConfigurationTest
    extends AbstractFunctionalTest
{
  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(AutomaticApplicationsConfigurationPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    automaticApplicationsConfigurationDAO = lookup(AutomaticApplicationsConfigurationDAO.class);
  }

  @Test
  public void automaticApplicationsConfigurationTest() {
    AutomaticApplicationsConfigurationPage automaticApplicationsConfigurationPage =
        new AutomaticApplicationsConfigurationPage();

    refreshOrOpen(AutomaticApplicationsConfigurationPage.url());

    // check descriptions visibility
    automaticApplicationsConfigurationPage.explanation().shouldBe(visible).shouldNotBe(empty);
    automaticApplicationsConfigurationPage.explanationSourceControl()
        .shouldBe(visible)
        .shouldNotHave(text("which is configured to use"));

    // check initial state
    automaticApplicationsConfigurationPage.organization().listItems().shouldHave(size(0));
    automaticApplicationsConfigurationPage.toggle().shouldBe(visible, enabled).shouldNotBe(checked);

    // check user cannot update when organizations are not present
    automaticApplicationsConfigurationPage.cancel().shouldBe(disabled);
    automaticApplicationsConfigurationPage.toggle().click();
    automaticApplicationsConfigurationPage.toggle().input().shouldBe(checked);
    automaticApplicationsConfigurationPage.update().click();
    FormUtils.getAlertElement(automaticApplicationsConfigurationPage)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to update: fields with invalid or missing data."));

    // check that configuration settings can be saved
    Organization org1 = tempEntity.newOrganization("Test Organization 1");
    refresh();
    automaticApplicationsConfigurationPage.organization().shouldNotBe(empty);
    automaticApplicationsConfigurationPage.toggle().click();
    automaticApplicationsConfigurationPage.toggle().input().shouldBe(checked);
    eyesWatcher.eyesCheck();
    automaticApplicationsConfigurationPage.organization().chooseOption(new Option(1, org1.getName()));
    automaticApplicationsConfigurationPage.update().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    verifyConfiguration(true, org1);

    // check that the updated configuration is displayed on refresh
    refresh();
    automaticApplicationsConfigurationPage.organization().shouldNotBe(empty);
    automaticApplicationsConfigurationPage.organization().shouldHave(text(org1.getName()));
    automaticApplicationsConfigurationPage.toggle().input().shouldBe(checked);

    // check that local changes to configuration settings can be cancelled
    automaticApplicationsConfigurationPage.toggle().click();
    automaticApplicationsConfigurationPage.organization().shouldBe(disabled);
    automaticApplicationsConfigurationPage.update().shouldNotBe(CLM.DISABLED);
    automaticApplicationsConfigurationPage.toggle().input().shouldNotBe(checked);
    automaticApplicationsConfigurationPage.cancel().shouldNotBe(disabled).click();
    automaticApplicationsConfigurationPage.organization().shouldHave(text(org1.getName()));
    automaticApplicationsConfigurationPage.toggle().input().shouldBe(checked);

    // check that subsequent changes to the configuration are also persisted
    automaticApplicationsConfigurationPage.toggle().click();
    automaticApplicationsConfigurationPage.toggle().input().shouldNotBe(checked);
    automaticApplicationsConfigurationPage.organization().shouldBe(disabled);
    automaticApplicationsConfigurationPage.update().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    verifyConfiguration(false, org1);
  }

  @Test
  public void automaticApplicationsConfigurationTest_explanationSourceControl() {
    // given automatic applications are enabled
    Organization organization = tempEntity.newOrganizationAutomaticApplicationsConfiguration();

    // and source control is configured
    tempEntity.newSourceControl(organization.getId(), null, "token", GITHUB);

    // when opening the automatic applications page
    AutomaticApplicationsConfigurationPage automaticApplicationsConfigurationPage =
        new AutomaticApplicationsConfigurationPage();
    refreshOrOpen(AutomaticApplicationsConfigurationPage.url());

    // then source control configuration is mentioned in the explanation
    automaticApplicationsConfigurationPage.explanation().shouldBe(visible).shouldNotBe(empty);
    automaticApplicationsConfigurationPage.explanationSourceControl()
        .shouldBe(visible)
        .shouldHave(text("which is configured to use Github"));
  }

  private void verifyConfiguration(boolean enabled, Organization organization) {
    assertThat(automaticApplicationsConfigurationDAO.isEnabled()).isEqualTo(enabled);
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo(organization.getId());
  }
}
