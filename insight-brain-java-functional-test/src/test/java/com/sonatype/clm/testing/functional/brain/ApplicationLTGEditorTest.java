/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.LTGEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public class ApplicationLTGEditorTest
    extends AbstractLTGEditorTest
{
  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  @Before
  public void init() {
    //note the ȧ being used to force a character to be encoded
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);

    super.init(application);
  }

  @Test
  public void testCreateLTG() {
    OwnerSummaryPage.licenseThreatGroupTile().addLTGButton().shouldNot(exist);
  }

  @Test
  public void testDeleteLTG_NavigationEdgeCase() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(currentOwner.getId(), "app ltg 1", 1);
    LicenseThreatGroup ltg2 = tempEntity.newLicenseThreatGroup(currentOwner.getId(), "app ltg 2", 1);

    refresh();

    OwnerSummaryPage.licenseThreatGroupTile().localLTG(ltg.getName()).click();
    waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltg.getId()));
    LTGEditorPage.title().shouldHave(text("Edit"));
    LTGEditorPage.ltgName().shouldBe(visible).shouldHave(CLM.PRISTINE).shouldHave(value("app ltg 1"));

    LTGEditorPage.deleteButton().click();
    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("License Threat Group"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(ltg.getName()));

    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltg2.getId()));
    LTGEditorPage.title().shouldHave(text("Edit"));
    LTGEditorPage.ltgName().shouldBe(visible).shouldHave(CLM.PRISTINE).shouldHave(value("app ltg 2"));

    LTGEditorPage.deleteButton().click();
    DeleteModal.root().shouldBe(visible);
    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    // no more ltgs left to delete so take user back to the summary page
    waitUntilUrl(OwnerSummaryPage.url(application));
  }

  @Override
  protected void assertNewLTGStateIsCorrect() {
    // no op
  }
}
