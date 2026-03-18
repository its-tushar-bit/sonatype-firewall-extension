/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.PublicDataSourcesEditorPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationPublicDataSourcesEditorTest
    extends AbstractPublicDataSourcesEditorTest
{
  private Application application;

  @Before
  public void init() {
    rootOrganization = ownerDao.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION, (Organization) rootOrganization);
    application = tempEntity.newApplicationWithParent(organization);
  }

  @Test
  public void testEditPublicDataSourceConfiguration_showsInheritedConfigurationSelected() {
    initPublicDataConfiguration(rootOrganization, true, true);
    String url = PublicDataSourcesEditorPage.url(application.getType().name(), application.getPublicId());
    refreshOrOpen(url);

    SelenideElement inheritedRadioInput = findRadioInputByPartialText(INHERIT_CONFIG_RADIO_TEXT);
    inheritedRadioInput.shouldHave(cssClass(RADIO_SELECTED_CSS_CLASS));

    SelenideElement enabledRadioInput = findRadioInputByText("Enabled");
    enabledRadioInput.shouldBe(visible);
    enabledRadioInput.shouldHave(cssClass(RADIO_UNSELECTED_CSS_CLASS));

    SelenideElement disableRadioInput = findRadioInputByText("Disabled");
    disableRadioInput.shouldHave(text("Disabled")).shouldBe(visible);
    disableRadioInput.shouldHave(cssClass(RADIO_UNSELECTED_CSS_CLASS));

    // allowOverride checkbox is not visible at application level
    PublicDataSourcesEditorPage.allowOverridesCheckbox().shouldNotBe(visible);
  }

  @Test
  public void testEditPublicDataSourceConfiguration_allowOverride_updateConfigurationToEnabled() {
    initPublicDataConfiguration(rootOrganization, false, true);
    String url = PublicDataSourcesEditorPage.url(application.getType().name(), application.getPublicId());
    refreshOrOpen(url);

    SelenideElement inheritedRadioInput = findRadioInputByPartialText(INHERIT_CONFIG_RADIO_TEXT);
    assertThat(inheritedRadioInput.text().contains(" (Disabled)")).isTrue();
    inheritedRadioInput.shouldHave(cssClass(RADIO_SELECTED_CSS_CLASS));

    SelenideElement enabledRadioInput = findRadioInputByPartialText("Enabled");

    enabledRadioInput.click();

    PublicDataSourcesEditorPage.submitButton().click();
    FormMask.seeAndWaitForDismissal();

    refreshOrOpen(url);
    enabledRadioInput = findRadioInputByText("Enabled");
    enabledRadioInput.shouldHave(cssClass(RADIO_SELECTED_CSS_CLASS));
  }

  @Test
  public void testEditPublicDataSourceConfiguration_allowOverride_isDisabled() {
    initPublicDataConfiguration(rootOrganization, false, false);
    String url = PublicDataSourcesEditorPage.url(application.getType().name(), application.getPublicId());
    refreshOrOpen(url);

    SelenideElement inheritedRadioInput = findRadioInputByPartialText(INHERIT_CONFIG_RADIO_TEXT);
    assertThat(inheritedRadioInput.text().contains(" (Disabled)")).isTrue();
    inheritedRadioInput.shouldHave(cssClass(RADIO_SELECTED_CSS_CLASS));

    PublicDataSourcesEditorPage.radioInputs()
        .forEach(input -> input.shouldHave(
            attribute("disabled", "")));
    PublicDataSourcesEditorPage.allowOverridesCheckbox().shouldNotBe(visible);
  }
}
