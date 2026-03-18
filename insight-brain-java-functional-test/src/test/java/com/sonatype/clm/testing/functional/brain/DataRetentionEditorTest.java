/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage.ApplicationReportRetentionEditor;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage.RetentionEditor;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage.SuccessMetricsRetentionEditor;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.textCaseSensitive;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

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

  private DataRetentionPolicyDAO dataRetentionPolicyDAO;

  static {
    CONTEXT_IDS.forEach(contextId -> EDITORS.put(contextId, new ApplicationReportRetentionEditor(contextId)));
    EDITORS.put(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, new SuccessMetricsRetentionEditor());
  }

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  private Organization organization;

  private Collection<DataRetentionPolicy> rootOrgDataRetentionPolicies;

  @Before
  public void before() {
    dataRetentionPolicyDAO = lookup(DataRetentionPolicyDAO.class);
    organization = tempEntity.newOrganization("Retention Test Org");
    rootOrgDataRetentionPolicies =
        dataRetentionPolicyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID).values();
  }

  @After
  public void after() {
    // Delete any DataRetentionPolicy created by the tests for the root org.
    Set<String> rootOrgDataRetentionPolicyIds =
        rootOrgDataRetentionPolicies.stream().map(DataRetentionPolicy::getId).collect(toSet());
    dataRetentionPolicyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)
        .values()
        .stream()
        .filter(dataRetentionPolicy -> !rootOrgDataRetentionPolicyIds.contains(dataRetentionPolicy.getId()))
        .forEach(dataRetentionPolicyDAO::delete);

    // Restore any standard root org DataRetentionPolicy changed by the tests.
    for (DataRetentionPolicy rootOrgDataRetentionPolicy : rootOrgDataRetentionPolicies) {
      if (dataRetentionPolicyDAO.getById(rootOrgDataRetentionPolicy.getId()) == null) {
        dataRetentionPolicyDAO.insert(rootOrgDataRetentionPolicy);
      }
      else {
        dataRetentionPolicyDAO.update(rootOrgDataRetentionPolicy);
      }
    }
  }

  @Test
  public void testDataRetentionEditor() {
    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));

    CONTEXT_IDS.forEach(contextId -> checkInherit(contextId, "Inherit"));

    refreshOrOpen(DataRetentionEditorPage.url(Organization.ROOT_ORGANIZATION_ID));
    setDisabled(Stage.ID_DEVELOP);
    setCustom(Stage.ID_BUILD, "1", "Days", "");
    setCustom(Stage.ID_STAGE_RELEASE, " ", "Years", "1");
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

    setCustom(Stage.ID_DEVELOP, " ", "Years", "6");
    setCustom(Stage.ID_BUILD, "2", "Weeks", "8");
    setCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "2");

    updateDataRetention();

    checkCustom(Stage.ID_DEVELOP, " ", "Years", "6");
    checkCustom(Stage.ID_BUILD, "2", "Weeks", "8");
    checkCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "2");

    setDisabled(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);

    updateDataRetention();

    checkDisabled(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);

    PAGE.getElement().parent().scrollIntoView(true);
    eyesWatcher.eyesCheck("Data retention editor top");
  }

  @Test
  public void testDataRetentionEditor_LicensingAware() {
    testProductLicense.setStageTypes(StageTypes.RELEASE);
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_MONITORING);

    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));

    EDITORS.get(Stage.ID_RELEASE).shouldBe(visible);
    EDITORS.get(Stage.ID_DEVELOP).shouldNot(exist);
    EDITORS.get(Stage.ID_BUILD).shouldNot(exist);
    EDITORS.get(Stage.ID_STAGE_RELEASE).shouldNot(exist);
    EDITORS.get(Stage.ID_OPERATE).shouldNot(exist);
    EDITORS.get(DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING).shouldNot(exist);
  }

  @Test
  public void testDataRetentionEditor_FormValidation() {
    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));
    EDITORS.get(Stage.ID_BUILD).shouldBe(visible);

    // No changes to save
    setInherit(Stage.ID_BUILD);
    updateDataRetention();
    FormUtils.getAlertElement(PAGE)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));

    // When there are field validation errors
    validateCustom(Stage.ID_BUILD, "0", "Days", "", "Minimum allowed value is 1", null);
    updateDataRetention();
    FormUtils.getAlertElement(PAGE)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to save: fields with invalid or missing data"));
  }

  @Test
  public void testDataRetentionEditor_Validation() {
    refreshOrOpen(DataRetentionEditorPage.url(organization.getId()));

    // Max age and/or max count
    validateCustom(Stage.ID_BUILD, "1", "Days", "1", null, null);
    validateCustom(Stage.ID_BUILD, "1", "Days", "", null, null);
    validateCustom(Stage.ID_BUILD, "", "Days", "1", null, null);

    // Max age limits
    validateCustom(Stage.ID_BUILD, "0", "Days", "", "Minimum allowed value is 1", null);
    validateCustom(Stage.ID_BUILD, "1", "Days", "", null, null);
    validateCustom(Stage.ID_BUILD, "18249", "Days", "", null, null);
    validateCustom(Stage.ID_BUILD, "18250", "Days", "", "Maximum allowed value is 18249", null);

    // Max count limits
    validateCustom(Stage.ID_BUILD, "", "Years", "0", "Maximum allowed value is 49", "Minimum allowed value is 1");
    validateCustom(Stage.ID_BUILD, " ", "Years", "1", null, null);
    validateCustom(Stage.ID_BUILD, "", "Years", "9999", null, null);
    validateCustom(Stage.ID_BUILD, "", "Years", "10000", "Must be non-empty", "Maximum allowed value is 9999");
    setCustom(Stage.ID_BUILD, "1", "Years", "1");

    // Success metrics
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "0", "Minimum allowed value is 1");
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "15", null);
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "49", null);
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, "50", "Maximum allowed value is 49");
    validateCustom(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS, " ", "Must be non-empty");
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
      editor.customRadioButton().shouldBe(visible).shouldHave(cssClass("tm-checked"));
    }

    eyesWatcher.eyesCheck();
  }

  private void checkInherit(String contextId, String inheritText) {
    RetentionEditor editor = EDITORS.get(contextId);
    editor.scrollIntoView();
    editor.inheritRadioButton()
        .shouldBe(visible)
        .shouldHave(cssClass("tm-checked"))
        .shouldHave(textCaseSensitive(inheritText));
    editor.disableRadioButton().shouldBe(visible).shouldNotHave(cssClass("tm-checked"));
    editor.customRadioButton().shouldBe(visible).shouldNotHave(cssClass("tm-checked"));
    editor.customRow().shouldNot(exist);
  }

  private void checkDisabled(String contextId, String inheritText) {
    RetentionEditor editor = EDITORS.get(contextId);
    editor.scrollIntoView();
    editor.inheritRadioButton()
        .shouldBe(visible)
        .shouldNotHave(cssClass("tm-checked"))
        .shouldHave(textCaseSensitive(inheritText));
    editor.disableRadioButton().shouldBe(visible).shouldHave(cssClass("tm-checked"));
    editor.customRadioButton().shouldBe(visible).shouldNotHave(cssClass("tm-checked"));
    editor.customRow().shouldNot(exist);
  }

  private void checkDisabled(String contextId) {
    checkDisabled(contextId, "Inherit");
  }

  private void checkCustom(String contextId, String inheritText, String maxAgeValue) {
    RetentionEditor editor = EDITORS.get(contextId);
    editor.scrollIntoView();
    editor.inheritRadioButton()
        .shouldBe(visible)
        .shouldNotHave(cssClass("tm-checked"))
        .shouldHave(textCaseSensitive(inheritText));
    editor.disableRadioButton().shouldBe(visible).shouldNotHave(cssClass("tm-checked"));
    editor.customRadioButton().shouldBe(visible).shouldHave(cssClass("tm-checked"));
    editor.customRow().shouldBe(visible);
    editor.maxAgeInput().shouldBe(visible).shouldHave(value(maxAgeValue));
  }

  private void checkCustom(String contextId, String maxAgeValue) {
    checkCustom(contextId, "Inherit", maxAgeValue);
  }

  private void checkCustom(
      String contextId,
      String inheritText,
      String maxAgeValue,
      String maxAgeTimeUnit,
      String maxCount)
  {
    checkCustom(contextId, inheritText, maxAgeValue);
    ApplicationReportRetentionEditor appEditor = (ApplicationReportRetentionEditor) EDITORS.get(contextId);
    appEditor.maxAgeDropdown().shouldBe(visible);
    assertThat(appEditor.maxAgeDropdown().selectedItem().getText()).isEqualTo(maxAgeTimeUnit);
    appEditor.maxCountInput().shouldBe(visible).shouldHave(value(maxCount));
  }

  private void checkCustom(
      String contextId,
      String maxAgeValue,
      String maxAgeTimeUnit,
      String maxCount)
  {
    checkCustom(contextId, "Inherit", maxAgeValue, maxAgeTimeUnit, maxCount);
  }

  private void validateCustom(
      String contextId,
      String maxAgeValue,
      String maxAgeTimeUnit,
      String maxCount,
      String maxAgeExpectedError,
      String maxCountExpectedError)
  {
    setCustom(contextId, maxAgeValue, maxAgeTimeUnit, maxCount);
    RetentionEditor editor = EDITORS.get(contextId);
    if (maxAgeExpectedError == null) {
      editor.ageErrorMessage().shouldNotBe(visible);
    }
    else {
      editor.ageErrorMessage().shouldHave(text(maxAgeExpectedError));
    }

    if (maxCountExpectedError == null) {
      editor.countErrorMessage().shouldNotBe(visible);
    }
    else {
      editor.countErrorMessage().shouldHave(text(maxCountExpectedError));
    }
  }

  private void validateCustom(String contextId, String maxAgeValue, String maxAgeExpectedError) {
    RetentionEditor editor = EDITORS.get(contextId);
    setCustom(contextId, maxAgeValue);
    if (maxAgeExpectedError == null) {
      editor.ageErrorMessage().shouldNotBe(visible);
    }
    else {
      editor.ageErrorMessage().shouldHave(text(maxAgeExpectedError));
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

  private void setCustom(
      String contextId,
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
    PAGE.updateButton().click();
    FormMask.seeAndWaitForDismissal();
  }
}
