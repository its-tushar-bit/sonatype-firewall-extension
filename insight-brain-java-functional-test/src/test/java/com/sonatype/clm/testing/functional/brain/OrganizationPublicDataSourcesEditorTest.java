/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.PublicDataSourcesEditorPage;
import com.sonatype.insight.brain.model.Organization;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationPublicDataSourcesEditorTest
    extends AbstractPublicDataSourcesEditorTest
{
  private Organization organization;

  @Before
  public void init() {
    rootOrganization = ownerDao.getById(Organization.ROOT_ORGANIZATION_ID);
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION, (Organization) rootOrganization);
  }

  @Test
  public void testEditPublicDataSourceConfiguration_rootEnabledByDefault() {
    currentOwner = ownerDao.getById(Organization.ROOT_ORGANIZATION_ID);
    initPublicDataConfiguration(currentOwner, true, true);
    String url = PublicDataSourcesEditorPage.url(currentOwner.getType().name(), currentOwner.getId());
    refreshOrOpen(url);
    ElementsCollection radioInputs = PublicDataSourcesEditorPage.radioInputs();
    SelenideElement enabledRadioInput = radioInputs.first();
    enabledRadioInput.shouldHave(text("Enabled")).shouldBe(visible);
    enabledRadioInput.shouldHave(cssClass(RADIO_SELECTED_CSS_CLASS));
  }

  @Test
  public void testEditPublicDataSourceConfiguration_updateConfig_forRoot() {
    // Enabled in db by default
    currentOwner = ownerDao.getById(Organization.ROOT_ORGANIZATION_ID);
    initPublicDataConfiguration(currentOwner, true, true);

    String url = PublicDataSourcesEditorPage.url(currentOwner.getType().name(), currentOwner.getId());
    refreshOrOpen(url);
    SelenideElement disableRadioInput = findRadioInputByPartialText("Disabled");
    disableRadioInput.shouldHave(text("Disabled")).shouldBe(visible);
    disableRadioInput.shouldHave(cssClass(RADIO_UNSELECTED_CSS_CLASS));

    disableRadioInput.click();

    PublicDataSourcesEditorPage.submitButton().click();
    FormMask.seeAndWaitForDismissal();

    refreshOrOpen(url);
    disableRadioInput = findRadioInputByPartialText("Disabled");
    disableRadioInput.shouldHave(cssClass(RADIO_SELECTED_CSS_CLASS));
  }

  @Test
  public void testEditPublicDataSourceConfiguration_showsInheritedConfigurationSelected() {
    initPublicDataConfiguration(rootOrganization, true, true);
    currentOwner = ownerDao.getById(organization.getId());
    String url = PublicDataSourcesEditorPage.url(currentOwner.getType().name(), currentOwner.getId());
    refreshOrOpen(url);

    SelenideElement inheritedRadioInput = findRadioInputByPartialText(INHERIT_CONFIG_RADIO_TEXT);
    inheritedRadioInput.shouldHave(cssClass(RADIO_SELECTED_CSS_CLASS));

    SelenideElement enabledRadioInput = findRadioInputByText("Enabled");
    enabledRadioInput.shouldBe(visible);
    enabledRadioInput.shouldHave(cssClass(RADIO_UNSELECTED_CSS_CLASS));

    SelenideElement disableRadioInput = findRadioInputByText("Disabled");
    disableRadioInput.shouldHave(text("Disabled")).shouldBe(visible);
    disableRadioInput.shouldHave(cssClass(RADIO_UNSELECTED_CSS_CLASS));

    // allowOverride checkbox should be checked because is inherited from root organization
    PublicDataSourcesEditorPage.allowOverridesCheckbox().shouldHave(cssClass(RADIO_SELECTED_CSS_CLASS));
  }

  @Test
  public void testEditPublicDataSourceConfiguration_allowOverride_updateConfigurationToEnabled() {
    initPublicDataConfiguration(rootOrganization, false, true);
    currentOwner = ownerDao.getById(organization.getId());
    String url = PublicDataSourcesEditorPage.url(currentOwner.getType().name(), currentOwner.getId());
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
    currentOwner = ownerDao.getById(organization.getId());
    String url = PublicDataSourcesEditorPage.url(currentOwner.getType().name(), currentOwner.getId());
    refreshOrOpen(url);

    SelenideElement inheritedRadioInput = findRadioInputByPartialText(INHERIT_CONFIG_RADIO_TEXT);
    assertThat(inheritedRadioInput.text().contains(" (Disabled)")).isTrue();
    inheritedRadioInput.shouldHave(cssClass(RADIO_SELECTED_CSS_CLASS));

    PublicDataSourcesEditorPage.radioInputs()
        .forEach(input -> input.shouldHave(
            attribute("disabled", "")));
    PublicDataSourcesEditorPage.allowOverridesCheckbox().shouldHave(attribute("disabled", ""));
  }
}
