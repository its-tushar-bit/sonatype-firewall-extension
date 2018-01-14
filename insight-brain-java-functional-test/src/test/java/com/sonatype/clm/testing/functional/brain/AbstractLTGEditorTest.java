/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;
import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker.Item;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.ThreatLevelSelector;
import com.sonatype.clm.testing.functional.pages.LTGEditorPage;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.DoubleColumnPickerTestHelper;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public abstract class AbstractLTGEditorTest
    extends AbstractFunctionalTest
{
  protected Owner currentOwner;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected LicenseThreatGroupDAO ltgDAO = new LicenseThreatGroupDAO();

  protected LicenseDAO licenseDAO = new LicenseDAO();

  protected LicenseThreatGroupLicenseDAO ltgLicenseDAO = new LicenseThreatGroupLicenseDAO();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  @Test
  public void testEditLTG() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(currentOwner.getId(), "original name", 1);
    refresh();

    OwnerSummaryPage.licenseThreatGroupTile().localLTGs().shouldHaveSize(1);
    OwnerSummaryPage.licenseThreatGroupTile().localLTG(ltg.getName()).click();
    waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltg.getId()));
    LTGEditorPage.title().shouldHave(text("Edit"));
    LTGEditorPage.ltgName().shouldBe(visible).shouldHave(CLM.PRISTINE).shouldHave(value("original name"));
    assertThreatLevelSelectorDefaultState(1);

    DoubleColumnPicker picker = LTGEditorPage.picker();
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(picker, licenseDAO.getAll().size());
    LTGEditorPage.saveButton().shouldHave(DISABLED);

    LTGEditorPage.ltgName().val("updated name");
    changeThreatLevel(6);
    filterLicenses(picker);
    pickFirstThreeLicenses(picker);
    LTGEditorPage.saveButton().shouldBe(enabled).shouldNotHave(DISABLED).click();

    LTGEditorPage.title().shouldHave(text("Edit"));
    LTGEditorPage.ltgName().shouldBe(visible).shouldHave(value("updated name"));
    ThreatLevelSelector.selectedThreatLevel().shouldBe(text("6"));
    picker.pickedItems().shouldHaveSize(3);
    LTGEditorPage.saveButton().shouldHave(DISABLED);

    List<LicenseThreatGroupLicense> includedLicenses = ltgLicenseDAO.getByLicenseThreatGroupId(ltg.getId());

    ltg = ltgDAO.getById(ltg.getId());
    assertThat(ltg, notNullValue());
    assertThat(ltg.getName(), is("updated name"));
    assertThat(ltg.getThreatLevel(), is(6));
    assertThat(includedLicenses.size(), is(3));

    for (int i = 0; i < includedLicenses.size(); i++) {
      picker.pickedItem(i).label()
          .shouldHave(text(licenseDAO.getById(includedLicenses.get(i).getLicenseId()).getLongDisplayName()));
    }

    testDeleteLTG(ltg);
  }

  public void testDeleteLTG(LicenseThreatGroup ltg) {
    LTGEditorPage.deleteButton().shouldBe(visible, enabled).click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("License Threat Group"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(ltg.getName()));

    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    assertNewLTGStateIsCorrect();
    assertThat(ltgDAO.getById(ltg.getId()), is(nullValue()));
  }

  protected void assertThreatLevelSelectorDefaultState(int selectedThreatLevel) {
    ThreatLevelSelector.root().shouldBe(visible);
    ThreatLevelSelector.caretButton().shouldBe(visible, enabled).click();
    ThreatLevelSelector.threatLevelList().shouldBe(visible);
    ThreatLevelSelector.threatLevelListItems().shouldHaveSize(ThreatLevelSelector.NUM_THREAT_LEVELS);

    for (int i = 0; i < ThreatLevelSelector.NUM_THREAT_LEVELS; i++) {
      ThreatLevelSelector.threatLevelListItem(i).shouldBe(visible).shouldHave(text(String.valueOf(10 - i)));
    }

    ThreatLevelSelector.selectedThreatLevel().shouldBe(visible, text(Integer.toString(selectedThreatLevel))).click();
  }

  private void changeThreatLevel(int threatLevel) {
    ThreatLevelSelector.caretButton().shouldBe(visible, enabled).click();
    ThreatLevelSelector.threatLevelListItem(10 - threatLevel).click();
    ThreatLevelSelector.selectedThreatLevel().shouldHave(text(String.valueOf(threatLevel)));
  }

  private void pickFirstThreeLicenses(DoubleColumnPicker picker) {
    int initialSize = picker.availableItems().size();
    List<String> pickedLicenseNames = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      Item item = picker.availableItem(i);
      item.hover().tooltip().shouldBe(visible).shouldHave(text(item.label().text()));
      LTGEditorPage.title().hover(); // hide the tooltip
      item.tooltip().shouldNot(exist);
      item.shouldBe(visible).click();
      LTGEditorPage.title().hover(); // hide the tooltip... click event may trigger tooltip again
      item.tooltip().shouldNot(exist);
      pickedLicenseNames.add(item.label().text());
    }

    picker.pickCheckedItemsButton().shouldBe(enabled).click();

    picker.availableItems().shouldHaveSize(initialSize - 3);
    picker.pickedItems().shouldHaveSize(3);
    picker.pickCheckedItemsButton().shouldBe(disabled);
    picker.unpickCheckedItemsButton().shouldBe(enabled);

    for (int i = 0; i < 3; i++) {
      picker.pickedItem(i).shouldBe(selected).label().shouldHave(text(pickedLicenseNames.get(i)));
    }
  }

  private void filterLicenses(DoubleColumnPicker picker) {
    int initialSize = picker.availableItems().size();

    String filterText = "Adobe";
    picker.filter().val(filterText);
    picker.availableItems().shouldHaveSize(3);

    for (int i = 0; i < 3; i++) {
      Item item = picker.availableItem(i);
      item.label().shouldBe(visible).shouldHave(text(filterText));
    }

    // reset filter 
    picker.filter().clear();
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(picker, initialSize);
  }

  protected abstract void assertNewLTGStateIsCorrect();
}
