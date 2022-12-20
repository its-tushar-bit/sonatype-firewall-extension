/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.pages.LTGEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.InputUtils;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import com.codeborne.selenide.Condition;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationLTGEditorTest
    extends AbstractLTGEditorTest
{
  private Organization organization;

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    super.init(organization);
  }

  @Test
  public void testCreateLTG() {
    String ltgName = "Test LTG";

    OwnerSummaryPage.licenseThreatGroupSummaryTile().addLTGButton().click();
    //OwnerSummaryPage.licenseThreatGroupTile().addLTGButton().click();
    assertNewLTGStateIsCorrect();
    eyesWatcher.eyesCheck();
    LTGEditorPage.ltgName().val("$$$"); // invalid characters
    LTGEditorPage.getInputValidationElement(LTGEditorPage.ltgName()).shouldHave(text("Use valid characters"));

    LTGEditorPage.saveButton().shouldHave(DISABLED);

    InputUtils.clearInput(LTGEditorPage.ltgName());

    LTGEditorPage.ltgName().val(ltgName);
    LTGEditorPage.getInputValidationElement(LTGEditorPage.ltgName()).shouldNotBe(visible);

    LTGEditorPage.saveButton().shouldBe(enabled).shouldNotHave(DISABLED).click();

    assertNewLTGStateIsCorrect();
    LicenseThreatGroup ltg = ltgDAO.getByOwnerIdAndName(currentOwner.getId(), ltgName);
    assertThat(ltg).isNotNull();
    assertThat(ltg.getName()).isEqualTo(ltgName);
    assertThat(ltg.getThreatLevel()).isEqualTo(LTGEditorPage.DEFAULT_THREAT_LEVEL);
    assertThat(ltgLicenseDAO.getByLicenseThreatGroupId(ltg.getId())).isEmpty();
  }

  @Override
  protected void assertNewLTGStateIsCorrect() {
    waitUntilUrl(LTGEditorPage.urlToCreate(currentOwner));

    LTGEditorPage.title().shouldHave(text("New"));
    LTGEditorPage.ltgName().shouldBe(visible, Condition.empty);

    LTGEditorPage.picker().availableItems().shouldHaveSize(licenseDAO.getAll().size());
    LTGEditorPage.saveButton().shouldHave(DISABLED);
  }
}
