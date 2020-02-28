/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;
import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker.Item;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.ThreatLevelSelector;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.LTGEditorPage;
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
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

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
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
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
    assertThat(ltg).isNotNull();
    assertThat(ltg.getName()).isEqualTo("updated name");
    assertThat(ltg.getThreatLevel()).isEqualTo(6);
    assertThat(includedLicenses).hasSize(3);

    List<String> includedLicensesLongDisplayNames = includedLicenses.stream()
        .map(includedLicense -> licenseDAO.getById(includedLicense.getLicenseId()).getLongDisplayName()).sorted()
        .collect(Collectors.toList());

    for (int i = 0; i < includedLicenses.size(); i++) {
      picker.pickedItem(i).label().shouldHave(text(includedLicensesLongDisplayNames.get(i)));
    }

    testDeleteLTG(ltg);
  }

  @Test
  public void testTooltips() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(currentOwner.getId(), "original name", 1);
    refresh();

    OwnerSummaryPage.licenseThreatGroupTile().localLTGs().shouldHaveSize(1);
    OwnerSummaryPage.licenseThreatGroupTile().localLTG(ltg.getName()).click();
    waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltg.getId()));

    DoubleColumnPicker picker = LTGEditorPage.picker();

    // no tooltip for short items
    picker.filter().val("Adobe");
    picker.availableItem(0).shouldHave(exactText("Adobe")).hover();
    Tooltip.get().shouldNot(exist);
    picker.availableItem(0).click();
    picker.pickCheckedItemsButton().hover().click();

    // tooltip should exist for overflowing items
    picker.filter().val("AFL");
    picker.availableItem(0).shouldHave(exactText("AFL-Style License Not Identifiable by Sonatype")).hover();
    Tooltip.get().shouldHave(exactText("AFL-Style License Not Identifiable by Sonatype"));

    eyesWatcher.eyesCheck();

    picker.availableItem(0).click();
    picker.pickCheckedItemsButton().hover().click();

    // check tooltips in the picked column too
    picker.filter().clear();
    picker.pickedItem(0).shouldHave(exactText("Adobe")).hover();
    Tooltip.get().shouldNot(exist);
    picker.pickedItem(1).shouldHave(exactText("AFL-Style License Not Identifiable by Sonatype")).hover();
    Tooltip.get().shouldHave(exactText("AFL-Style License Not Identifiable by Sonatype"));
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
    assertThat(ltgDAO.getById(ltg.getId())).isNull();
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
      item.shouldBe(visible).click();
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
    picker.availableItems().shouldHaveSize(4);

    for (int i = 0; i < 4; i++) {
      Item item = picker.availableItem(i);
      item.label().shouldBe(visible).shouldHave(text(filterText));
    }

    // reset filter
    picker.filter().clear();
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(picker, initialSize);
  }

  protected abstract void assertNewLTGStateIsCorrect();
}
