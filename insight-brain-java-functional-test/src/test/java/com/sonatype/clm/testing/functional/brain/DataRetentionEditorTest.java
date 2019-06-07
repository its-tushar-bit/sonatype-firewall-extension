/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage.ApplicationReportRetentionEditor;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage.RetentionEditor;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage.SuccessMetricsRetentionEditor;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.textCaseSensitive;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public class DataRetentionEditorTest
    extends AbstractFunctionalTest
{
  private static final List<String> CONTEXT_IDS = Arrays.asList(
      Stage.ID_DEVELOP,
      Stage.ID_BUILD,
      Stage.ID_STAGE_RELEASE,
      Stage.ID_RELEASE,
      Stage.ID_OPERATE,
      DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING);

  private static final DataRetentionEditorPage PAGE = new DataRetentionEditorPage();

  private static final Map<String, RetentionEditor> EDITORS = new HashMap<>();

  static {
    CONTEXT_IDS.forEach(contextId -> EDITORS.put(contextId, new ApplicationReportRetentionEditor(contextId)));
    EDITORS.put(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, new SuccessMetricsRetentionEditor());
  }

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OrganizationManagementPage.ROOT_ORG_URL);
    loginAsAdmin();
  }

  private Organization organization;

  @Before
  public void before() {
    organization = tempEntity.newOrganization("Retention Test Org");
  }

  @Test
  public void testDataRetentionEditor() {
    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));

    CONTEXT_IDS.forEach(contextId -> checkInherit(contextId, "Inherit"));

    refreshOrOpen(DataRetentionEditorPage.url(Organization.ROOT_ORGANIZATION_ID));
    setDisabled(Stage.ID_DEVELOP);
    setCustom(Stage.ID_BUILD, "1", "Days", "");
    setCustom(Stage.ID_STAGE_RELEASE, "", "Years", "1");
    setCustom(Stage.ID_RELEASE, "2", "Days", "2");
    setDisabled(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
    updateDataRetention();
    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));
    setDisabled(Stage.ID_OPERATE);
    setCustom(DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING, "1", "Days", "");

    updateDataRetention();

    checkInherit(Stage.ID_DEVELOP, "don't purge");
    checkInherit(Stage.ID_BUILD, "keep at most 1 day");
    checkInherit(Stage.ID_STAGE_RELEASE, "keep at most 1 report");
    checkInherit(Stage.ID_RELEASE, "keep at most 2 days, 2 report");
    checkInherit(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "don't purge");
    checkDisabled(Stage.ID_OPERATE);
    checkCustom(DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING, "1", "Days", "");

    setCustom(Stage.ID_DEVELOP, "", "Years", "6");
    setCustom(Stage.ID_BUILD, "2", "Weeks", "8");
    setCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "2");

    updateDataRetention();

    checkCustom(Stage.ID_DEVELOP, "", "Years", "6");
    checkCustom(Stage.ID_BUILD, "2", "Weeks", "8");
    checkCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "2");

    setDisabled(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);

    updateDataRetention();

    checkDisabled(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);

    PAGE.getElement().parent().scrollIntoView(true);
    eyesWatcher.eyesCheck("Data retention editor top");
    PAGE.updateButton().scrollIntoView(true);
    eyesWatcher.eyesCheck("Data retention editor bottom");
  }

  @Test
  public void testDataRetentionEditor_LicensingAware() {
    clmLicenseManager.setStageTypes(StageTypes.RELEASE);
    clmLicenseManager.setMissingFeatures(Feature.POLICY_MONITORING);

    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));

    EDITORS.get(Stage.ID_RELEASE).shouldBe(visible);
    EDITORS.get(Stage.ID_DEVELOP).shouldNot(exist);
    EDITORS.get(Stage.ID_BUILD).shouldNot(exist);
    EDITORS.get(Stage.ID_STAGE_RELEASE).shouldNot(exist);
    EDITORS.get(Stage.ID_OPERATE).shouldNot(exist);
    EDITORS.get(DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING).shouldNot(exist);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testDataRetentionEditor_Dirty() {
    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));
    EDITORS.get(Stage.ID_BUILD).shouldBe(visible);

    // Initially inherit
    checkUpdateDisabled();
    setDisabled(Stage.ID_BUILD);
    checkUpdateEnabled();
    setInherit(Stage.ID_BUILD);
    checkUpdateDisabled();
    setCustom(Stage.ID_BUILD, "1", "Years", "");
    checkUpdateEnabled();
    setInherit(Stage.ID_BUILD);
    checkUpdateDisabled();

    // Initially disabled
    setDisabled(Stage.ID_BUILD);
    updateDataRetention();
    checkUpdateDisabled();
    setInherit(Stage.ID_BUILD);
    checkUpdateEnabled();
    setDisabled(Stage.ID_BUILD);
    checkUpdateDisabled();
    setCustom(Stage.ID_BUILD, "1", "Years", "");
    checkUpdateEnabled();
    setDisabled(Stage.ID_BUILD);
    checkUpdateDisabled();

    // Initially custom
    setCustom(Stage.ID_BUILD, "1", "Years", "2");
    updateDataRetention();
    checkUpdateDisabled();
    setInherit(Stage.ID_BUILD);
    checkUpdateEnabled();
    setCustom(Stage.ID_BUILD, "1", "Years", "2");
    checkUpdateDisabled();
    setDisabled(Stage.ID_BUILD);
    checkUpdateEnabled();
    setCustom(Stage.ID_BUILD, "1", "Years", "2");
    checkUpdateDisabled();

    // Change custom max values
    setCustom(Stage.ID_BUILD, "2", "Years", "2");
    checkUpdateEnabled();
    setCustom(Stage.ID_BUILD, "1", "Years", "2");
    checkUpdateDisabled();
    setCustom(Stage.ID_BUILD, "1", "Months", "2");
    checkUpdateEnabled();
    setCustom(Stage.ID_BUILD, "1", "Years", "2");
    checkUpdateDisabled();
    setCustom(Stage.ID_BUILD, "1", "Years", "3");
    checkUpdateEnabled();
    setCustom(Stage.ID_BUILD, "1", "Years", "2");
    checkUpdateDisabled();
  }

  @Test
  public void testDataRetentionEditor_Validation() {
    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));

    // Max age and/or max count
    validateCustom(Stage.ID_BUILD, "1", "Days", "1", null, null);
    validateCustom(Stage.ID_BUILD, "1", "Days", "", null, null);
    validateCustom(Stage.ID_BUILD, "", "Days", "1", null, null);

    // Max age limits
    validateCustom(Stage.ID_BUILD, "", "Days", "", "Please enter a value", "Please enter a value");
    validateCustom(Stage.ID_BUILD, "0", "Days", "", "Minimum allowed value is 1", "Please enter a value");
    validateCustom(Stage.ID_BUILD, "1", "Days", "", null, null);
    validateCustom(Stage.ID_BUILD, "18249", "Days", "", null, null);
    validateCustom(Stage.ID_BUILD, "18250", "Days", "", "Maximum allowed value is 18249", "Please enter a value");

    // Max count limits
    validateCustom(Stage.ID_BUILD, "", "Years", "0", "Please enter a value", "Minimum allowed value is 1");
    validateCustom(Stage.ID_BUILD, "", "Years", "1", null, null);
    validateCustom(Stage.ID_BUILD, "", "Years", "9999", null, null);
    validateCustom(Stage.ID_BUILD, "", "Years", "10000", "Please enter a value", "Maximum allowed value is 9999");
    setCustom(Stage.ID_BUILD, "1", "Years", "1");

    // Success metrics
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "0", "Minimum allowed value is 1");
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "1", null);
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "49", null);
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "50", "Maximum allowed value is 49");
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "", "Please enter a value");
  }

  @Test
  public void testDataRetentionEditor_RootOrganization() {
    refreshOrOpen(DataRetentionEditorPage.url(Organization.ROOT_ORGANIZATION_ID));

    DataRetentionEditorPage page = new DataRetentionEditorPage();

    for (String contextId : CONTEXT_IDS) {
      ApplicationReportRetentionEditor editor = page.editor(contextId);
      editor.radioButtonGroup().scrollIntoView(true).shouldBe(visible);
      editor.inheritRadioButton().shouldNotBe(visible);
      editor.disableRadioButton().shouldBe(visible);
      editor.customRadioButton().shouldBe(visible, selected);
    }

    eyesWatcher.eyesCheck();
  }

  private void checkUpdateEnabled() {
    PAGE.updateButton().should(exist).scrollIntoView(true).shouldBe(visible).shouldNotBe(CLM.DISABLED);
  }

  private void checkUpdateDisabled() {
    PAGE.updateButton().should(exist).scrollIntoView(true).shouldBe(visible, CLM.DISABLED);
  }

  private void checkInherit(String contextId, String inheritText) {
    RetentionEditor editor = EDITORS.get(contextId);
    editor.scrollIntoView();
    editor.inheritRadioButton().shouldBe(visible, selected).shouldHave(textCaseSensitive(inheritText));
    editor.disableRadioButton().shouldBe(visible).shouldNotBe(selected);
    editor.customRadioButton().shouldBe(visible).shouldNotBe(selected);
    editor.customRow().shouldNot(exist);
  }

  private void checkDisabled(String contextId, String inheritText) {
    RetentionEditor editor = EDITORS.get(contextId);
    editor.scrollIntoView();
    editor.inheritRadioButton().shouldBe(visible).shouldNotBe(selected).shouldHave(textCaseSensitive(inheritText));
    editor.disableRadioButton().shouldBe(visible, selected);
    editor.customRadioButton().shouldBe(visible).shouldNotBe(selected);
    editor.customRow().shouldNot(exist);
  }

  private void checkDisabled(String contextId) {
    checkDisabled(contextId, "Inherit");
  }

  private void checkCustom(String contextId, String inheritText, String maxAgeValue) {
    RetentionEditor editor = EDITORS.get(contextId);
    editor.scrollIntoView();
    editor.inheritRadioButton().shouldBe(visible).shouldNotBe(selected).shouldHave(textCaseSensitive(inheritText));
    editor.disableRadioButton().shouldBe(visible).shouldNotBe(selected);
    editor.customRadioButton().shouldBe(visible, selected);
    editor.customRow().shouldBe(visible);
    editor.maxAgeInput().shouldBe(visible).shouldHave(value(maxAgeValue));
  }

  private void checkCustom(String contextId, String maxAgeValue) {
    checkCustom(contextId, "Inherit", maxAgeValue);
  }

  private void checkCustom(String contextId,
                           String inheritText,
                           String maxAgeValue,
                           String maxAgeTimeUnit,
                           String maxCount)
  {
    checkCustom(contextId, inheritText, maxAgeValue);
    ApplicationReportRetentionEditor appEditor = (ApplicationReportRetentionEditor) EDITORS.get(contextId);
    appEditor.maxAgeDropdown().shouldBe(visible).shouldHave(exactTextCaseSensitive(maxAgeTimeUnit));
    appEditor.maxCountInput().shouldBe(visible).shouldHave(value(maxCount));
  }

  private void checkCustom(String contextId,
                           String maxAgeValue,
                           String maxAgeTimeUnit,
                           String maxCount)
  {
    checkCustom(contextId, "Inherit", maxAgeValue, maxAgeTimeUnit, maxCount);
  }

  private void validateCustom(String contextId,
                              String maxAgeValue,
                              String maxAgeTimeUnit,
                              String maxCount,
                              String maxAgeExpectedPopoverError,
                              String maxCountExpectedPopoverError)
  {
    ApplicationReportRetentionEditor appEditor = (ApplicationReportRetentionEditor) EDITORS.get(contextId);
    setCustom(contextId, maxAgeValue, maxAgeTimeUnit, maxCount);
    PopoverViolations maxAgePopoverError = PopoverViolations.on(appEditor.maxAgeInput());
    if (maxAgeExpectedPopoverError == null) {
      maxAgePopoverError.shouldNotExist();
    }
    else {
      maxAgePopoverError.shouldShowError(maxAgeExpectedPopoverError);
    }
    PopoverViolations maxCountPopoverError = PopoverViolations.on(appEditor.maxCountInput());
    if (maxCountExpectedPopoverError == null) {
      maxCountPopoverError.shouldNotExist();
    }
    else {
      maxCountPopoverError.shouldShowError(maxCountExpectedPopoverError);
    }
    SelenideElement updateButton = PAGE.updateButton().scrollIntoView(true).shouldBe(visible);
    if (maxAgeExpectedPopoverError == null && maxCountExpectedPopoverError == null) {
      updateButton.shouldNotBe(CLM.DISABLED);
    }
    else {
      updateButton.shouldBe(CLM.DISABLED);
    }
  }

  private void validateCustom(String contextId, String maxAgeValue, String maxAgeExpectedPopoverError) {
    RetentionEditor editor = EDITORS.get(contextId);
    setCustom(contextId, maxAgeValue);
    PopoverViolations maxAgePopoverError = PopoverViolations.on(editor.maxAgeInput());
    if (maxAgeExpectedPopoverError == null) {
      maxAgePopoverError.shouldNotExist();
      PAGE.updateButton().scrollIntoView(true).shouldBe(visible).shouldNotBe(CLM.DISABLED);
    }
    else {
      maxAgePopoverError.shouldShowError(maxAgeExpectedPopoverError);
      PAGE.updateButton().scrollIntoView(true).shouldBe(visible, CLM.DISABLED);
    }
  }

  private void setInherit(String contextId) {
    RetentionEditor editor = EDITORS.get(contextId);
    editor.scrollIntoView();
    editor.inheritRadioButton().click();
  }

  private void setDisabled(String contextId) {
    RetentionEditor editor = EDITORS.get(contextId);
    editor.scrollIntoView();
    editor.disableRadioButton().click();
  }

  private void setCustom(String contextId, String maxAgeValue) {
    RetentionEditor editor = EDITORS.get(contextId);
    editor.scrollIntoView();
    editor.customRadioButton().click();
    editor.maxAgeInput().setValue(maxAgeValue);
  }

  private void setCustom(String contextId,
                         String maxAgeValue,
                         String maxAgeTimeUnit,
                         String maxCount)
  {
    ApplicationReportRetentionEditor appEditor = (ApplicationReportRetentionEditor) EDITORS.get(contextId);
    appEditor.scrollIntoView();
    appEditor.customRadioButton().click();
    // dropdown should be set before the input to avoid potential unwanted errors
    appEditor.maxAgeDropdown().shouldBe(visible).selectedItem().click();
    appEditor.maxAgeDropdown().listItems().findBy(exactTextCaseSensitive(maxAgeTimeUnit)).click();
    appEditor.maxAgeInput().setValue(maxAgeValue);
    appEditor.maxCountInput().setValue(maxCount);
  }

  private void updateDataRetention() {
    checkUpdateEnabled();
    PAGE.updateButton().click();
    FormMask.seeAndWaitForDismissal();
  }
}
