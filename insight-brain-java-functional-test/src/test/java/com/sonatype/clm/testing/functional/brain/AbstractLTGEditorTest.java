/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxThreatLevelDropdown;
import com.sonatype.clm.testing.functional.elements.NxTransferList;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.LTGEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.InputUtils;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractLTGEditorTest
    extends AbstractFunctionalTest
{
  protected Owner currentOwner;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected LicenseThreatGroupDAO ltgDAO;

  protected LicenseDAO licenseDAO;

  protected LicenseThreatGroupLicenseDAO ltgLicenseDAO;

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

  @Before
  public void setUp() {
    ltgDAO = lookup(LicenseThreatGroupDAO.class);
    licenseDAO = lookup(LicenseDAO.class);
    ltgLicenseDAO = lookup(LicenseThreatGroupLicenseDAO.class);
  }

  @Test
  public void testEditLTG() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(currentOwner.getId(), "original name", 1);
    refresh();

    OwnerSummaryPage.licenseThreatGroupSummaryTile()
        .getLocalLTGSection()
        .getSectionContentRows()
        .shouldHave(size(1));
    OwnerSummaryPage.licenseThreatGroupSummaryTile().getLocalLTGSection().getLTG(ltg.getName()).click();

    waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltg.getId()));

    LTGEditorPage.title().shouldHave(text("Edit"));
    LTGEditorPage.ltgName().shouldBe(visible).shouldHave(value("original name"));

    assertThreatLevelDropdownDefaultState(1);

    NxTransferList picker = LTGEditorPage.picker();

    picker.shouldBe(visible);
    SelenideElement availableFooter = picker.availableFooter();
    availableFooter.shouldHave(text(licenseDAO.getAll().size() + " Licenses available"));

    LTGEditorPage.ltgName().val("updated name");

    changeThreatLevel(6);

    filterLicenses(picker);
    pickFirstThreeLicenses(picker);

    LTGEditorPage.saveButton().click();

    LTGEditorPage.title().shouldHave(text("Edit"));
    LTGEditorPage.ltgName().shouldBe(visible).shouldHave(value("updated name"));
    NxThreatLevelDropdown.selectedThreatLevel().shouldBe(text("6"));
    picker.transferredItems().shouldHave(size(3));

    refresh();
    List<LicenseThreatGroupLicense> includedLicenses = ltgLicenseDAO.getByLicenseThreatGroupId(ltg.getId());
    ltg = ltgDAO.getById(ltg.getId());
    assertThat(ltg).isNotNull();
    assertThat(ltg.getName()).isEqualTo("updated name");
    assertThat(ltg.getThreatLevel()).isEqualTo(6);
    assertThat(includedLicenses).hasSize(3);

    final String expectedLicenseDisplayFormat = "(%s) %s";
    Function<License, String> formatLicenseForDisplay = license -> String
        .format(expectedLicenseDisplayFormat, license.getShortDisplayName(), license.getLongDisplayName());

    List<String> includedLicensesLongDisplayNames = includedLicenses.stream()
        .map(includedLicense -> licenseDAO.getById(includedLicense.getLicenseId()))
        .map(formatLicenseForDisplay)
        .sorted()
        .toList();

    for (int i = 0; i < includedLicenses.size(); i++) {
      picker.transferredItem(i).shouldHave(text(includedLicensesLongDisplayNames.get(i)));
    }

    testDeleteLTG(ltg);
  }

  @Test
  public void testTooltips() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(currentOwner.getId(), "original name", 1);
    refresh();

    OwnerSummaryPage.licenseThreatGroupSummaryTile()
        .getLocalLTGSection()
        .getSectionContentRows()
        .shouldHave(size(1));
    OwnerSummaryPage.licenseThreatGroupSummaryTile().getLocalLTGSection().getLTG(ltg.getName()).click();

    waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltg.getId()));

    NxTransferList picker = LTGEditorPage.picker();

    // no tooltip for short items
    picker.filter().val("Abstyles");
    picker.availableItem(0).shouldHave(exactText("(Abstyles) Abstyles License")).hover();
    Tooltip.get().shouldNot(exist);

    picker.availableItem(0).click();
    picker.transferredItem(0).hover();
    Tooltip.get().shouldNot(exist);

    InputUtils.clearInput(picker.filter());

    // tooltip should exist for overflowing items
    picker.filter().val("AFL");
    picker.availableItem(0).shouldHave(exactText("(AFL) AFL-Style License Not Identifiable by Sonatype")).hover();
    Tooltip.get().shouldHave(exactText("(AFL) AFL-Style License Not Identifiable by Sonatype"));

    picker.availableItem(0).click();

    picker.transferredItem(1).shouldHave(exactText("(AFL) AFL-Style License Not Identifiable by Sonatype")).hover();
    Tooltip.get().shouldHave(exactText("(AFL) AFL-Style License Not Identifiable by Sonatype"));
  }

  public void testDeleteLTG(LicenseThreatGroup ltg) {
    LTGEditorPage.deleteButton().shouldBe(visible, enabled).click();
    NxDeleteModal deleteModal = LTGEditorPage.deleteModal();

    deleteModal.shouldBe(visible);
    deleteModal.header().shouldHave(text("Delete License Threat Group"));
    deleteModal.alertContent()
        .shouldHave(text("You are about to permanently remove " + ltg.getName() +
            ". This action cannot be undone."));

    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.shouldBe(hidden);

    assertNewLTGStateIsCorrect();
    assertThat(ltgDAO.getById(ltg.getId())).isNull();
  }

  protected void assertThreatLevelDropdownDefaultState(int selectedThreatLevel) {
    NxThreatLevelDropdown.root().shouldBe(visible);
    NxThreatLevelDropdown.caretButton().shouldBe(visible, enabled).click();
    NxThreatLevelDropdown.threatLevelList().shouldBe(visible);
    NxThreatLevelDropdown.threatLevelListItems()
        .shouldHave(size(NxThreatLevelDropdown.NUM_THREAT_LEVELS));

    for (int i = 0; i < NxThreatLevelDropdown.NUM_THREAT_LEVELS; i++) {
      NxThreatLevelDropdown.threatLevelListItem(i).shouldBe(visible).shouldHave(text(String.valueOf(10 - i)));
    }

    NxThreatLevelDropdown.selectedThreatLevel().shouldBe(visible, text(Integer.toString(selectedThreatLevel))).click();
  }

  private void changeThreatLevel(int threatLevel) {
    NxThreatLevelDropdown.caretButton().shouldBe(visible, enabled).click();
    NxThreatLevelDropdown.threatLevelListItem(10 - threatLevel).click();
    NxThreatLevelDropdown.selectedThreatLevel().shouldHave(text(String.valueOf(threatLevel)));
  }

  private void pickFirstThreeLicenses(NxTransferList picker) {
    int size = licenseDAO.getAll().size();
    SelenideElement availableFooter = picker.availableFooter();
    availableFooter.shouldHave(text(size + " Licenses available"));
    SelenideElement transferredFooter = picker.transferredFooter();
    transferredFooter.shouldHave(text("0 Licenses transferred"));

    List<String> pickedLicenseNames = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      SelenideElement item = picker.availableItem(i);
      pickedLicenseNames.add(item.text());
      item.shouldBe(visible).click();
    }

    availableFooter = picker.availableFooter();
    availableFooter.shouldHave(text(size - 3 + " Licenses available"));
    transferredFooter = picker.transferredFooter();
    transferredFooter.shouldHave(text("3 Licenses transferred"));

    for (int i = 0; i < 3; i++) {
      picker.transferredItem(i).shouldHave(text(pickedLicenseNames.get(i)));
    }
  }

  private void filterLicenses(NxTransferList picker) {
    int initialSize = picker.availableItems().size();

    String filterText = "Adobe";
    picker.filter().val(filterText);
    picker.availableItems().shouldHave(size(10));

    for (int i = 0; i < 9; i++) {
      SelenideElement item = picker.availableItem(i);
      item.shouldBe(visible).shouldHave(text(filterText));
    }

    // reset filter
    InputUtils.clearInput(picker.filter());
    picker.availableItems().shouldHave(size(initialSize));
  }

  protected abstract void assertNewLTGStateIsCorrect();
}
