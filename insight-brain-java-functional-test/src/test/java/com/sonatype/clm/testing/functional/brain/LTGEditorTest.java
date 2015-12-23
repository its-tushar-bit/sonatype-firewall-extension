/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.elements.ThreatLevelSelector;
import com.sonatype.clm.testing.functional.pages.LTGEditorPage;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.clm.testing.functional.utils.DoubleColumnPickerTestHelper;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.disabledClass;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class LTGEditorTest
    extends AbstractFunctionalTest
{
  private Organization organization;

  private LicenseThreatGroupDAO ltgDAO = new LicenseThreatGroupDAO();

  private LicenseDAO licenseDAO = new LicenseDAO();

  private LicenseThreatGroupLicenseDAO ltgLicenseDAO = new LicenseThreatGroupLicenseDAO();


  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    organization = tempEntity.newOrganization();
    refreshOrOpen(OwnerSummaryPage.url("organization", organization.getId()));
  }

  @Test
  public void testCreateLTG() {
    String ltgName = "Test LTG";

    SummaryTile.addLTGButton().click();
    assertNewLTGStateIsCorrect();
    LTGEditorPage.ltgName().val("$$$"); // invalid characters
    PopoverViolations.on(LTGEditorPage.ltgName()).shouldShowInvalidCharactersError();
    LTGEditorPage.saveButton().shouldHave(disabledClass());

    LTGEditorPage.ltgName().val(ltgName);
    PopoverViolations.on(LTGEditorPage.ltgName()).shouldNotExist();
    LTGEditorPage.saveButton().shouldBe(enabled).shouldNotHave(disabledClass()).click();

    assertNewLTGStateIsCorrect();
    LicenseThreatGroup ltg = ltgDAO.getByOwnerIdAndName(organization.getId(), ltgName);
    assertThat(ltg, notNullValue());
    assertThat(ltg.getName(), is(ltgName));
    assertThat(ltg.getThreatLevel(), is(LTGEditorPage.DEFAULT_THREAT_LEVEL));
    assertThat(ltgLicenseDAO.getByLicenseThreatGroupId(ltg.getId()), empty());
  }

  @Test
  public void testEditLTG() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(organization.getId(), "original name", 1);
    refresh();

    SummaryTile.localLTG(ltg.getName()).click();
    waitUntilUrl(LTGEditorPage.urlToEdit(organization.getId(), ltg.getId()));
    LTGEditorPage.title().shouldHave(text("Edit"));
    LTGEditorPage.ltgName().shouldBe(visible).shouldHave(cssClass("initial-value")).shouldHave(value("original name"));
    assertThreatLevelSelectorDefaultState(1);
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(licenseDAO.getAll().size());
    LTGEditorPage.saveButton().shouldHave(disabledClass());

    LTGEditorPage.ltgName().val("updated name");
    changeThreatLevel(6);
    pickFirstThreeLicenses();
    LTGEditorPage.saveButton().shouldBe(enabled).shouldNotHave(disabledClass()).click();

    LTGEditorPage.title().shouldHave(text("Edit"));
    LTGEditorPage.ltgName().shouldBe(visible).shouldHave(value("updated name"));
    ThreatLevelSelector.selectedThreatLevel().shouldBe(text("6"));
    DoubleColumnPicker.pickedItems().shouldHaveSize(3);
    LTGEditorPage.saveButton().shouldHave(disabledClass());

    List<LicenseThreatGroupLicense> includedLicenses = ltgLicenseDAO.getByLicenseThreatGroupId(ltg.getId());

    ltg = ltgDAO.getById(ltg.getId());
    assertThat(ltg, notNullValue());
    assertThat(ltg.getName(), is("updated name"));
    assertThat(ltg.getThreatLevel(), is(6));
    assertThat(includedLicenses.size(), is(3));

    for (int i = 0; i < includedLicenses.size(); i++) {
      DoubleColumnPicker.pickedItem(i).name().shouldHave(
          text(licenseDAO.getById(includedLicenses.get(i).getLicenseId()).getLongDisplayName()));
    }

    testDeleteLTG(ltg);
  }

  public void testDeleteLTG(LicenseThreatGroup ltg) {
    LTGEditorPage.deleteButton().shouldBe(visible, enabled).click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("License Threat Group"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(ltg.getName()));

    DeleteModal.continueButton().click();
    DeleteModal.root().shouldNotBe(visible);

    assertNewLTGStateIsCorrect();
    assertThat(ltgDAO.getById(ltg.getId()), is(nullValue()));
  }

  private void assertNewLTGStateIsCorrect() {
    waitUntilUrl(LTGEditorPage.urlToCreate(organization.getId()));
    LTGEditorPage.title().shouldHave(text("New"));
    LTGEditorPage.ltgName().shouldBe(visible, empty).shouldHave(cssClass("initial-value"));
    assertThreatLevelSelectorDefaultState(LTGEditorPage.DEFAULT_THREAT_LEVEL);
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(licenseDAO.getAll().size());
    LTGEditorPage.saveButton().shouldHave(disabledClass());
  }

  private void assertThreatLevelSelectorDefaultState(int selectedThreatLevel) {
    ThreatLevelSelector.root().shouldBe(visible);
    ThreatLevelSelector.caretButton().shouldBe(visible, enabled).click();
    ThreatLevelSelector.threatLevelList().shouldBe(visible);
    ThreatLevelSelector.threatLevelListItems().shouldHaveSize(ThreatLevelSelector.NUM_THREAT_LEVELS);

    for (int i = 0; i < ThreatLevelSelector.NUM_THREAT_LEVELS; i++) {
      ThreatLevelSelector.threatLevelListItem(i).shouldBe(visible);
      assertThat(Integer.parseInt(ThreatLevelSelector.threatLevelListItem(i).text()), is(10 - i));
    }

    ThreatLevelSelector.selectedThreatLevel().shouldBe(visible, text(Integer.toString(selectedThreatLevel)))
        .click();
  }

  private void changeThreatLevel(int threatLevel) {
    ThreatLevelSelector.caretButton().shouldBe(visible, enabled).click();
    ThreatLevelSelector.threatLevelListItem(10 - threatLevel).click();
    assertThat(Integer.parseInt(ThreatLevelSelector.selectedThreatLevel().text()), is(threatLevel));
  }

  private void pickFirstThreeLicenses() {
    int initialSize = DoubleColumnPicker.availableItems().size();
    List<String> pickedLicenseNames = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      DoubleColumnPicker.availableItem(i).checkbox().shouldBe(visible).click();
      pickedLicenseNames.add(DoubleColumnPicker.availableItem(i).name().text());
    }

    DoubleColumnPicker.pickCheckedItemsButton().shouldBe(enabled).click();

    DoubleColumnPicker.availableItems().shouldHaveSize(initialSize - 3);
    DoubleColumnPicker.pickedItems().shouldHaveSize(3);
    DoubleColumnPicker.pickCheckedItemsButton().shouldBe(disabled);
    DoubleColumnPicker.unpickCheckedItemsButton().shouldBe(enabled);

    for (int i = 0; i < 3; i++) {
      DoubleColumnPicker.pickedItem(i).checkbox().shouldBe(selected);
      DoubleColumnPicker.pickedItem(i).name().shouldHave(text(pickedLicenseNames.get(i)));
    }
  }
}
